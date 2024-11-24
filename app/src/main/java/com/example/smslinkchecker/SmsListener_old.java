//package com.example.smslinkchecker;
//
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.telephony.SmsMessage;
//import android.util.Log;
//import android.util.Patterns;
//import android.widget.Toast;
//
//import androidx.core.app.NotificationCompat;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.regex.Matcher;
//
//import okhttp3.MediaType;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.RequestBody;
//import okhttp3.Response;
//
//public class SmsListener_old extends BroadcastReceiver {
//    private static final String CHANNEL_ID = "1001";
//    private static final String API_KEY = BuildConfig.VT_API_KEY;
//    private static final String ss_API_KEY = BuildConfig.SS_API_KEY;
//    private static final int POLLING_INTERVAL_MS = 10000; // 10 seconds
//
//    // Modified smslistener (puro error kase)
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;
//
//        DBHelper dbHelper = new DBHelper(context);
//        SmsMessage[] messages = extractSmsMessages(intent);
//        if (messages == null) {
//            return;
//        }
//        for (SmsMessage message : messages) {
//            String msgFrom = message.getOriginatingAddress();
//            String msgBody = message.getMessageBody();
//            Log.v("URLs", "From: " + msgFrom + " , Body: " + msgBody);
//
//            // Method Regular Expression
//            List<String> urls = extractUrlsFromMessage(msgBody, context);
//
//            if (!MessageFragment.isInternetConnected(context)) {
//                handleOfflineUrls(context, urls, msgFrom, dbHelper);
//            } else {
//                processUrlsWithInternet(context, urls, msgFrom, dbHelper);
//            }
//        }
//    }
//
//    private SmsMessage[] extractSmsMessages(Intent intent) {
//        Bundle bundle = intent.getExtras();
//        if (bundle == null) return null;
//
//        Object[] pdus = (Object[]) bundle.get("pdus");
//        SmsMessage[] messages = new SmsMessage[pdus.length];
//        for (int i = 0; i < pdus.length; i++) {
//            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
//        }
//        return messages;
//    }
//
//    private List<String> extractUrlsFromMessage(String msgBody, Context context) {
//        // Regular Expression
//        List<String> urls = new ArrayList<>();
//        Matcher matcher = Patterns.WEB_URL.matcher(msgBody);
//        while (matcher.find()) {
//            String detectedUrl = matcher.group();
//            urls.add(detectedUrl);
//            Toast.makeText(context, "URL Detected: " + detectedUrl, Toast.LENGTH_LONG).show();
//            Log.v("URL", detectedUrl);
//        }
//        return urls;
//    }
//
//    private void handleOfflineUrls(Context context, List<String> urls, String msgFrom, DBHelper dbHelper) {
//        Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
//        int notificationID = 0;
//        for (String url : urls) {
//            notificationID++;
//            if (dbHelper.insertOfflineTbl(url, msgFrom)) {
//                noInternet(context, url, notificationID);
//            }
//        }
//    }
//
//    private void processUrlsWithInternet(Context context, List<String> urls, String msgFrom, DBHelper dbHelper) {
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        try {
//            for (String url : urls) {
//                createNotification(context, "URL Detected in SMS message", url, 100);
//                executorService.execute(() -> processSingleUrl(context, url, msgFrom, dbHelper));
//            }
//        } finally {
//            executorService.shutdown();
//        }
//    }
//
//    private void processSingleUrl(Context context, String url, String msgFrom, DBHelper dbHelper) {
//        try {
//            String apiUrl = SnapshotmachineAPI(url);
//            String analysisId = processUrls(context, url);
//            if (analysisId != null) {
//                handleAnalysisResult(context, url, msgFrom, apiUrl, analysisId, dbHelper);
//            } else {
//                showRetryNotification(context, url, msgFrom);
//            }
//        } catch (Exception e) {
//            Log.e("Error", "Failed to process URL", e);
//            showRetryNotification(context, url, msgFrom);
//        }
//    }
//
//    private void handleAnalysisResult(Context context, String url, String msgFrom, String apiUrl, String analysisId, DBHelper dbHelper) throws IOException, JSONException, NoSuchAlgorithmException {
//        String analysisResultJSON = getAnalysis(analysisId);
//        if (analysisResultJSON != null) {
//            Bitmap bitmap = BitmapFactory.decodeStream(new URL(apiUrl).openStream());
//            byte[] image = getBitmapAsByteArray(bitmap);
//
//            new Handler(Looper.getMainLooper()).post(() -> {
//                String analysis = NotifyResult(context, url, analysisResultJSON);
//                dbHelper.insertData(url, msgFrom, apiUrl, analysis, image, analysisResultJSON);
//            });
//        } else {
//            showRetryNotification(context, url, msgFrom);
//        }
//    }
//
//    public void showRetryNotification(Context context, String url, String sender) {
//        Intent retryIntent = new Intent(context, RetryReceiver.class);
//        retryIntent.putExtra("url", url);
//        retryIntent.putExtra("sender", sender);
//
//        // Use FLAG_IMMUTABLE if targeting Android 12 or higher, otherwise use FLAG_UPDATE_CURRENT
//        PendingIntent retryPendingIntent = PendingIntent.getBroadcast(
//                context,
//                0,
//                retryIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0)
//        );
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("Scan Failed")
//                .setContentText("Tap to retry scanning the URL.")
//                .addAction(R.drawable.retry, "Retry", retryPendingIntent)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true);
//
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(100, builder.build());
//    }
//
//
//    public String NotifyResult(Context context,String url, String JSON){
//        NotificationManager notificationManager = (NotificationManager)  context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//
//// Create a notification channel if targeting Android 8.0 or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "SMS Channel",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Create an Intent to open your main Activity when the notification is clicked
//        Intent intent = new Intent(context, MainActivity.class); // Replace with your main activity
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Ensure app is brought to foreground
//
//        // Wrap the Intent in a PendingIntent with FLAG_IMMUTABLE
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context,
//                0, // requestCode
//                intent,
//                PendingIntent.FLAG_IMMUTABLE // Required for Android 12+
//        );
//
//        // Notification 1
//        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("LinkGuard identified the URL as suspicious.")
//                .setContentText("URL: "+url)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true) // Dismiss the notification when clicked
//                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification
//
//        // Notification 2
//        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("LinkGuard identified the URL as Malicious.")
//                .setContentText("URL: " + url)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true) // Dismiss the notification when clicked
//                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification
//        // Notification 3
//        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("LinkGuard identified the URL as both Malicious and Suspicious.")
//                .setContentText("URL: " + url)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true) // Dismiss the notification when clicked
//                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification
//        // Notification 4
//        NotificationCompat.Builder builder4 = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("LinkGuard report shows the URL is Harmless.")
//                .setContentText("URL: " + url)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true) // Dismiss the notification when clicked
//                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification
//
//        try {
//            JSONObject jsonObject = new JSONObject(JSON);
//
//            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt( "malicious");
//            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");
//            int harmless = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("harmless");
//
//            if(malicious > 0 && suspicious > 0){ // 3 if malicious and suspicious
//                notificationManager.notify(100, builder3.build()); // Notification ID 3
//                System.out.println("Link is Malicious and Suspicious" + url);
//                return "3";
//            }
//            else if(malicious > 0){ // 1 if malicious
//                notificationManager.notify(100, builder2.build()); // Notification ID 2
//                System.out.println("Link is Malicious" + url);
//                return "1";
//            }
//            else if(suspicious > 0){ // 2 if suspicious
//                notificationManager.notify(100, builder1.build()); // Notification ID 1
//                System.out.println("Link is Suspicious/Phishing" + url);
//                return "2";
//            }
//            else { // 0 if harmless
//                notificationManager.notify(100, builder4.build()); // Notification ID 4
//                return "0";
//            }
//
//        } catch (JSONException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public String SnapshotmachineAPI(String url) throws UnsupportedEncodingException, NoSuchAlgorithmException {
//        // Call ScreenshotMachine API
//        String customerKey = ss_API_KEY;
//        String secretPhrase = ""; // leave secret phrase empty if not needed
//        ScreenshoMachine sm = new ScreenshoMachine(customerKey, secretPhrase);
//        Map<String, String> options = new HashMap<>();
//        options.put("url", url);
//        options.put("dimension", "1366x768");
//        options.put("device", "desktop");
//        options.put("format", "png");
//        options.put("cacheLimit", "0");
//        options.put("delay", "8000"); // 10 seconds
//        options.put("zoom", "100");
//
//        String apiUrl = sm.generateScreenshotApiUrl(options);
//        Log.v("Screenshot API URL", apiUrl);
//
//        return apiUrl;
//    }
//
//    public byte[] getBitmapAsByteArray(Bitmap bitmap) {
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
//        return outputStream.toByteArray();
//    }
//
//    private void createNotification(Context context, String title, String url, int notificationID) {
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Create NotificationChannel for Android Oreo and higher
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Create an Intent to open your main Activity when the notification is clicked
//        Intent intent = new Intent(context, MainActivity.class); // Replace with your main activity
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Ensure app is brought to foreground
//
//        // Wrap the Intent in a PendingIntent with FLAG_IMMUTABLE
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context,
//                0, // requestCode
//                intent,
//                PendingIntent.FLAG_IMMUTABLE // Required for Android 12+
//        );
//
//        // Build the notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle(title)
//                .setContentText("Exercise caution and await the analysis of this URL: " + url)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true) // Dismiss the notification when clicked
//                .setContentIntent(pendingIntent); // Set the intent that will fire when the user taps the notification
//
//        // Show the notification
//        notificationManager.notify(notificationID, builder.build());
//    }
//
////    private static final int MAX_RETRIES = 3; // Number of retries
//
////    public String scanURL(Context context, String url) throws IOException {
////        OkHttpClient client = new OkHttpClient();
////        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
////        String encodedUrl = URLEncoder.encode(url, "UTF-8");
////        RequestBody body = RequestBody.create(mediaType, "url=" + encodedUrl);
////        Request request = new Request.Builder()
////                .url("https://www.virustotal.com/api/v3/urls")
////                .post(body)
////                .addHeader("accept", "application/json")
////                .addHeader("x-apikey", API_KEY)
////                .addHeader("content-type", "application/x-www-form-urlencoded")
////                .build();
////
////        int retryCount = 0;
////        while (retryCount < MAX_RETRIES) {
////            try (Response response = client.newCall(request).execute()) {
////                if (!response.isSuccessful()) {
////                    Log.e("ScanURL", "Request failed with code: " + response.code() + " - " + response.message());
////                    System.out.println("Scan URL Error: " + response.code() + " - " + response.message());
////                    throw new IOException("Unexpected code " + response);
////                }
////
////                String responseBody = response.body().string();
////                Log.d("ScanURL", "Response received: " + responseBody);
////                System.out.println("Scan URL Response: " + responseBody);
////
////                JSONObject jsonResponse = new JSONObject(responseBody);
////                return jsonResponse.getJSONObject("data").getString("id");
////
////            } catch (IOException | JSONException e) {
////                Log.e("ScanURL", "Error in scanURL attempt " + retryCount + ": " + e.getMessage());
////                e.printStackTrace();
////
////                retryCount++;
////                if (retryCount >= MAX_RETRIES) {
////                    invalidURL(context, url); // Handle the failure after max retries
////                    return null;
////                }
////
////                try {
////                    // Wait before retrying (you can adjust the interval as needed)
////                    Thread.sleep(2000); // 2 seconds delay before retrying
////                } catch (InterruptedException interruptedException) {
////                    Thread.currentThread().interrupt();
////                    return null;
////                }
////            }
////        }
////        return null;
////    }
//
//
//    private void invalidURL(Context context, String url) {
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Create NotificationChannel for Android Oreo and higher
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Build the notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("Error in Scanning URL")
//                .setContentText("URL: " + url)
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//        // Show the notification
//        notificationManager.notify(6, builder.build());
//    }
//    private void noInternet(Context context, String url, int notificationID) {
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Create NotificationChannel for Android Oreo and higher
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Build the notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("LinkGuard is Offline, '" + url + "' is saved for later analysis")
//                .setContentText("Please avoid clicking this url '" + url + "', until connection is stored.")
//                .setPriority(NotificationCompat.PRIORITY_HIGH);
//
//        // Show the notification
//        notificationManager.notify(notificationID, builder.build());
//    }
//
//    public String getAnalysis(String analysisId) throws IOException {
//        if (analysisId == null || analysisId.isEmpty()) {
//            Log.e("SmsListener", "No Analysis ID provided.");
//            return null;
//        }
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(60, TimeUnit.SECONDS)
//                .readTimeout(60, TimeUnit.SECONDS)
//                .writeTimeout(60, TimeUnit.SECONDS)
//                .build();
//
//        Request request = new Request.Builder()
//                .url("https://www.virustotal.com/api/v3/analyses/" + analysisId)
//                .get()
//                .addHeader("accept", "application/json")
//                .addHeader("x-apikey", API_KEY)
//                .build();
//
//        while (true) {
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new IOException("Unexpected code " + response);
//                }
//
//                String responseBody = response.body().string();
//                Log.d("SmsListener", "getAnalysis Response: " + responseBody);
//                System.out.println("Get Analysis Response received: " + responseBody);
//
//                JSONObject jsonResponse = new JSONObject(responseBody);
//                JSONObject attributes = jsonResponse.getJSONObject("data").getJSONObject("attributes");
//
//                if (!attributes.getString("status").equals("queued")) {
//                    Log.d("SmsListener", "Analysis Results: " + jsonResponse.toString(2));
//                    return jsonResponse.toString();
//                } else {
//                    Log.d("SmsListener", "Analysis still queued. Checking again in 10 seconds.");
//                    Thread.sleep(POLLING_INTERVAL_MS);
//                }
//
//            } catch (IOException | JSONException | InterruptedException e) {
//                Log.e("SmsListener", "Error in getAnalysis: " + e.getMessage());
//                e.printStackTrace();
//                break;
//            }
//        }
//        return null;
//    }
//
//    int retryCountNew = 3;
//
//    public String processUrls(Context context, String url) throws IOException, JSONException {
//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(30, TimeUnit.SECONDS)
//                .writeTimeout(30, TimeUnit.SECONDS)
//                .build();
//
//        String encodedUrl = URLEncoder.encode(url, "UTF-8");
//        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
//        RequestBody body = RequestBody.create(mediaType, "url=" + encodedUrl);
//
//        for (int i = 0; i < retryCountNew; i++) {
//            try {
//                Request request = new Request.Builder()
//                        .url("https://www.virustotal.com/api/v3/urls")
//                        .post(body)
//                        .addHeader("accept", "application/json")
//                        .addHeader("x-apikey", API_KEY)
//                        .addHeader("content-type", "application/x-www-form-urlencoded")
//                        .build();
//
//                try (Response response = client.newCall(request).execute()) {
//                    if (!response.isSuccessful()) {
//                        throw new IOException("Unexpected code " + response);
//                    }
//
//                    String responseBody = response.body().string();
//                    JSONObject jsonResponse = new JSONObject(responseBody);
//                    return jsonResponse.getJSONObject("data").getString("id");
//                }
//            } catch (IOException | JSONException e) {
//                Log.e("TestListener", "Attempt " + (i + 1) + " failed", e);
//                if (i == retryCountNew - 1) {
//                    invalidURL(context, url);
//                    throw e;  // Final attempt failed, exception
//                }
//            }
//        }
//        return null;
//    }
//
//}
