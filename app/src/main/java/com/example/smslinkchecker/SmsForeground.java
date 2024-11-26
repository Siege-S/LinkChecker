package com.example.smslinkchecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SmsForeground extends Service { // Android Documentation: https://developer.android.com/develop/background-work/services/foreground-services

    public static final String CHANNEL_ID = "1001";
    public static final String API_KEY = BuildConfig.VT_API_KEY;
    public static final String ss_API_KEY = BuildConfig.SS_API_KEY;

    //Adapted From: https://www.youtube.com/watch?v=2eT0QWJFJeY
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Retrieve data sent to the service
        String sender = intent.getStringExtra("sender");
        List<String> urls = intent.getStringArrayListExtra("urls");

        if (sender != null && urls != null && !urls.isEmpty()) {
            // Create the notification channel (Android 8.0+)
            createNotificationChannel();
            System.out.println("Notification Channel Created...");

            processUrlsWithInternet(this, urls, sender, new DBHelper(this));
        }


        return START_STICKY; // Service will restart if killed by the system
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Create the notification channel for Android 8.0 and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "SMS Service";
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for SMS processing service.");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    public void processUrlsWithInternet(Context context, List<String> urls, String msgFrom, DBHelper dbHelper) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            for (String url : urls) {
                System.out.println("URL: " + url);
                System.out.println("Url index: " + (urls.indexOf(url) + 200));
                int notificationID = urls.indexOf(url) + 200;

                // Create a foreground notification
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("LinkGuard is Online")
                        .setContentText("Analyzing link. . .")
                        .setSmallIcon(R.drawable.ic_notification)
                        .build();

                // Start the foreground service
                startForeground(1, notification);
                System.out.println("ForeGround Service Started...");

                createNotification(context, "LinkGuard Detected a URL", url, notificationID);
                executorService.execute(() -> {

                    // Get the pinned OkHttpClient
                    OkHttpClient client = NetworkClient.getPinnedHttpClient();
                    JSONObject jsonResponse;
                    String analysisId;
                    try {
                        // POST request to VirusTotal API
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
                            System.out.println("postResponseBody: " + responseBody);
                            jsonResponse = new JSONObject(responseBody);
                            analysisId = jsonResponse.getJSONObject("data").getString("id");
                        }

                        // GET request to check analysis status
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

                                // If the status is not queued, break out of the loop
                                if (!attributes.getString("status").equals("queued")) {
                                    break;
                                }
                                Thread.sleep(2000); // 2 second delay
                            }
                        }

                        // Screenshot API
                        String apiUrl = SnapshotmachineAPI(url);
                        Bitmap bitmap = BitmapFactory.decodeStream(new URL(apiUrl).openStream());
                        byte[] image = getBitmapAsByteArray(bitmap);

                        // Send the data to the database
                        JSONObject finalJsonResponse = jsonResponse;
                        String analysisResultJSON = jsonResponse.toString();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            String analysis = NotifyResult(context, url, msgFrom, String.valueOf(finalJsonResponse), notificationID);

                            if(dbHelper.duplicateURL(url, msgFrom)){
                                System.out.println("SmsForeground: Duplicate Entry - (" + msgFrom + " : " + url + ") Updating Data. . .");
                                dbHelper.updateData(url, msgFrom, apiUrl, analysis, image, analysisResultJSON);
                            } else {
                                System.out.println("SmsForeground: New Entry - (" + msgFrom + " : " + url + ") Inserting Data. . .");
                                dbHelper.insertData(url, msgFrom, apiUrl, analysis, image, analysisResultJSON);
                            }


                            // Process Completed
                            System.out.println("Process Completed. . .\nStopping ForeGround Service...");
                            stopForeground(true);  // Stop the foreground service
                            stopSelf();  // Stop the service entirely
                        });

                    } catch (IOException | JSONException | InterruptedException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        // Process Failed
                        System.out.println("Process Failed. . .\nStopping ForeGround Service...");
                        stopForeground(true);  // Stop the foreground service
                        stopSelf();  // Stop the service entirely
                        showRetryNotification(context, url, msgFrom, notificationID); // show retry notification
                    }
                });
            }
        } finally {
            // Stop service only after all tasks are completed
            executorService.shutdown();
        }
    }


    public String NotifyResult(Context context, String url, String sender, String JSON, int notificationID){
        NotificationManager notificationManager = (NotificationManager)  context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Open Main Activity
        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.putExtra("dismiss", notificationID);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Wrap the Intent in a PendingIntent with FLAG_IMMUTABLE
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                context,
                5, // requestCode
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Report as Spam or Delete SMS Action
        Intent reportIntent = new Intent(context, MainActivity.class);
        reportIntent.putExtra("sender", sender);
        reportIntent.putExtra("notificationID", notificationID);
        reportIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create a PendingIntent for the Activity
        PendingIntent reportPendingIntent = PendingIntent.getActivity(
                context,
                4, // Unique request code
                reportIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String setTitle;
        String setContent;
        String result;
        try {
            JSONObject jsonObject = new JSONObject(JSON);

            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt( "malicious");
            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");
            int harmless = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("harmless");

            if(malicious > 0 && suspicious > 0){ // 3 if malicious and suspicious
                setTitle = "Link is Malicious and Suspicious";
                setContent = "URL: '" + url + "' Consider reporting this as Spam or Deleting this SMS";
                result = "3";
            }
            else if(malicious > 0){ // 1 if malicious
                setTitle = "Link is Malicious";
                setContent = "URL: '" + url + "' Consider reporting this as Spam or Deleting this SMS";
                result = "1";
            }
            else if(suspicious > 0){ // 2 if suspicious
                setTitle = "Link is Suspicious";
                setContent = "URL: '" + url + "'";
                result = "2";
            }
            else { // 0 if harmless
                setTitle = "Link is Harmless";
                setContent = "URL: '" + url + "'";
                result = "0";
            }

            // Notification 1
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(setTitle)
                    .setContentText(setContent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            if(malicious > 0){
                builder.addAction(R.drawable.report, "Report as Spam or Delete SMS", reportPendingIntent);
                builder.addAction(R.drawable.next, "Check Results", appPendingIntent);
            } else {
                builder.addAction(R.drawable.next, "Check Results", appPendingIntent);
                builder.addAction(R.drawable.report, "Report as Spam or Delete SMS", reportPendingIntent);
            }
            System.out.println("Notify Result: " + url + " : " + notificationID);
            notificationManager.notify(notificationID, builder.build());
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

    public void createNotification(Context context, String title, String url, int notificationID) {
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
        System.out.println("Create Notification: " + url + " : " + notificationID);
        notificationManager.notify(notificationID, builder.build());
    }

    public void showRetryNotification(Context context, String url, String sender, int notificationID) {
        // Retry Action Intent
        Intent retryIntent = new Intent(context, RetryReceiver.class);
        retryIntent.putExtra("url", url);
        retryIntent.putExtra("sender", sender);
        retryIntent.putExtra("notificationID", notificationID);

        // Use FLAG_IMMUTABLE if targeting Android 12 or higher, otherwise use FLAG_UPDATE_CURRENT
        PendingIntent retryPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Save for Later Action
        Intent saveIntent = new Intent(context, SaveForLaterService.class); // Use IntentService for simplicity
        saveIntent.putExtra("url", url);
        saveIntent.putExtra("sender", sender);
        saveIntent.putExtra("notificationID", notificationID);
        PendingIntent savePendingIntent = PendingIntent.getService(
                context,
                1, // Different request code
                saveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Scan Failed (" + url + ")")
//                .setContentText("Tap to retry scanning the URL.")
                .setContentText("Retry scanning the URL or save for later.")
                .addAction(R.drawable.retry, "Retry", retryPendingIntent)
                .addAction(R.drawable.save, "Save for Later", savePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, builder.build());
    }
}
