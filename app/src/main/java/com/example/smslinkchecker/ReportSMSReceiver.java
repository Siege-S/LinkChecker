//package com.example.smslinkchecker;
//
//import android.app.IntentService;
//import android.app.NotificationManager;
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//
//import androidx.annotation.Nullable;
//
//public class ReportSMSReceiver extends IntentService {
//    /**
//     * Creates an IntentService.  Invoked by your subclass's constructor.
//     *
//     * @param name Used to name the worker thread, important only for debugging.
//     */
//    public ReportSMSReceiver() {
//        super("ReportSMSReceiver");
//    }
//
//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
//        String sender = intent.getStringExtra("sender");
//        int notificationID = intent.getIntExtra("notificationID", 0);
//        if (sender != null) {
//            // Create an Intent to open the ReportGuide Activity
//            Intent dialogIntent = new Intent(this, ReportGuide.class);
//            dialogIntent.putExtra("sender", sender); // Pass data to the Activity
//            dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            // FLAG_ACTIVITY_CLEAR_TOP ensures only one instance of ReportGuide is open
//            startActivity(dialogIntent);
//        }
//        // Dismiss the notification after going to sms sender
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.cancel(notificationID);
//    }
//}
