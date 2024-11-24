package com.example.smslinkchecker;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import java.sql.SQLOutput;

public class SaveForLaterService extends IntentService {
    public SaveForLaterService() {
        super("SaveForLaterService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String url = intent.getStringExtra("url");
        String sender = intent.getStringExtra("sender");

        // Save to database
        DBHelper dbHelper = new DBHelper(this);
        if(dbHelper.insertOfflineTbl(url, sender)){
            System.out.println("Save for Later Analysis");
        } else {
            System.out.println("Error Saving");
        }

        // Dismiss the notification after saving
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(69); // Cancel the notification with ID 100

    }
}
