package com.example.smslinkchecker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DetailMessageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DetailMessageFragment extends Fragment {

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

    public DetailMessageFragment() {
        // Required empty public constructor
    }

    public static DetailMessageFragment newInstance(String id, String sender, String url,String json, byte[] image) {
        DetailMessageFragment fragment = new DetailMessageFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_detail_message, container, false);

        // Use mSender, mMessage, and mImage as needed
        TextView idTextView = view.findViewById(R.id.txtdetailID);
        TextView senderTextView = view.findViewById(R.id.txtdetailNumber);
        TextView urlTextView = view.findViewById(R.id.txtdetailURL);
        TextView jsonTextView = view.findViewById(R.id.txtJSON);

        // Display
        imageView = view.findViewById(R.id.IV_detailImage);
        idTextView.setText(mId);
        senderTextView.setText("Sender: " + mSender);
        urlTextView.setText("URL Detected: " + mURL);

        if (mImage != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(mImage, 0, mImage.length);
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground); // Or handle the case where there's no image
        }

        //JSON parsing
        try {
            JSONObject jsonObject = new JSONObject(mJSON);
            JSONObject results = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("results");
            jsonTextView.setText(results.toString(4));

            // Virustotal stats
            TextView txtmalicious = view.findViewById(R.id.txtJSONMalicious);
            TextView txtsuspicious = view.findViewById(R.id.txtJSONSuspicious);
            TextView txtundetected = view.findViewById(R.id.txtJSONUndetected);
            TextView txtharmless = view.findViewById(R.id.txtJSONHarmless);
            TextView txttimeout = view.findViewById(R.id.txtJSONTimeout);
            TextView txtstatus = view.findViewById(R.id.txtJSONStatus);
            TextView txtanalysis = view.findViewById(R.id.txtAnalysis);

            ImageView IV_analysis = view.findViewById(R.id.IV_analysis);

            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("malicious");
            txtmalicious.setText(Integer.toString(malicious));

            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");
            txtsuspicious.setText(Integer.toString(suspicious));

            int undetected = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("undetected");
            txtundetected.setText(Integer.toString(undetected));

            int timeout = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("timeout");
            txttimeout.setText(Integer.toString(timeout));

            int harmless = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("harmless");
            txtharmless.setText(Integer.toString(harmless));

            String status = jsonObject.getJSONObject("data").getJSONObject("attributes").getString("status");
            txtstatus.setText(status);

            if(malicious > 0 && suspicious > 0){
                txtanalysis.setText("VirusTotal identified the URL as both malicious and suspicious");
                txtanalysis.setTextColor(getResources().getColor(R.color.red));
                IV_analysis.setImageResource(R.drawable.warning_red);

            } else if (suspicious > 0) {
                txtanalysis.setText("VirusTotal identified the URL as suspicious");
                txtanalysis.setTextColor(getResources().getColor(R.color.red));
                IV_analysis.setImageResource(R.drawable.warning_red);
            } else if (malicious > 0) {
                txtanalysis.setText("VirusTotal identified the URL as Malicious");
                txtanalysis.setTextColor(getResources().getColor(R.color.red));
                IV_analysis.setImageResource(R.drawable.warning_red);
            } else {
                txtanalysis.setText("VirusTotal report shows the URL is Harmless");
                txtanalysis.setTextColor(getResources().getColor(R.color.green));
                IV_analysis.setImageResource(R.drawable.check_green);
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

        Button btndelete = view.findViewById(R.id.btndelete);
        Button btnBack = view.findViewById(R.id.btnBack);

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



}
