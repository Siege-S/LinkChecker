package com.example.smslinkchecker;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.Manifest;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScanFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScanFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    ArrayList<String> detectedURL;
    ArrayList<String> newDetectedURL;
    public ScanFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ScanFragment.
     */
    // TODO: Rename and change types and number of parameters
    private static final int SMS_PERMISSION_CODE = 1000;
    public static ScanFragment newInstance(String param1, String param2) {
        ScanFragment fragment = new ScanFragment();
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
        // Initialize any non-view components here
        detectedURL = new ArrayList<>();
        newDetectedURL = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
        // Inflate the layout for this fragment
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize any view components here

        // Initialize the RecyclerView
        RecyclerView rvDetectedURL = view.findViewById(R.id.rvDetectedURL);
        ScanFragmentListAdapter adapter = new ScanFragmentListAdapter(newDetectedURL);
        rvDetectedURL.setAdapter(adapter);
        rvDetectedURL.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the button
        Button btnScanSMS = view.findViewById(R.id.btnScanSMS);
        btnScanSMS.setOnClickListener(v -> {
            if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {

                Uri uri = Uri.parse("content://sms/inbox");
                Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {

                        int idIndex = cursor.getColumnIndex("_id");
                        int bodyIndex = cursor.getColumnIndex("body");

                        if (idIndex != -1 && bodyIndex != -1) {
                            String id = cursor.getString(idIndex);
                            String body = cursor.getString(bodyIndex);
                            System.out.println("ID: " + id + ", Body: " + body);

                            // Regular expression to match URLs
                            Pattern urlPattern = Patterns.WEB_URL;
                            Matcher matcher = urlPattern.matcher(body);
                            while(matcher.find()){
                                detectedURL.add(matcher.group());
                            }

                        }
                    }
                    System.out.println("Detected URL on SMS: " + detectedURL);
                    System.out.println("Detected URL size: " + detectedURL.size()); // Add Details on detected URL size

                    ArrayList<String> scannedURL = getScannedLinks();
                    newDetectedURL = new ArrayList<>(detectedURL);
                    newDetectedURL.removeAll(scannedURL);
                    System.out.println("Detected URL after removing scanned URL: " + newDetectedURL + ", Size: " + newDetectedURL.size());

                    // Update the adapter's data
                    adapter.updateData(newDetectedURL);
                    cursor.close();
                }

            } else {
                requestSMSPermissions();
            }
        });
    }

    private void requestSMSPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        }
    }

    private ArrayList<String> getScannedLinks() {
        DBHelper db = new DBHelper(getContext());
        Cursor cursor = db.scannedURLS();
        ArrayList<String> urls = new ArrayList<>();
        if(cursor != null) {
            while(cursor.moveToNext()) {
                String url = cursor.getString(1); // DBHelper column 1 is the URL
                urls.add(url);
            }
            System.out.println("Scanned URLs: " + urls);
            cursor.close();
        } else {
            System.out.println("(getScannedLinks) Cursor is null");
            cursor.close();
        }
        return urls;
    }
}