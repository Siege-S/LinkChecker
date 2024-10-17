package com.example.smslinkchecker;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageDialogFragment extends DialogFragment {

    private static final String ARG_PHONE_NUMBER = "phoneNumber";
    private int[] images = { R.drawable.slider0,R.drawable.slider1, R.drawable.slider2, R.drawable.slider3, R.drawable.slider4};
    private String[] descriptions = {"Please follow the instructions for moving sms to spam or deleting sms.",
            "Click the '3 dots' on the top right corner to open menu.",
            "Click 'Delete' if you want to delete the sms or Click 'Block & report' spam to move to spam.",
            "Confirm Deletion.",
            "Confirm Blocking and Reporting sms."
    };
    private int currentIndex = 0;  // Index to keep track of current image
    private String phoneNumber; // To hold the passed phone number

    public static ImageDialogFragment newInstance(String phoneNumber) {
        ImageDialogFragment fragment = new ImageDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHONE_NUMBER, phoneNumber); // Put the phone number in the arguments
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Reset the current index to 0 when the dialog is opened
        currentIndex = 0;

        // Retrieve the phone number from arguments
        if (getArguments() != null) {
            phoneNumber = getArguments().getString(ARG_PHONE_NUMBER);
        }

        // Inflate the custom layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_slider, null);  // Reference your custom dialog layout

        // Find views in the dialog layout
        ImageView dialogImage = dialogView.findViewById(R.id.dialog_image);
        TextView descriptionText = dialogView.findViewById(R.id.image_description);
        Button nextButton = dialogView.findViewById(R.id.btn_next);
        Button previousButton = dialogView.findViewById(R.id.btn_previous);
        Button gotItButton = dialogView.findViewById(R.id.btn_got_it); // "Got it" button
        Button btnExit = dialogView.findViewById(R.id.btnExit); // "Exit" button

        // Set initial image and description when dialog opens
        dialogImage.setImageResource(images[currentIndex]);
        descriptionText.setText(descriptions[currentIndex]);

        // Initially hide "Previous" and "Got it" buttons
        previousButton.setVisibility(View.GONE);
        gotItButton.setVisibility(View.GONE);

        // Handle Next button click
        nextButton.setOnClickListener(v -> {
            if (currentIndex < images.length - 1) {
                currentIndex++;
                dialogImage.setImageResource(images[currentIndex]);
                descriptionText.setText(descriptions[currentIndex]);

                // Show "Previous" button when moving forward
                previousButton.setVisibility(View.VISIBLE);

                // If it's the last image, hide "Next" button and show "Got it" and "Send SMS" buttons
                if (currentIndex == images.length - 1) {
                    nextButton.setVisibility(View.GONE);
                    gotItButton.setVisibility(View.VISIBLE);
                }
            }
        });

        // Handle Previous button click
        previousButton.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                dialogImage.setImageResource(images[currentIndex]);
                descriptionText.setText(descriptions[currentIndex]);

                // Show "Next" button when moving back
                nextButton.setVisibility(View.VISIBLE);

                // Hide "Previous" button when at the first image
                if (currentIndex == 0) {
                    previousButton.setVisibility(View.GONE);
                }

                // Hide "Got it" and "Send SMS" buttons when moving back from the last image
                if (gotItButton.getVisibility() == View.VISIBLE) {
                    gotItButton.setVisibility(View.GONE);
                }
            }
        });

        // Handle Got it button click
        gotItButton.setOnClickListener(v -> {
            // Dismiss the dialog when "Got it" is clicked
            gotoSMS(phoneNumber);
            dismiss();
        });
        btnExit.setOnClickListener(v -> {
            // Dismiss the dialog when "Exit" is clicked
            dismiss();
        });

        // Create and return the Dialog
        Dialog dialog = new Dialog(requireContext(), getTheme()) {
            @Override
            public void onBackPressed() {
                // Handle back press if needed
                dismiss();
            }
        };
        dialog.setContentView(dialogView);  // Set the custom view to the dialog

        return dialog;  // Return the dialog
    }

    private void gotoSMS(String phoneNumber) {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + phoneNumber)); // Only SMS apps should handle this
        startActivity(smsIntent); // Start the SMS app
    }
}
