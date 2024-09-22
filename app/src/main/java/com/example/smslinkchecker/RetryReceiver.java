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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RetryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String url = intent.getStringExtra("url");
        String sender = intent.getStringExtra("sender");
        String message = intent.getStringExtra("message");

        if (url != null) {
            // Re-attempt the scan when the retry action is triggered
            SmsListener smsListener = new SmsListener();

            // Scan Detected URL and Get Analysis Result using VirusTotal API
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                try {
                    // Method Call API return API URL
                    String apiUrl = smsListener.SnapshotmachineAPI(url);
                    // Asynchronously scan URL
                    String analysisId = smsListener.processUrls(context, url);

                    if (analysisId != null) {
                        // Asynchronously get analysis result
                        String analysisResultJSON = smsListener.getAnalysis(analysisId);

                        if (analysisResultJSON != null) {
                            // Download image and process the analysis result on a background thread
                            Bitmap bitmap;
                            InputStream in = new URL(apiUrl).openStream();
                            bitmap = BitmapFactory.decodeStream(in);
                            byte[] image = smsListener.getBitmapAsByteArray(bitmap);

                            // Notify the user with the analysis result on the main thread
                            new Handler(Looper.getMainLooper()).post(() -> {
                                String analysis = smsListener.NotifyResult(context, url, analysisResultJSON);

                                // Insert into database in SQLite
                                DBHelper dbHelper = new DBHelper(context);
                                dbHelper.insertData(url, sender , message, apiUrl, analysis, image, analysisResultJSON);
                            });
                        } else {
                            smsListener.showRetryNotification(context, url, message, sender);
                            Log.v("GetAnalysis", "Failed to get analysis result.");
                            System.out.println("Failed to get analysis result.");
                        }
                    } else {
                        smsListener.showRetryNotification(context, url, sender, message);
                        Log.v("ScanURL", "Failed to scan URL.");
                        System.out.println("Failed to scan URL.");
                    }
                } catch (IOException e) {
                    Log.v("SmsListener", "Error in VirusTotal: " + e.getMessage());
                    smsListener.showRetryNotification(context, url, sender, message);
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } finally {
                    // Shutdown the executor service
                    executorService.shutdown();
                }
            });
        }
        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(100);
    }
}
