package com.example.smslinkchecker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
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
import java.util.ArrayList;
import java.util.List;
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
    private ExecutorService executorService;

    private static final int POLLING_INTERVAL_MS = 10000; // 10 seconds

    @Override
    public void onReceive(Context context, Intent intent) {
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

                            Pattern URL_PATTERN = Patterns.WEB_URL; // Regular expression pattern to detect Web URLs
                            Matcher URL_MATCHER = URL_PATTERN.matcher(msgBody);
                            while (URL_MATCHER.find()) {
                                String detectedUrl = URL_MATCHER.group();
                                urls.add(detectedUrl);  // Add URL to list
                                Toast.makeText(context, "URL Detected: " + detectedUrl, Toast.LENGTH_LONG).show();
                                Log.v("URL", detectedUrl);
                            }
                            if(!MessageFragment.isInternetConnected(context)){
                                Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
                                noInternet(context, urls.toString());
                            } else {
                                // Process each URL in the list
                                for (String url : urls) {
                                    createNotification(context, "URL Detected in SMS message", url);
                                    // Method Call API return API URL
                                    String apiUrl = SnapshotmachineAPI(url);
                                    String finalMsg_from = msg_from;

                                    // Scan Detected URL and Get Analysis Result using VirusTotal API
                                    executorService = Executors.newSingleThreadExecutor();
                                    executorService.execute(() -> {
                                        try {
                                            String analysisId = scanURL(context, url);
                                            String analysisResultJSON = getAnalysis(analysisId);

                                            if (analysisResultJSON != null) {
                                                Bitmap bitmap;
                                                InputStream in = new URL(apiUrl).openStream();
                                                bitmap = BitmapFactory.decodeStream(in);
                                                byte[] image = getBitmapAsByteArray(bitmap);

                                                //Notification for analysis
                                                String analysis = NotifyResult(context, url, analysisResultJSON);

                                                // Insert into database in SQLite
                                                DBHelper dbHelper = new DBHelper(context);
                                                dbHelper.insertData(url, msgBody, finalMsg_from, apiUrl, analysis, image, analysisResultJSON);
                                            }
                                        } catch (IOException e) {
                                            System.out.println("NO Internet Connection");
                                            e.printStackTrace();
                                        }
                                    });
                                }
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


            }
        }
    }

    private String NotifyResult(Context context,String url, String JSON){
        NotificationManager notificationManager = (NotificationManager)  context.getSystemService(Context.NOTIFICATION_SERVICE);

// Create a notification channel if targeting Android 8.0 or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Notification 1
        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("VirusTotal identified the URL as suspicious or a phishing threat.")
                .setContentText("URL: "+url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Notification 2
        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("VirusTotal identified the URL as Malicious.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        // Notification 3
        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("VirusTotal identified the URL as both malicious and a phishing threat.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        // Notification 4
        NotificationCompat.Builder builder4 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("VirusTotal report shows the URL is Harmless.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            JSONObject jsonObject = new JSONObject(JSON);

            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt( "malicious");
            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");
            int harmless = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("harmless");

            if(malicious > 0 && suspicious > 0){
                notificationManager.notify(3, builder3.build()); // Notification ID 3
                System.out.println("Link is Malicious and Suspicious" + url);
                return "VirusTotal identified the URL as both malicious and a phishing threat";
            }
            else if(malicious > 0){
                notificationManager.notify(2, builder2.build()); // Notification ID 2
                System.out.println("Link is Malicious" + url);
                return "VirusTotal identified the URL as Malicious";
            }
            else if(suspicious > 0){
                notificationManager.notify(1, builder1.build()); // Notification ID 1
                System.out.println("Link is Suspicious/Phishing" + url);
                return "VirusTotal identified the URL as suspicious or a phishing threat";
            }
            else {
                notificationManager.notify(4, builder4.build()); // Notification ID 4
                return "VirusTotal report shows the URL is Harmless";
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String SnapshotmachineAPI(String url) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // Call ScreenshotMachine API
        String customerKey = "990acf";
        String secretPhrase = ""; // leave secret phrase empty if not needed

        ScreenshoMachine sm = new ScreenshoMachine(customerKey, secretPhrase);
        Map<String, String> options = new HashMap<>();
        options.put("url", url);
        options.put("dimension", "1366x768");
        options.put("device", "desktop");
        options.put("format", "png");
        options.put("cacheLimit", "0");
        options.put("delay", "5000");
        options.put("zoom", "100");

        String apiUrl = sm.generateScreenshotApiUrl(options);
        Log.v("Screenshot API URL", apiUrl);

        return apiUrl;
    }

    private byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    private void createNotification(Context context, String title, String url) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText("Exercise caution and await the analysis of this URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private String scanURL(Context context, String url) throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "url=" + url);
        Request request = new Request.Builder()
                .url("https://www.virustotal.com/api/v3/urls")
                .post(body)
                .addHeader("accept", "application/json")
                .addHeader("x-apikey", API_KEY)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.v("ScanURL", "Unexpected code " + response);
                throw new IOException("Unexpected code " + response);
            }

            // Handle the response here
            String responseBody = response.body().string();
            System.out.println("Response received: " + responseBody);

            // Assuming the response is JSON, you can parse it to extract the analysis ID
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getJSONObject("data").getString("id");

        } catch (IOException | JSONException e) {
            invalidURL(context, url);
            e.printStackTrace();
            return null;
        }
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
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Invalid URL")
                .setContentText("This URL does not exist" + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        notificationManager.notify(6, builder.build());
    }
    private void noInternet(Context context, String url) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("No Internet Connection")
                .setContentText("Cannot process URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        notificationManager.notify(7, builder.build());
    }

    private String getAnalysis(String analysisId) throws IOException {
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


}
