package com.example.smslinkchecker;

import static com.example.smslinkchecker.MainActivity.REQUEST_SMS_PERMISSION;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.Toast;
import android.Manifest;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public Switch switchPermission;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String email = "linkguard0311@gmail.com";
    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchPermission = view.findViewById(R.id.switchPermission);

        // Check current permission status and update the switch state
        boolean hasSmsPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        switchPermission.setChecked(hasSmsPermission);

        switchPermission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_SMS_PERMISSION);
                    switchPermission.setChecked(hasSmsPermission);
                } else {
                    Toast.makeText(getContext(), "User must manually disable SMS permissions.", Toast.LENGTH_SHORT).show();
                    openAppSettings();
                    switchPermission.setChecked(hasSmsPermission);
                }
            }
        });
        // Send Feedback
        View layoutFeedback = view.findViewById(R.id.layoutFeedback);
        View layoutNotification = view.findViewById(R.id.layoutNotification);
        View layoutAutoStart = view.findViewById(R.id.layoutAutoStart);
        View layoutPermission = view.findViewById(R.id.layoutPermission);
        View layoutCredits = view.findViewById(R.id.layoutCredits);
        layoutFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Dialog Feedback
//                FeedbackDialogFragment feedbackDialog = new FeedbackDialogFragment();
//                feedbackDialog.show(getChildFragmentManager(), "feedbackDialog");
                feedback_dialog();
            }
        });
        layoutNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationSettings();
            }
        });
        layoutAutoStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View view = inflater.inflate(R.layout.dialog_autostart, null);
                Button btnGoToSettings = view.findViewById(R.id.btnGoToSettings);
                Button btnClose = view.findViewById(R.id.btnClose);

                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setView(view)
                        .create();

                alertDialog.show();
                btnGoToSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        autoStartSettings();
                    }
                });
                btnClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });
            }

        });
        layoutPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "User must manually disable SMS permissions.", Toast.LENGTH_SHORT).show();
                openAppSettings();
            }
        });
        layoutCredits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                credits_dialog();
            }
        });
    }

    public void credits_dialog(){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_about, null);

        Button btnClose = view.findViewById(R.id.btnGoToSettings);

        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    public void feedback_dialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_feedback, null);

        RatingBar rating_bar = view.findViewById(R.id.rating_bar);
        EditText feedback_input = view.findViewById(R.id.feedback_input);
        Button btnClose = view.findViewById(R.id.btnGoToSettings);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);

        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (feedback_input.getText().toString().isEmpty() || rating_bar.getRating() == 0) {
                    Toast.makeText(getContext(), "Please enter your feedback and rating.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Send feedback via Gmail", Toast.LENGTH_SHORT).show();
                    sendFeedbackViaEmail(feedback_input.getText().toString(), rating_bar.getRating());
                    alertDialog.dismiss();
                }
            }
        });
        alertDialog.show();
    }
    private void sendFeedbackViaEmail(String feedbackText, float ratingValue) {
        // Define the email subject and body
        String subject = "App Feedback";
        String body = "Feedback: " + feedbackText + "\nRating: " + ratingValue + " stars";

        // Create the email Intent
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        // Start the email intent
        try {
            startActivity(Intent.createChooser(emailIntent, "Send Feedback via:"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }
    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0 and above
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
        } else {
            // For versions below Android 8.0
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
        }
        startActivity(intent);
    }
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        // Exit the app after the settings screen opens
        requireActivity().finish();
    }

    private void autoStartSettings() {
        Intent intent = new Intent();
        String manufacturer = android.os.Build.MANUFACTURER;

        if ("xiaomi".equalsIgnoreCase(manufacturer)) {
            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        }  else if ("vivo".equalsIgnoreCase(manufacturer)) {
            intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
        } else if ("letv".equalsIgnoreCase(manufacturer)) {
            intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
        } else {
            // Fallback message for other manufacturers
            Toast.makeText(getContext(), "Settings>>Manage Apps>>Auto-Start>>LinkGuard", Toast.LENGTH_LONG).show();
        }

        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Show a fallback message or open general settings if the specific Intent fails
            startActivity(new Intent(Settings.ACTION_SETTINGS));
//            startActivity(new Intent(Settings.ACTION_SEARCH_SETTINGS));
//            startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        }
    }
}