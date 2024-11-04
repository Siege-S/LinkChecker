package com.example.smslinkchecker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DetailMessageFragment_new#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DetailMessageFragment_new extends Fragment {

    private static final String ARG_ID = "id";
    private static final String ARG_SENDER = "sender";
    private static final String ARG_URL = "url";
    private static final String ARG_JSON = "json";
    private static final String ARG_IMAGE = "image";

    private String mId;
    private String mSender;
    private String mURL;
    private String mJSON;
    private byte[] mImage;

    ImageView imageView;
    ScaleGestureDetector scaleGestureDetector;
    float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 1.0f; // Minimum zoom level
    private static final float MAX_ZOOM = 5.0f; // Maximum zoom level

    //JSON Results
    private RecyclerView recyclerView;
    private List<ScanResult> scanResults;
    private ScanResultAdapter adapter;

    public DetailMessageFragment_new() {
        // Required empty public constructor
    }

    public static DetailMessageFragment_new newInstance(String id, String sender, String url, String json, byte[] image) {
        DetailMessageFragment_new fragment = new DetailMessageFragment_new();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putString(ARG_SENDER, sender);
        args.putString(ARG_URL, url);
        args.putString(ARG_JSON, json);
        args.putByteArray(ARG_IMAGE, image);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mId = getArguments().getString(ARG_ID);
            mSender = getArguments().getString(ARG_SENDER);
            mURL = getArguments().getString(ARG_URL);
            mImage = getArguments().getByteArray(ARG_IMAGE);
            mJSON = getArguments().getString(ARG_JSON);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_detail_message_new, container, false);
        // Use mSender, mMessage, and mImage as needed
        TextView idTextView = view.findViewById(R.id.txtdetailID);
        TextView senderTextView = view.findViewById(R.id.txtdetailNumber);
        TextView urlTextView = view.findViewById(R.id.txtdetailURL);

        // Display
        imageView = view.findViewById(R.id.IV_detailImage);
        idTextView.setText(mId);
        senderTextView.setText("Sender: " + mSender);
        urlTextView.setText("URL Detected: \n" + mURL);

        if (mImage != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(mImage, 0, mImage.length);
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground); // Or handle the case where there's no image
        }

        // Initialize RecyclerView for JSON results
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize the list
        scanResults = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(mJSON);
            JSONObject results = jsonResponse.getJSONObject("data").getJSONObject("attributes").getJSONObject("results");
            Iterator<String> keys = results.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject engineResult = results.getJSONObject(key);
                ScanResult result = new ScanResult(
                        engineResult.getString("engine_name"),
                        engineResult.getString("method"),
                        engineResult.getString("category"),
                        engineResult.getString("result")
                );
                scanResults.add(result);
            }

            // Sort the list to move "malicious" to the top
            sortScanResults();

            // Setup the RecyclerView adapter with the processed results
            adapter = new ScanResultAdapter(scanResults);
            recyclerView.setAdapter(adapter);

        } catch (Exception e) {
            Log.e("JSONError", "Error processing JSON response: " + e.getMessage());
            e.printStackTrace(); // Handle exceptions properly in production code
        }
        //JSON parsing
        try {
            JSONObject jsonObject = new JSONObject(mJSON);
            JSONObject results = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("results");
//            jsonTextView.setText(results.toString(4));

            // Virustotal stats
            ProgressBar circularProgressBar = view.findViewById(R.id.circularProgressBar);
            TextView progressText = view.findViewById(R.id.progressText);
            TextView txtanalysis = view.findViewById(R.id.txtAnalysis);
            TextView txtAnalysis2 = view.findViewById(R.id.txtAnalysis2);
            ImageView IV_analysis = view.findViewById(R.id.IV_analysis);

            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("malicious");
            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");

            if(malicious > 0){
                txtanalysis.setText(malicious + " security vendors flagged this URL as malicious");
                txtAnalysis2.setText(malicious + " Threat found");
                txtanalysis.setTextColor(getResources().getColor(R.color.red));
                txtAnalysis2.setTextColor(getResources().getColor(R.color.red));
                progressText.setTextColor(getResources().getColor(R.color.red));
                IV_analysis.setImageResource(R.drawable.warning_red);

                // Update progress and text
                circularProgressBar.setProgress(malicious);
                progressText.setText(malicious + " / 96");
            } else if (suspicious > 0){
                txtanalysis.setText(suspicious + " security vendors flagged this URL as suspicious");
                txtAnalysis2.setText("URL is suspicious");
                txtanalysis.setTextColor(getResources().getColor(R.color.yellow));
                txtAnalysis2.setTextColor(getResources().getColor(R.color.yellow));
                progressText.setTextColor(getResources().getColor(R.color.yellow));
                IV_analysis.setImageResource(R.drawable.suspicious_icon);

                // Update progress and text
                circularProgressBar.setProgress(suspicious);
                progressText.setText(suspicious + " / 96");
            } else {
                txtanalysis.setText("No security vendors flagged this URL as malicious");
                txtAnalysis2.setText("URL is safe");
                txtanalysis.setTextColor(getResources().getColor(R.color.green));
                txtAnalysis2.setTextColor(getResources().getColor(R.color.green));
                progressText.setTextColor(getResources().getColor(R.color.green));
                IV_analysis.setImageResource(R.drawable.check_green);

                // Update progress and text
                circularProgressBar.setProgress(malicious);
                progressText.setText(malicious + " / 96");
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            }
        });

        // Buttons
        Button btndelete = view.findViewById(R.id.btndelete);
        Button btnBack = view.findViewById(R.id.btnBack);
        Button btnDeleteSMS = view.findViewById(R.id.btnDeleteSMS);

        btnDeleteSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // In your Activity or Fragment where you want to show the dialog
                String phoneNumber = mSender;
                ImageDialogFragment imageDialog = ImageDialogFragment.newInstance(phoneNumber);
                imageDialog.show(getChildFragmentManager(), "ImageDialog");
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to the previous fragment or activity
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                MessageFragment messageFragment = new MessageFragment(); // or MessageFragment.newInstance() if you have arguments

                fragmentTransaction.replace(R.id.frame_layout, messageFragment);
//                fragmentTransaction.addToBackStack(null); // Optional: add the transaction to the back stack so the user can navigate back
                fragmentTransaction.commit();
            }
        });
        btndelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Do you want to Delete Result?")
                        .setMessage("It is recommended to 'Move to Spam / Delete SMS' instead of deleting result")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Action for "OK" button click
                                //                System.out.println("Delete test");
                                int id = Integer.parseInt(mId);
                                DBHelper db = new DBHelper(getContext());
                                db.deleteRecord(id);

                                // Go to Message Fragment
                                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                                MessageFragment messageFragment = new MessageFragment(); // or MessageFragment.newInstance() if you have arguments

                                fragmentTransaction.replace(R.id.frame_layout, messageFragment);
//                fragmentTransaction.addToBackStack(null); // Optional: add the transaction to the back stack so the user can navigate back
                                fragmentTransaction.commit();
                                Toast.makeText(getContext(), "Record deleted", Toast.LENGTH_SHORT);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Action for "Cancel" button click
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        return view;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM)); // Set the min and max zoom levels
            imageView.setScaleX(scaleFactor);
            imageView.setScaleY(scaleFactor);
            return true;
        }
    }

    private void sortScanResults() {
        // Sort the list: "malicious" first, "suspicious" second, everything else afterward
        Collections.sort(scanResults, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult result1, ScanResult result2) {
                // Prioritize "malicious" first
                if (result1.getCategory().equalsIgnoreCase("malicious")) {
                    return -1; // Move result1 above result2
                } else if (result2.getCategory().equalsIgnoreCase("malicious")) {
                    return 1;  // Move result2 above result1
                }
                // Then "suspicious" second
                else if (result1.getCategory().equalsIgnoreCase("suspicious")) {
                    return -1; // Move result1 above result2
                } else if (result2.getCategory().equalsIgnoreCase("suspicious")) {
                    return 1;  // Move result2 above result1
                }
                // Then "harmless" second
                else if (result1.getCategory().equalsIgnoreCase("harmless")) {
                    return -1; // Move result1 above result2
                } else if (result2.getCategory().equalsIgnoreCase("harmless")) {
                    return 1;  // Move result2 above result1
                }
                // Keep everything else as is
                return 0;
            }
        });
    }

}
