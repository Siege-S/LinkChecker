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
import android.telephony.SmsMessage;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

// Regex
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// screenshotmachine API
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

// another
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class SmsListener extends BroadcastReceiver {
    private static final String CHANNEL_ID = "1001";
    private static final int NOTIFICATION_ID = 123;
    private static final String API_KEY = "d2a66c9f38303515894f1721ed3aaf695f9ec0eb6ab81c000ef3b4aa228bad94";

    private static final int POLLING_INTERVAL_MS = 10000; // 10 seconds

    @Override
    public void onReceive(Context context, Intent intent) {
        DBHelper dbHelper = new DBHelper(context);

        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs;
            String msg_from;
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        Log.v("URLs", "From: " + msg_from + " , Body: " + msgBody);

                        // List to store detected URLs
                        List<String> urls = new ArrayList<>();
                        String detectedUrl;
                        Pattern URL_PATTERN = Patterns.WEB_URL; // Regular expression pattern to detect Web URLs
                        Matcher URL_MATCHER = URL_PATTERN.matcher(msgBody);
                        while (URL_MATCHER.find()) {
                            detectedUrl = URL_MATCHER.group();
                            urls.add(detectedUrl);  // Add URL to list
                            Toast.makeText(context, "URL Detected: " + detectedUrl, Toast.LENGTH_LONG).show();
                            Log.v("URL", detectedUrl);
                        }

                        if (!MessageFragment.isInternetConnected(context)) {
                            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
                            int notificationID = 0;
                            for (String url : urls) {
                                notificationID++;
                                if (dbHelper.insertOfflineTbl(url, msg_from, msgBody)) {
                                    noInternet(context, url, notificationID);
                                }
                            }
                        } else {
                            // Create a single ExecutorService for processing URLs
                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            try {
                                // Process each URL in the list
                                for (String url : urls) {
                                    int notifID = 0;
                                    createNotification(context, "URL Detected in SMS message", url, notifID);
                                    notifID++;
                                    String finalMsg_from = msg_from;
                                    executorService.execute(() -> {
                                        try {
                                            // Method Call API return API URL
                                            String apiUrl = SnapshotmachineAPI(url);
                                            // Asynchronously scan URL
                                            String analysisId = processUrls(context, url);

                                            if (analysisId != null) {
                                                // Asynchronously get analysis result
                                                String analysisResultJSON = getAnalysis(analysisId);

                                                if (analysisResultJSON != null) {
                                                    // Download image and process the analysis result on a background thread
                                                    Bitmap bitmap;
                                                    InputStream in = new URL(apiUrl).openStream();
                                                    bitmap = BitmapFactory.decodeStream(in);
                                                    byte[] image = getBitmapAsByteArray(bitmap);

                                                    // Notify the user with the analysis result on the main thread
                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                        String analysis = NotifyResult(context, url, analysisResultJSON);

                                                        // Insert into database in SQLite
                                                        dbHelper.insertData(url, finalMsg_from, msgBody, apiUrl, analysis, image, analysisResultJSON);
                                                    });
                                                } else {
                                                    showRetryNotification(context, url, msgBody, finalMsg_from);
                                                    System.out.println("Failed to get analysis result.");
                                                }
                                            } else {
                                                showRetryNotification(context, url, msgBody, finalMsg_from);
                                                System.out.println("Failed to scan URL.");
                                            }
                                        } catch (IOException e) {
                                            System.out.println("Error in VirusTotal:" + e.getMessage());
                                            showRetryNotification(context, url, msgBody, finalMsg_from);
                                            e.printStackTrace();
                                        } catch (NoSuchAlgorithmException e) {
                                            throw new RuntimeException(e);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    });
                                }
                            } finally {
                                // Shutdown the executor service after all tasks are submitted
                                executorService.shutdown();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void showRetryNotification(Context context, String url, String sender, String message) {
        Intent retryIntent = new Intent(context, RetryReceiver.class);
        retryIntent.putExtra("url", url);
        retryIntent.putExtra("message", message);
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


    public String NotifyResult(Context context,String url, String JSON){
        NotificationManager notificationManager = (NotificationManager)  context.getSystemService(Context.NOTIFICATION_SERVICE);


// Create a notification channel if targeting Android 8.0 or above
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

        // Notification 1
        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("LinkGuard identified the URL as suspicious or a phishing threat.")
                .setContentText("URL: "+url)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss the notification when clicked
                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification

        // Notification 2
        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("LinkGuard identified the URL as Malicious.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss the notification when clicked
                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification
        // Notification 3
        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("LinkGuard identified the URL as both malicious and a phishing threat.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss the notification when clicked
                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification
        // Notification 4
        NotificationCompat.Builder builder4 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("LinkGuard report shows the URL is Harmless.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss the notification when clicked
                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification

        try {
            JSONObject jsonObject = new JSONObject(JSON);

            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt( "malicious");
            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");
            int harmless = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("harmless");

            if(malicious > 0 && suspicious > 0){ // 3 if malicious and suspicious
                notificationManager.notify(3, builder3.build()); // Notification ID 3
                System.out.println("Link is Malicious and Suspicious" + url);
                return "3";
            }
            else if(malicious > 0){ // 1 if malicious
                notificationManager.notify(2, builder2.build()); // Notification ID 2
                System.out.println("Link is Malicious" + url);
                return "1";
            }
            else if(suspicious > 0){ // 2 if suspicious
                notificationManager.notify(1, builder1.build()); // Notification ID 1
                System.out.println("Link is Suspicious/Phishing" + url);
                return "2";
            }
            else { // 0 if harmless
                notificationManager.notify(4, builder4.build()); // Notification ID 4
                return "0";
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String SnapshotmachineAPI(String url) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // Call ScreenshotMachine API
        String customerKey = "990acf"; // 990acf // e83172
        String secretPhrase = ""; // leave secret phrase empty if not needed
        ScreenshoMachine sm = new ScreenshoMachine(customerKey, secretPhrase);
        Map<String, String> options = new HashMap<>();
        options.put("url", url);
        options.put("dimension", "1366x768");
        options.put("device", "desktop");
        options.put("format", "png");
        options.put("cacheLimit", "0");
        options.put("delay", "10000"); // 10 seconds
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

    private static final int MAX_RETRIES = 3; // Number of retries

//    public String scanURL(Context context, String url) throws IOException {
//        OkHttpClient client = new OkHttpClient();
//        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
//        String encodedUrl = URLEncoder.encode(url, "UTF-8");
//        RequestBody body = RequestBody.create(mediaType, "url=" + encodedUrl);
//        Request request = new Request.Builder()
//                .url("https://www.virustotal.com/api/v3/urls")
//                .post(body)
//                .addHeader("accept", "application/json")
//                .addHeader("x-apikey", API_KEY)
//                .addHeader("content-type", "application/x-www-form-urlencoded")
//                .build();
//
//        int retryCount = 0;
//        while (retryCount < MAX_RETRIES) {
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    Log.e("ScanURL", "Request failed with code: " + response.code() + " - " + response.message());
//                    System.out.println("Scan URL Error: " + response.code() + " - " + response.message());
//                    throw new IOException("Unexpected code " + response);
//                }
//
//                String responseBody = response.body().string();
//                Log.d("ScanURL", "Response received: " + responseBody);
//                System.out.println("Scan URL Response: " + responseBody);
//
//                JSONObject jsonResponse = new JSONObject(responseBody);
//                return jsonResponse.getJSONObject("data").getString("id");
//
//            } catch (IOException | JSONException e) {
//                Log.e("ScanURL", "Error in scanURL attempt " + retryCount + ": " + e.getMessage());
//                e.printStackTrace();
//
//                retryCount++;
//                if (retryCount >= MAX_RETRIES) {
//                    invalidURL(context, url); // Handle the failure after max retries
//                    return null;
//                }
//
//                try {
//                    // Wait before retrying (you can adjust the interval as needed)
//                    Thread.sleep(2000); // 2 seconds delay before retrying
//                } catch (InterruptedException interruptedException) {
//                    Thread.currentThread().interrupt();
//                    return null;
//                }
//            }
//        }
//        return null;
//    }


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
                .setContentTitle("No Internet Connection")
                .setContentText("URL is saved for later analysis: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        notificationManager.notify(notificationID, builder.build());
    }

    public String getAnalysis(String analysisId) throws IOException {
        if (analysisId == null || analysisId.isEmpty()) {
            Log.e("SmsListener", "No Analysis ID provided.");
            return null;
        }

        OkHttpClient client = new OkHttpClient();
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
                    Log.d("SmsListener", "Analysis still queued. Checking again in 10 seconds.");
                    Thread.sleep(POLLING_INTERVAL_MS);
                }

            } catch (IOException | JSONException | InterruptedException e) {
                Log.e("SmsListener", "Error in getAnalysis: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
        return null;
    }

    int retryCountNew = 3;

    public String processUrls(Context context, String url) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

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
                    throw e;  // Final attempt failed, propagate the exception
                }
            }
        }
        return null;
    }

}
