package com.example.smslinkchecker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsMessage;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

// Regex
import androidx.core.app.NotificationCompat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// screenshotmachine API
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

// antoher
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class SmsListener extends BroadcastReceiver {
    private static final String CHANNEL_ID = "1001";
    private static final int NOTIFICATION_ID = 123;
    public static String url = "";
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
                        Pattern URL_PATTERN = Pattern.compile("\\b(?:https?://)?(?:www\\.)?([\\w-]+(?:\\.[\\w-]+)*)\\.[a-z]{2,}(?:/[\\w-]+(?:/[^ \\n]*+)*)?\\b");
                        Matcher URL_MATCHER = URL_PATTERN.matcher(msgBody);
                        while (URL_MATCHER.find()) {
                            url = URL_MATCHER.group();
                            Toast.makeText(context, "URL Detected: " + url, Toast.LENGTH_LONG).show();
                            Log.v("URL", url);
                            createNotification(context, "URL Detected in SMS message", url);

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
                            options.put("delay", "200");
                            options.put("zoom", "100");

                            String apiUrl = sm.generateScreenshotApiUrl(options);
                            Log.v("Screenshot API URL", apiUrl);
                            downloadScreenshot(apiUrl, context);

                            // Update ListView in MainActivity
                            String recent = "Sender: " + msg_from + ", Body: " + msgBody + ", URL: " + url;
                            MainActivity mainActivity = MainActivity.getInstance();
                            if (mainActivity != null) {
                                mainActivity.addUrl(recent);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void downloadScreenshot(String apiUrl, Context context) {
        new Thread(() -> {
            try {
                URLConnection connection = new URL(apiUrl).openConnection();
                connection.addRequestProperty("User-Agent", "Mozilla/4.0");
                InputStream inputStream = connection.getInputStream();

                File screenshotFile = new File(context.getExternalFilesDir(null), "screenshot.png");
                FileOutputStream outputStream = new FileOutputStream(screenshotFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();
                Log.v("test", "Screenshot saved: " + screenshotFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("test", "Error downloading screenshot: " + e.getMessage());
            }
        }).start();
    }


    private void createNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create NotificationChannel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMS Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }


}
