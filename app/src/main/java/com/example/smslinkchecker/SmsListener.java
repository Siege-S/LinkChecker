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
import androidx.core.content.ContextCompat;

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
    // Newest
    @Override
    public void onReceive(Context context, Intent intent) {

        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())){

            DBHelper dbHelper = new DBHelper(context);

            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)){
                String sender = smsMessage.getOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();

                // Test
                System.out.println(sender + " : " + messageBody);

                // Regular Expression
                List<String> urls = extractUrlsFromMessage(messageBody, context);

                // Check Internet Connection
                if (!MessageFragment.isInternetConnected(context)) {
                    handleOfflineUrls(context, urls, sender, dbHelper);
                } else {
                    Intent serviceIntent = new Intent(context, SmsForeground.class);
                    serviceIntent.putExtra("sender", sender);
                    serviceIntent.putStringArrayListExtra("urls", (ArrayList<String>) urls);
                    System.out.println("smslistener" + urls);
                    if(!urls.isEmpty()){
                        System.out.println("url found in msg: " + messageBody);
                        System.out.println("Start Foreground Service. . .");
                        ContextCompat.startForegroundService(context, serviceIntent);
                    } else {
                        System.out.println("No url found in msg: " + messageBody);
                    }

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
                .setContentTitle("LinkGuard is Offline, saved for later analysis")
                .setContentText("Please avoid clicking this url '" + url + "', until connection is stored.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Show the notification
        notificationManager.notify(notificationID, builder.build());
    }

}
