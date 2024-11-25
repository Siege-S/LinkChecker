package com.example.smslinkchecker;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smslinkchecker.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    public static final int REQUEST_SMS_PERMISSION = 1000;
     ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {// Handle home action
                replaceFragment(new HomeFragment());
            }
            else if (itemId == R.id.messages) {// Handle messages action
                replaceFragment(new MessageFragment());
            }
//            else if (itemId == R.id.contacts) {// Handle contacts action
//                replaceFragment(new GuideFragment());
//            }
            else if (itemId == R.id.settings) {// Handle settings action
                replaceFragment(new SettingsFragment());
            }
            else {
                return false;
            }
            return true;
        });

        // Report as Spam Guide receive from Retry Notification
        String sender = getIntent().getStringExtra("sender");
        int notificationID = getIntent().getIntExtra("notificationID", 0);
        int dismiss = getIntent().getIntExtra("dismiss", 0);

        if(dismiss != 0) {
            System.out.println("Received Intent from Result Notification: Check Results " + dismiss);
            // Dismiss the notification after going to sms sender
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(dismiss);
        }

        if(sender != null && notificationID != 0) {
            System.out.println("Received Intent from Result Notification: Report as Spam " + dismiss);
            // Open Guide to Report SMS as Spam
            showImageSliderDialog(sender, notificationID);
        }

        // Check if the code has already run (delete this after)
//        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
//        boolean hasRunBefore = prefs.getBoolean("hasInsertedData", false);
//
//        if (!hasRunBefore) {
//            // Run the code you want to execute only once
//            DBHelper db = new DBHelper(this);
//            db.insertOfflineTbl("ogpe.cc", "sieger");
//            db.insertOfflineTbl("google.com", "sieger");
//
//            // Mark that the code has run
//            SharedPreferences.Editor editor = prefs.edit();
//            editor.putBoolean("hasInsertedData", true);
//            editor.apply();
//        }

        // Allow content behind the notch
        // Android 9.0 (API level 28) Above Only!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }

        // SYSTEM_UI_FLAG_FULLSCREEN // Hide the status bar
        // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Let the layout expand into status bar
        // SYSTEM_UI_FLAG_LAYOUT_STABLE // avoid abrupt layout changes during toggling of status and navigation bars
        // SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hide the navigation bar
        // SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION; // Let the layout expand into navigation bar
        // SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Keeps the app in immersive mode by preventing accidental gestures from immediately revealing the system bars.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Request Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_SMS_PERMISSION);
        }

    } // onCreate

    int imageCurrentIndex = 0;
    private void showImageSliderDialog(final String sender, final int notificationID) {

        // Inflate the custom dialog view
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_image_slider, null);

        // Initialize views
        Button btnExit = view.findViewById(R.id.btnExit);
        Button btn_previous = view.findViewById(R.id.btn_previous);
        Button btn_next = view.findViewById(R.id.btn_next);
        Button btn_got_it = view.findViewById(R.id.btn_got_it);
        TextView image_description = view.findViewById(R.id.image_description);
        ImageView dialog_image = view.findViewById(R.id.dialog_image);

        imageCurrentIndex = 0;
        // Set image slider logic
        final int[] Images = {R.drawable.slider0, R.drawable.slider1, R.drawable.slider2, R.drawable.slider3, R.drawable.slider4};
        final String[] descriptions = {
                "Please follow the instructions for moving SMS to spam or deleting SMS.",
                "Click the '3 dots' on the top right corner to open the menu.",
                "Click 'Delete' if you want to delete the SMS or click 'Block & report' spam to move it to spam.",
                "Confirm Deletion.",
                "Confirm Blocking and Reporting SMS."
        };

        dialog_image.setImageResource(Images[imageCurrentIndex]);
        image_description.setText(descriptions[imageCurrentIndex]);
        btn_previous.setVisibility(View.GONE);
        btn_got_it.setVisibility(View.GONE);

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        // Button logic
        btnExit.setOnClickListener(v -> alertDialog.dismiss());

        btn_previous.setOnClickListener(v -> {
            if (imageCurrentIndex > 0) {
                imageCurrentIndex--;
                dialog_image.setImageResource(Images[imageCurrentIndex]);
                image_description.setText(descriptions[imageCurrentIndex]);
                btn_next.setVisibility(View.VISIBLE);
                if (imageCurrentIndex == 0) btn_previous.setVisibility(View.GONE);
                btn_got_it.setVisibility(View.GONE);
            }
        });

        btn_next.setOnClickListener(v -> {
            if (imageCurrentIndex < Images.length - 1) {
                imageCurrentIndex++;
                dialog_image.setImageResource(Images[imageCurrentIndex]);
                image_description.setText(descriptions[imageCurrentIndex]);
                btn_previous.setVisibility(View.VISIBLE);
                if (imageCurrentIndex == Images.length - 1) {
                    btn_next.setVisibility(View.GONE);
                    btn_got_it.setVisibility(View.VISIBLE);
                }
            }
        });

        btn_got_it.setOnClickListener(v -> {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + sender)); // go to sms sender
            smsIntent.putExtra("sms_body", "Please follow the instructions for moving sms to spam or deleting sms.");
            startActivity(smsIntent);

            alertDialog.dismiss();
        });

        alertDialog.show();

        // Dismiss the notification after going to sms sender
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationID);
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)) {
                    // Show an explanation to the user and re-request the permission
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("This app requires SMS permission to function properly. Please grant the permission.")
                            .setPositiveButton("Grant", (dialog, which) -> {
                                // Re-request the permission
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_SMS_PERMISSION);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // User chose not to grant permission
                                Toast.makeText(this, "Permission to read SMS was denied. The app cannot function without it.", Toast.LENGTH_LONG).show();
                                finish(); // Close the app
                            })
                            .show();
                } else {
                    // User checked "Don't ask again" or denied multiple times
                    Toast.makeText(this, "Permission to read SMS was denied permanently. Please enable it in app settings.", Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Permission >> Allow SMS Permission", Toast.LENGTH_LONG).show();
                    // Optionally, redirect the user to app settings
                    openAppSettings();
                }
            } else {
                // Permission granted, read SMS inbox
                Toast.makeText(this, "Permission to read SMS granted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        // Exit the app
        finish();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
//            // Check if notification permission is granted after returning from settings
//            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
//                // Notification permission granted, proceed with your notification logic
//            }
//        }
//    }
    public void setBottomNavigationEnabled(boolean enabled) {
        for (int i = 0; i < binding.bottomNavigationView.getMenu().size(); i++) {
            binding.bottomNavigationView.getMenu().getItem(i).setEnabled(enabled);
        }
    }

}


