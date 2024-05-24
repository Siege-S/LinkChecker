package com.example.smslinkchecker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Telephony;
import android.util.Log;


import androidx.annotation.Nullable;
public class MyForegroundService extends Service {
    public SmsListener smsListener;
    private boolean isRunning;
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Perform your foreground service logic here
        smsListener = new SmsListener();
        IntentFilter intentFilter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        registerReceiver(smsListener, intentFilter);

        isRunning = true;
        new Thread(
                () -> {
                    while (isRunning) {
                        Log.v("Foreground", "Foreground Service is running. . .");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        ).start();

        final String CHANNEL_ID = "Foreground Service ID";
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
        );

        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("LinkGuard Service")
                .setContentText("Running")
                .setSmallIcon(R.drawable.ic_launcher_foreground);

        startForeground(1001, notification.build());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        if (smsListener != null) {
            unregisterReceiver(smsListener);
            smsListener = null;
        }
        isRunning = false;
        stopForeground(true);
        stopSelf();
    }
    public void stopService() {
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
