package com.example.smslinkchecker;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class FeedbackDialogFragment extends DialogFragment {

    private static final String email = "linkguard0311@gmail.com";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate the custom layout for feedback
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_feedback, null);

        // Get references to UI elements in the dialog
        EditText feedbackInput = view.findViewById(R.id.feedback_input);
        RatingBar ratingBar = view.findViewById(R.id.rating_bar);

        // Set the custom view in the dialog
        builder.setView(view)
                .setPositiveButton("Send Feedback", null)  // Set the listener to null, we'll override it later
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Dismiss the dialog
                        FeedbackDialogFragment.this.getDialog().cancel();
                    }
                });

        // Create the dialog
        AlertDialog dialog = builder.create();

        // Override the Submit button click listener after the dialog is created
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                submitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Capture feedback and rating
                        String feedbackText = feedbackInput.getText().toString();
                        float ratingValue = ratingBar.getRating();

                        // Check if fields are empty and don't dismiss the dialog if they are
                        if (feedbackText.isEmpty() || ratingValue == 0) {
                            Toast.makeText(getActivity(), "Please Give Feedback and Rating", Toast.LENGTH_LONG).show();
                        } else {
                            // Send feedback via Gmail
                            sendFeedbackViaEmail(feedbackText, ratingValue);
                            dialog.dismiss();  // Manually dismiss the dialog when input is valid
                        }
                    }
                });
            }
        });

        return dialog;
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

}

