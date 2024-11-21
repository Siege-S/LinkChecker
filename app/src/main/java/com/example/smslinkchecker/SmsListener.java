package com.example.smslinkchecker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

// Regex
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

// screenshotmachine API
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

// another
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SmsListener extends BroadcastReceiver {
    private static final String CHANNEL_ID = "1001";
    public static final String API_KEY = BuildConfig.VT_API_KEY;
    public static final String ss_API_KEY = BuildConfig.SS_API_KEY;

    // Newest
    @Override
    public void onReceive(Context context, Intent intent) {

        // Adapted from (Source: https://www.youtube.com/watch?v=Q_5mnnj2Mfg)
        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())){
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SmsApp::WakeLockTag"
            );
            wakeLock.acquire(60 * 1000L); // Acquire for up to 2 minutes

            DBHelper dbHelper = new DBHelper(context);

            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)){
                String sender = smsMessage.getOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();

                // Test
                System.out.println(sender + " : " + messageBody);

                // Regular Expression
                List<String> urls = extractUrlsFromMessage(messageBody, context);

                if (!MessageFragment.isInternetConnected(context)) {
                    handleOfflineUrls(context, urls, sender, dbHelper);
                } else {
                    processUrlsWithInternet(context, urls, sender, dbHelper);
                }
            }

        }
    }

    private List<String> extractUrlsFromMessage(String msgBody, Context context) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = Patterns.WEB_URL.matcher(msgBody);
        while (matcher.find()) {
            String detectedUrl = matcher.group();
            urls.add(detectedUrl);
            Toast.makeText(context, "URL Detected: " + detectedUrl, Toast.LENGTH_LONG).show();
            Log.v("URL", "URL Detected: " + detectedUrl);
        }
        return urls;
    }

    private void handleOfflineUrls(Context context, List<String> urls, String msgFrom, DBHelper dbHelper) {
        Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
        int notificationID = 0;
        for (String url : urls) {
            notificationID++;
            if (dbHelper.insertOfflineTbl(url, msgFrom)) {
                noInternet(context, url, notificationID);
            }
        }
    }
    public void processUrlsWithInternet(Context context, List<String> urls, String msgFrom, DBHelper dbHelper) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            for (String url : urls) {
                createNotification(context, "URL Detected in SMS message", url, 100);
                executorService.execute(() -> {
                    // Get the pinned OkHttpClient
                    // Documentation https://square.github.io/okhttp/#post-to-a-server
                    OkHttpClient client = NetworkClient.getPinnedHttpClient();

                    JSONObject jsonResponse;
                    String analysisId;
                    try {
                        // POST request https://docs.virustotal.com/reference/scan-url
                        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                        RequestBody body = RequestBody.create(mediaType, "url=" + url);
                        Request request = new Request.Builder()
                                .url("https://www.virustotal.com/api/v3/urls")
                                .post(body)
                                .addHeader("accept", "application/json")
                                .addHeader("x-apikey", API_KEY)
                                .addHeader("content-type", "application/x-www-form-urlencoded")
                                .build();

                        try (Response postResponse = client.newCall(request).execute()) {
                            if (!postResponse.isSuccessful()) {
                                throw new IOException("Unexpected code " + postResponse);
                            }

                            String responseBody = postResponse.body().string();
                            jsonResponse = new JSONObject(responseBody);
                            analysisId =  jsonResponse.getJSONObject("data").getString("id");
                        }

                        // GET request https://docs.virustotal.com/reference/analysis
                        Request getRequest = new Request.Builder()
                                .url("https://www.virustotal.com/api/v3/analyses/" + analysisId)
                                .get()
                                .addHeader("accept", "application/json")
                                .addHeader("x-apikey", API_KEY)
                                .build();

                        while (true) {
                            try (Response getResponse = client.newCall(getRequest).execute()) {
                                if (!getResponse.isSuccessful()) {
                                    throw new IOException("Unexpected code " + getResponse);
                                }
                                String getResponseBody = getResponse.body().string();
                                System.out.println("getResponseBody: " + getResponseBody);

                                jsonResponse = new JSONObject(getResponseBody);
                                JSONObject attributes = jsonResponse.getJSONObject("data").getJSONObject("attributes");

                                Log.v("getResponseBody", jsonResponse.toString(2));
                                System.out.println("Status: " + attributes.getString("status"));

                                if (!attributes.getString("status").equals("queued")) {
                                    break;
                                }
                                Thread.sleep(2000);
                            }
                        }
                        // Screenshot using screenshotmachine API
                        String apiUrl = SnapshotmachineAPI(url);
                        Bitmap bitmap = BitmapFactory.decodeStream(new URL(apiUrl).openStream());
                        byte[] image = getBitmapAsByteArray(bitmap);

                        // Send to Database
                        JSONObject finalJsonResponse = jsonResponse;
                        String analysisResultJSON = jsonResponse.toString();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            String analysis = NotifyResult(context, url, String.valueOf(finalJsonResponse));
                            dbHelper.insertData(url, msgFrom, apiUrl, analysis, image, analysisResultJSON);
                        });

                    } catch (IOException | JSONException | InterruptedException |
                             NoSuchAlgorithmException e) {
                        showRetryNotification(context, url, msgFrom);
                        e.printStackTrace();
                    }
                });
            }
        } finally {
            executorService.shutdown();
        }
    }

    public void showRetryNotification(Context context, String url, String sender) {
        Intent retryIntent = new Intent(context, RetryReceiver.class);
        retryIntent.putExtra("url", url);
        retryIntent.putExtra("sender", sender);

        // Use FLAG_IMMUTABLE if targeting Android 12 or higher, otherwise use FLAG_UPDATE_CURRENT
        PendingIntent retryPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Scan Failed")
                .setContentText("Tap to retry scanning the URL.")
                .addAction(R.drawable.retry, "Retry", retryPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(100, builder.build());
    }


    public String NotifyResult(Context context, String url, String JSON){
        NotificationManager notificationManager = (NotificationManager)  context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Create an Intent to open your main Activity when the notification is clicked
        Intent intent = new Intent(context, MainActivity.class); // Replace with your main activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Ensure app is brought to foreground

        // Wrap the Intent in a PendingIntent with FLAG_IMMUTABLE
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // requestCode
                intent,
                PendingIntent.FLAG_IMMUTABLE // Required for Android 12+
        );

        String setTitle;
        String result;
        try {
            JSONObject jsonObject = new JSONObject(JSON);

            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt( "malicious");
            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");
            int harmless = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("harmless");

            if(malicious > 0 && suspicious > 0){ // 3 if malicious and suspicious
                setTitle = "Link is Malicious and Suspicious";
                result = "3";
            }
            else if(malicious > 0){ // 1 if malicious
                setTitle = "Link is Malicious";
                result = "1";
            }
            else if(suspicious > 0){ // 2 if suspicious
                setTitle = "Link is Suspicious";
                result = "2";
            }
            else { // 0 if harmless
                setTitle = "Link is Harmless";
                result = "0";

            }

            // Notification 1
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(setTitle)
                    .setContentText("URL: " + url)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true) // Dismiss the notification when clicked
                    .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification

            notificationManager.notify(100, builder.build());
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String SnapshotmachineAPI(String url) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // Call ScreenshotMachine API
        String customerKey = ss_API_KEY;
        String secretPhrase = ""; // leave secret phrase empty if not needed
        ScreenshoMachine sm = new ScreenshoMachine(customerKey, secretPhrase);
        Map<String, String> options = new HashMap<>();
        options.put("url", url);
        options.put("dimension", "1366x768");
        options.put("device", "desktop");
        options.put("format", "png");
        options.put("cacheLimit", "0");
        options.put("delay", "3000"); // 3 seconds
        options.put("zoom", "100");

        String apiUrl = sm.generateScreenshotApiUrl(options);
        Log.v("Screenshot API URL", apiUrl);

        return apiUrl;
    }

    public byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    private void createNotification(Context context, String title, String url, int notificationID) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Create an Intent to open your main Activity when the notification is clicked
        Intent intent = new Intent(context, MainActivity.class); // Replace with your main activity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Ensure app is brought to foreground

        // Wrap the Intent in a PendingIntent with FLAG_IMMUTABLE
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // requestCode
                intent,
                PendingIntent.FLAG_IMMUTABLE // Required for Android 12+
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("Exercise caution and await the analysis of this URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss the notification when clicked
                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification

        // Show the notification
        notificationManager.notify(notificationID, builder.build());
    }

    private void invalidURL(Context context, String url) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Error in Scanning URL")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        notificationManager.notify(6, builder.build());
    }
    private void noInternet(Context context, String url, int notificationID) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("LinkGuard is Offline, '" + url + "' is saved for later analysis")
                .setContentText("Please avoid clicking this url '" + url + "', until connection is stored.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        notificationManager.notify(notificationID, builder.build());
    }

    public String getAnalysis(String analysisId) throws IOException {
        if (analysisId == null || analysisId.isEmpty()) {
            Log.e("SmsListener", "No Analysis ID provided.");
            return null;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("https://www.virustotal.com/api/v3/analyses/" + analysisId)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("x-apikey", API_KEY)
                .build();

        while (true) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                Log.d("SmsListener", "getAnalysis Response: " + responseBody);
                System.out.println("Get Analysis Response received: " + responseBody);

                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONObject attributes = jsonResponse.getJSONObject("data").getJSONObject("attributes");

                if (!attributes.getString("status").equals("queued")) {
                    Log.d("SmsListener", "Analysis Results: " + jsonResponse.toString(2));
                    return jsonResponse.toString();
                } else {
                    Log.d("SmsListener", "Analysis still queued.");
                    Thread.sleep(1000);
                }

            } catch (IOException | JSONException | InterruptedException e) {
                Log.e("SmsListener", "Error in getAnalysis: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
        return null;
    }

    int retryCountNew = 5;

    public String processUrls(Context context, String url) throws IOException, JSONException {
        // POST request https://docs.virustotal.com/reference/scan-url

        // Get the pinned OkHttpClient
        OkHttpClient client = NetworkClient.getPinnedHttpClient();

        String encodedUrl = URLEncoder.encode(url, "UTF-8");
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "url=" + encodedUrl);

        for (int i = 0; i < retryCountNew; i++) {
            try {
                Request request = new Request.Builder()
                        .url("https://www.virustotal.com/api/v3/urls")
                        .post(body)
                        .addHeader("accept", "application/json")
                        .addHeader("x-apikey", API_KEY)
                        .addHeader("content-type", "application/x-www-form-urlencoded")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    return jsonResponse.getJSONObject("data").getString("id");
                }
            } catch (IOException | JSONException e) {
                Log.e("TestListener", "Attempt " + (i + 1) + " failed", e);
                if (i == retryCountNew - 1) {
                    invalidURL(context, url);
                    throw e;  // Final attempt failed, exception
                }
            }
        }
        return null;
    }

}
