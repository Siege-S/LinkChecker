//package com.example.smslinkchecker;
//
//import android.app.AlertDialog;
//import android.app.NotificationManager;
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class ReportGuide extends AppCompatActivity {
//    int imageCurrentIndex = 0;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//
//        String sender = getIntent().getStringExtra("sender");
//        int notificationID = getIntent().getIntExtra("notificationID", 0);
//
//        // Inflate the custom dialog view
//        LayoutInflater inflater = LayoutInflater.from(this);
//        View view = inflater.inflate(R.layout.dialog_image_slider, null);
//
//        // Initialize views
//        Button btnExit = view.findViewById(R.id.btnExit);
//        Button btn_previous = view.findViewById(R.id.btn_previous);
//        Button btn_next = view.findViewById(R.id.btn_next);
//        Button btn_got_it = view.findViewById(R.id.btn_got_it);
//        TextView image_description = view.findViewById(R.id.image_description);
//        ImageView dialog_image = view.findViewById(R.id.dialog_image);
//
//        // Set image slider logic
////        int imageCurrentIndex = 0;
//        int[] Images = {R.drawable.slider0, R.drawable.slider1, R.drawable.slider2, R.drawable.slider3, R.drawable.slider4};
//        String[] descriptions = {
//                "Please follow the instructions for moving SMS to spam or deleting SMS.",
//                "Click the '3 dots' on the top right corner to open the menu.",
//                "Click 'Delete' if you want to delete the SMS or click 'Block & report' spam to move it to spam.",
//                "Confirm Deletion.",
//                "Confirm Blocking and Reporting SMS."
//        };
//
//        dialog_image.setImageResource(Images[imageCurrentIndex]);
//        image_description.setText(descriptions[imageCurrentIndex]);
//        btn_previous.setVisibility(View.GONE);
//        btn_got_it.setVisibility(View.GONE);
//
//        AlertDialog alertDialog = new AlertDialog.Builder(this)
//                .setView(view)
//                .setCancelable(false)
//                .create();
//
//        // Button logic
//        btnExit.setOnClickListener(v -> alertDialog.dismiss());
//        btn_previous.setOnClickListener(v -> {
//            if (imageCurrentIndex > 0) {
//                imageCurrentIndex--;
//                dialog_image.setImageResource(Images[imageCurrentIndex]);
//                image_description.setText(descriptions[imageCurrentIndex]);
//                btn_next.setVisibility(View.VISIBLE);
//                if (imageCurrentIndex == 0) btn_previous.setVisibility(View.GONE);
//                btn_got_it.setVisibility(View.GONE);
//            }
//        });
//        btn_next.setOnClickListener(v -> {
//            if (imageCurrentIndex < Images.length - 1) {
//                imageCurrentIndex++;
//                dialog_image.setImageResource(Images[imageCurrentIndex]);
//                image_description.setText(descriptions[imageCurrentIndex]);
//                btn_previous.setVisibility(View.VISIBLE);
//                if (imageCurrentIndex == Images.length - 1) {
//                    btn_next.setVisibility(View.GONE);
//                    btn_got_it.setVisibility(View.VISIBLE);
//                }
//            }
//        });
//        btn_got_it.setOnClickListener(v -> {
//            if (sender != null) {
//                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
//                smsIntent.setData(Uri.parse("smsto:" + sender));
//                startActivity(smsIntent);
//            }
//            alertDialog.dismiss();
//
//            // Navigate to MainActivity
//            Intent mainActivityIntent = new Intent(ReportGuide.this, MainActivity.class);
//            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);  // Clear the activity stack and open MainActivity
//            startActivity(mainActivityIntent);
//            finish();  // Close the current activity (ReportGuide)
//        });
//        alertDialog.show();
//
//        // Dismiss the notification after going to sms sender
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.cancel(notificationID);
//    }
//
//}
