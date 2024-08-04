package com.example.smslinkchecker;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smslinkchecker.databinding.ActivityMainBinding;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private Button buttonStartService;

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
            else if (itemId == R.id.history) {// Handle history action
                replaceFragment(new HistoryFragment());
            }
            else {
                return false;
            }
            return true;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.RECEIVE_SMS}, 1000);
        }

        checkNotificationPermission();

//        buttonStartService = findViewById(R.id.btnStartService);
//        Intent serviceIntent = new Intent(this, MyForegroundService.class);
//        if(!foregroundServiceRunning()) {
//            buttonStartService.setText("Start Service");
//        } else {
//            buttonStartService.setText("Stop Service");
//        }
//        // Button to start the service
//
//        buttonStartService.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                btnStartServiceOnClick(v);
//            }
//        });

    } // onCreate

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    //Start Service Method
//    public void btnStartServiceOnClick(View view) {
//        Log.v("btnService", "Button Service is CLicked.");
//        Intent serviceIntent = new Intent(this, MyForegroundService.class);
//        if(!foregroundServiceRunning()) {
//            buttonStartService.setText("Stop Service");
//            startForegroundService(serviceIntent);
//        } else {
//            buttonStartService.setText("Start Service");
//            stopService(serviceIntent);
//        }
//    }



    // Method to check if the foreground service is running
    public boolean foregroundServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed with the SMS-related tasks here
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly (e.g., show a message, disable SMS-related functionality)
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    private void checkNotificationPermission() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            // Notifications are not enabled
            // Prompt the user to grant notification permission
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enable Notifications");
            builder.setMessage("This app requires notification permission to function properly. Please enable it in settings.");
            builder.setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Open app settings
                    openAppSettings();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } else {
            // Notifications are enabled
            // Proceed with your notification logic
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_NOTIFICATION_PERMISSION);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            // Check if notification permission is granted after returning from settings
            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                // Notification permission granted, proceed with your notification logic
            }
        }
    }

}


