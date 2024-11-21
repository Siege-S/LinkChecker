package com.example.smslinkchecker;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RetryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String url = intent.getStringExtra("url");
        String sender = intent.getStringExtra("sender");

        if (url != null) {
            // Re-attempt the scan when the retry action is triggered
            SmsListener smsListener = new SmsListener();
            DBHelper dbHelper = new DBHelper(context);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            try {
                    executorService.execute(() -> {
                        OkHttpClient client = NetworkClient.getPinnedHttpClient();

                        JSONObject jsonResponse;
                        String analysisId;
                        try {
                            // POST request
                            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                            RequestBody body = RequestBody.create(mediaType, "url=" + url);
                            Request postRequest = new Request.Builder()
                                    .url("https://www.virustotal.com/api/v3/urls")
                                    .post(body)
                                    .addHeader("accept", "application/json")
                                    .addHeader("x-apikey", SmsListener.API_KEY)
                                    .addHeader("content-type", "application/x-www-form-urlencoded")
                                    .build();

                            try (Response postResponse = client.newCall(postRequest).execute()) {
                                if (!postResponse.isSuccessful()) {
                                    throw new IOException("Unexpected code " + postResponse);
                                }
                                String postResponseBody = postResponse.body().string();
                                System.out.println("responseBody: " + postResponseBody);
                                jsonResponse = new JSONObject(postResponseBody);
                                analysisId = jsonResponse.getJSONObject("data").getString("id");
                                System.out.println("Analysis ID: " + analysisId);
                            }

                            // GET request
                            Request getRequest = new Request.Builder()
                                    .url("https://www.virustotal.com/api/v3/analyses/" + analysisId)
                                    .get()
                                    .addHeader("accept", "application/json")
                                    .addHeader("x-apikey", SmsListener.API_KEY)
                                    .build();

                            while (true) {
                                try (Response getResponse = client.newCall(getRequest).execute()) {
                                    if (!getResponse.isSuccessful()) {
                                        throw new IOException("Unexpected code " + getResponse);
                                    }
                                    String getResponseBody = getResponse.body().string();
                                    System.out.println("responseBody: " + getResponseBody);

                                    jsonResponse = new JSONObject(getResponseBody);
                                    JSONObject attributes = jsonResponse.getJSONObject("data").getJSONObject("attributes");

                                    Log.v("responseBody", jsonResponse.toString(2));
                                    System.out.println("Status: " + attributes.getString("status"));

                                    if (!attributes.getString("status").equals("queued")) {
                                        break;
                                    }
                                    Thread.sleep(1000);
                                }
                            }
                            // Screenshot using screenshotmachine API
                            String apiUrl = smsListener.SnapshotmachineAPI(url);
                            Bitmap bitmap = BitmapFactory.decodeStream(new URL(apiUrl).openStream());
                            byte[] image = smsListener.getBitmapAsByteArray(bitmap);

                            // Send to Database
                            JSONObject finalJsonResponse = jsonResponse;
                            String analysisResultJSON = jsonResponse.toString();
                            new Handler(Looper.getMainLooper()).post(() -> {
                                String analysis = smsListener.NotifyResult(context, url, String.valueOf(finalJsonResponse));
                                dbHelper.insertData(url, sender, apiUrl, analysis, image, analysisResultJSON);
                            });

                        } catch (IOException | JSONException | InterruptedException |
                                 NoSuchAlgorithmException e) {
                            smsListener.showRetryNotification(context, url, sender);
                            e.printStackTrace();
                        }
                    });

            } finally {
                executorService.shutdown();
            }
        }
        //            // Scan Detected URL and Get Analysis Result using VirusTotal API
//            ExecutorService executorService = Executors.newSingleThreadExecutor();
//            executorService.execute(() -> {
//                try {
//                    // Method Call API return API URL
//                    String apiUrl = smsListener.SnapshotmachineAPI(url);
//                    // Asynchronously scan URL
//                    String analysisId = smsListener.processUrls(context, url);
//
//                    if (analysisId != null) {
//                        // Asynchronously get analysis result
//                        String analysisResultJSON = smsListener.getAnalysis(analysisId);
//
//                        if (analysisResultJSON != null) {
//                            // Download image and process the analysis result on a background thread
//                            Bitmap bitmap;
//                            InputStream in = new URL(apiUrl).openStream();
//                            bitmap = BitmapFactory.decodeStream(in);
//                            byte[] image = smsListener.getBitmapAsByteArray(bitmap);
//
//                            // Notify the user with the analysis result on the main thread
//                            new Handler(Looper.getMainLooper()).post(() -> {
//                                String analysis = smsListener.NotifyResult(context, url, analysisResultJSON);
//
//                                // Insert into database in SQLite
//                                DBHelper dbHelper = new DBHelper(context);
//                                dbHelper.insertData(url, sender, apiUrl, analysis, image, analysisResultJSON);
//                            });
//                        } else {
//                            smsListener.showRetryNotification(context, url, sender);
//                            Log.v("GetAnalysis", "Failed to get analysis result.");
//                            System.out.println("Failed to get analysis result.");
//                        }
//                    } else {
//                        smsListener.showRetryNotification(context, url, sender);
//                        Log.v("ScanURL", "Failed to scan URL.");
//                        System.out.println("Failed to scan URL.");
//                    }
//                } catch (IOException e) {
//                    Log.v("SmsListener", "Error in VirusTotal: " + e.getMessage());
//                    smsListener.showRetryNotification(context, url, sender);
//                    e.printStackTrace();
//                } catch (NoSuchAlgorithmException e) {
//                    throw new RuntimeException(e);
//                } catch (JSONException e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    // Shutdown the executor service
//                    executorService.shutdown();
//                }
//            });

        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(100);
    }
}


