package com.example.smslinkchecker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DetailMessageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DetailMessageFragment extends Fragment {

    private static final String ARG_SENDER = "sender";
    private static final String ARG_URL = "url";
    private static final String ARG_IMAGE = "image";

    private String mSender;
    private String mMessage;
    private byte[] mImage;

    ImageView imageView;
    ScaleGestureDetector scaleGestureDetector;
    float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 1.0f; // Minimum zoom level
    private static final float MAX_ZOOM = 5.0f; // Maximum zoom level

    public DetailMessageFragment() {
        // Required empty public constructor
    }

    public static DetailMessageFragment newInstance(String sender, String url, byte[] image) {
        DetailMessageFragment fragment = new DetailMessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SENDER, sender);
        args.putString(ARG_URL, url);
        args.putByteArray(ARG_IMAGE, image);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSender = getArguments().getString(ARG_SENDER);
            mMessage = getArguments().getString(ARG_URL);
            mImage = getArguments().getByteArray(ARG_IMAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_detail_message, container, false);

        // Use mSender, mMessage, and mImage as needed
        TextView senderTextView = view.findViewById(R.id.txtdetailNumber);
        TextView urlTextView = view.findViewById(R.id.txtdetailURL);
        imageView = view.findViewById(R.id.IV_detailImage);
        senderTextView.setText(mSender);
        urlTextView.setText(mMessage);
        if (mImage != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(mImage, 0, mImage.length);
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground); // Or handle the case where there's no image
        }

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
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
