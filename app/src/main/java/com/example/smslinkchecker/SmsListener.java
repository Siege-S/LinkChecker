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

public class SmsListener extends BroadcastReceiver {
    private static final String CHANNEL_ID = "1001";
    private static final int NOTIFICATION_ID = 123;
    public static String url;
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs;
            String msg_from;
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for(int i=0 ; i < msgs.length; i++){
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();

//                         Toast.makeText(context, "From: "+ msg_from+" , Body: "+msgBody, Toast.LENGTH_SHORT).show();

                        Log.v("URLs", "From: "+ msg_from+" , Body: "+msgBody);
                        Pattern URL_PATTERN = Pattern.compile("\\b(?:https?://)?(?:www\\.)?([\\w-]+(?:\\.[\\w-]+)*)\\.[a-z]{2,}(?:/[\\w-]+(?:/[^ \\n]*+)*)?\\b");
                        Matcher URL_MATCHER = URL_PATTERN.matcher(msgBody);
                        while (URL_MATCHER.find()) {
                            url = URL_MATCHER.group();
                            Toast.makeText(context, "URL Detected: " + url, Toast.LENGTH_LONG).show();
                            Log.v("URL", url);
                            createNotification(context, "URL Detected in sms message", url );
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
