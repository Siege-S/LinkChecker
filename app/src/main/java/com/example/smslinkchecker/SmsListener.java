package com.example.smslinkchecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

// Regex
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsListener extends BroadcastReceiver {


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

                        // Check if sms message contains Links using regex
                        // 1st regex pattern (testing)
//                        Pattern URL_PATTERN = Pattern.compile("\\b(?:https?://)?(?:www\\.)?\\w+\\.\\w{2,}(?:/[-a-zA-Z0-9@:%_\\+.~#?&//=]*)?\\b");
                        // 2nd regex pattern (testing)
//                        Pattern URL_PATTERN = Pattern.compile("https?:\\/\\/(?:www\\.)?(?:\\w+-?)+\\.\\w+(?:\\/\\w+)*\n");
                        // 3rd regex pattern (testing -
                        Log.v("URLs", "From: "+ msg_from+" , Body: "+msgBody);
                        Pattern URL_PATTERN = Pattern.compile("\\b(?:https?://)?(?:www\\.)?([\\w-]+(?:\\.[\\w-]+)*)\\.[a-z]{2,}(?:/[\\w-]+(?:/[^ \\n]*+)*)?\\b");
                        Matcher URL_MATCHER = URL_PATTERN.matcher(msgBody);
                        while (URL_MATCHER.find()) {
                            String url = URL_MATCHER.group();
                            // Do something with the URL (e.g., Toast or Log)
                            Toast.makeText(context, "URL Detected: " + url, Toast.LENGTH_LONG).show();
                            Log.v("URL", url);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
