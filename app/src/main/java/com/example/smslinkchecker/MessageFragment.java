package com.example.smslinkchecker;

import static android.R.layout.simple_spinner_item;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessageFragment extends Fragment implements RecyclerViewInterface {
    private static final String CHANNEL_ID = "1001";
    @Override
    public void onItemClick(int position) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Assuming imageURL.get(position) returns a URL or a Bitmap
        String id = ID.get(position);
        String sender = Sender.get(position);
        String url = URL.get(position);
        String jsonResponse = JSONResponse.get(position);
        byte[] imageBytes = imageURL.get(position);  // This is a byte array

        DetailMessageFragment fragment = DetailMessageFragment.newInstance(id, sender, url, jsonResponse, imageBytes);
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;
    ArrayList<String> ID, Sender, URL, JSONResponse, Date;
    ArrayList<byte[]> imageURL;
    DBHelper DB;
    MyAdapter adapter;

    public MessageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MessageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MessageFragment newInstance(String param1, String param2) {
        MessageFragment fragment = new MessageFragment();
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
        DB = new DBHelper(getContext());
        ID = new ArrayList<>();
        Sender = new ArrayList<>();
        URL = new ArrayList<>();
        imageURL = new ArrayList<>();
        JSONResponse = new ArrayList<>();
        Date = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_message, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DBHelper dbHelper = new DBHelper(getContext());
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.RV_Messages);
        adapter = new MyAdapter(getContext(), ID, Sender, URL,JSONResponse, imageURL, Date, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        displayData();




        // Check internet connection
        ImageView internet = view.findViewById(R.id.IV_Internet);
        TextView txtInternet = view.findViewById(R.id.txtInternet);
        View layoutOfflineProcess = view.findViewById(R.id.layoutOfflineProcess);
        View layoutSpinnerButton = view.findViewById(R.id.layoutSpinnerButton);
        TextView txtDetectedURL = view.findViewById(R.id.txtDetectedURL);
        TextView txtProcessText = view.findViewById(R.id.txtProcessText);
        txtProcessText.setVisibility(View.GONE);
        int count = dbHelper.getOfflineDataCount();
        txtDetectedURL.setText("When Offline LinkGuard Detected "+ count + " URL");
        if(count == 0){
            layoutSpinnerButton.setVisibility(View.GONE);
        } else {
            layoutSpinnerButton.setVisibility(View.VISIBLE);
        }
        if (isInternetConnected(getContext())) {
            System.out.println("Internet Connected");
            internet.setVisibility(View.GONE);
            txtInternet.setVisibility(View.GONE);
            layoutOfflineProcess.setVisibility(View.VISIBLE);
        } else {
            System.out.println("Internet Not Connected");
            internet.setVisibility(View.VISIBLE);
            txtInternet.setVisibility(View.VISIBLE);
            layoutOfflineProcess.setVisibility(View.GONE);
        }

        // Set up the swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });

        // Offline Feature
        Spinner spin_url = view.findViewById(R.id.spin_url);
        Button btnScanOffline = view.findViewById(R.id.btnScanOffline);
        Button btnRemoveItem = view.findViewById(R.id.btnRemoveItem);
        // Fetch URLs from SQLite
        ArrayList<String> offlineURL = dbHelper.getOfflineUrls();
        // Create an ArrayAdapter with the retrieved URLs
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, offlineURL);
        // Specify the layout for dropdown choices
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set the adapter to the spinner
        spin_url.setAdapter(adapter);

        spin_url.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the selected item
                String selectedItem = parentView.getItemAtPosition(position).toString();
                btnRemoveItem.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Delete the selected item from the database
                        dbHelper.deleteRecordByURL(selectedItem);

                        // Reload the Spinner data after deletion
                        List<String> updatedUrls = dbHelper.getOfflineUrls(); // Implement this to fetch URLs again
                        ArrayAdapter<String> newAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, updatedUrls);
                        newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        int count = dbHelper.getOfflineDataCount();
                        txtDetectedURL.setText("When Offline LinkGuard Detected "+ count + " URL");
                        if(count == 0){
                            layoutSpinnerButton.setVisibility(View.GONE);
                        } else {
                            layoutSpinnerButton.setVisibility(View.VISIBLE);
                        }

                        // Set the updated adapter to the Spinner
                        spin_url.setAdapter(newAdapter);

                        Toast.makeText(getContext(), selectedItem + " Deleted", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Handle if no item is selected
            }
        });

        btnScanOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                txtProcessText.setVisibility(View.VISIBLE);
                processOfflineData(context);
                Toast.makeText(getContext(), "Please wait while LinkGuard is processing the data", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processOfflineData(Context context) {
        DBHelper dbHelper = new DBHelper(getContext());
        Cursor cursor = dbHelper.getOfflineData(); // Retrieve the cursor from your database helper
        if (cursor != null && cursor.moveToFirst()) {
            int urlIndex = cursor.getColumnIndex("url");
            int senderIndex = cursor.getColumnIndex("sender");
            int messageIndex = cursor.getColumnIndex("message");
            int idIndex = cursor.getColumnIndex("id"); // Assuming you have an 'id' column for unique identification

            if (urlIndex != -1 && idIndex != -1) {
                do {
                    SmsListener smsListener = new SmsListener();

                    String url = cursor.getString(urlIndex);
                    String sender = cursor.getString(senderIndex);
                    String message = cursor.getString(messageIndex);
                    int id = cursor.getInt(idIndex); // Get the unique ID of the record

                    // Process the data asynchronously
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(() -> {
                        try {
                            String apiUrl = smsListener.SnapshotmachineAPI(url);
                            String analysisId = smsListener.scanURL(getContext(), url);

                            if (analysisId != null) {
                                String analysisResultJSON = smsListener.getAnalysis(analysisId);

                                if (analysisResultJSON != null) {
                                    InputStream in = new URL(apiUrl).openStream();
                                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                                    byte[] image = smsListener.getBitmapAsByteArray(bitmap);

                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        String analysis = NotifyResultURL(context, url, analysisResultJSON);
                                        dbHelper.insertData(url, sender ,message, apiUrl, analysis, image, analysisResultJSON);

                                        // After processing, delete the record by its ID
                                        dbHelper.deleteRecordById(id);
                                    });
                                }
                            }
                        } catch (IOException | NoSuchAlgorithmException e) {
                            Log.e("SmsListener", "Error: " + e.getMessage());
                        } finally {
                            executorService.shutdown();
                        }
                    });
                } while (cursor.moveToNext());
            }

            cursor.close(); // Close the cursor when done
        } else {
            Log.e("Error", "Cursor is null or empty");
        }
    }


    private void displayData() {
        Cursor cursor = DB.getdata();
        TextView txtdata = getView().findViewById(R.id.txtdata);

        // Clear previous data to prevent duplication
        ID.clear();
        Sender.clear();
        URL.clear();
        JSONResponse.clear();
        imageURL.clear();
        Date.clear();

        if (cursor.getCount() == 0) {
            txtdata.setText("No Data Found");
        } else {
            txtdata.setText("");  // Clear any previous text
            while (cursor.moveToNext()) { // Based on your Database column!!!
                ID.add(cursor.getString(0));           // Column 0: ID
                Sender.add(cursor.getString(2));       // Column 2: Sender
                URL.add(cursor.getString(1));          // Column 1: URL
                JSONResponse.add(cursor.getString(7)); // Column 7: JSONResponse
                imageURL.add(cursor.getBlob(6));       // Column 6: ImageURL
                Date.add(cursor.getString(8));         // Column 8: Date
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void refreshData() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        MessageFragment messageFragment = new MessageFragment(); // or MessageFragment.newInstance() if you have arguments

        fragmentTransaction.replace(R.id.frame_layout, messageFragment);
        // fragmentTransaction.addToBackStack(null); // Optional: add the transaction to the back stack so the user can navigate back
        fragmentTransaction.commit();

        // Stop the refreshing animation
        swipeRefreshLayout.setRefreshing(false);
    }

    public static boolean isInternetConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }

        return false;
    }

    private String NotifyResultURL(Context context, String url, String JSON) {
        if (context == null) {
            Log.e("NotifyResultURL", "Context is null");
            return "Failed to send notification: context is null";
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e("NotifyResultURL", "NotificationManager is null");
            return "Failed to send notification: NotificationManager is null";
        }

        // Create a notification channel if targeting Android 8.0 or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Create notifications
        NotificationCompat.Builder builder1 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VirusTotal identified the URL as suspicious or a phishing threat.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VirusTotal identified the URL as Malicious.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationCompat.Builder builder3 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VirusTotal identified the URL as both malicious and a phishing threat.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationCompat.Builder builder4 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("VirusTotal report shows the URL is Harmless.")
                .setContentText("URL: " + url)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            JSONObject jsonObject = new JSONObject(JSON);

            int malicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("malicious");
            int suspicious = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("suspicious");
            int harmless = jsonObject.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats").getInt("harmless");

            if (malicious > 0 && suspicious > 0) {
                notificationManager.notify(3, builder3.build()); // Notification ID 3
                return "VirusTotal identified the URL as both malicious and a phishing threat";
            } else if (malicious > 0) {
                notificationManager.notify(2, builder2.build()); // Notification ID 2
                return "VirusTotal identified the URL as Malicious";
            } else if (suspicious > 0) {
                notificationManager.notify(1, builder1.build()); // Notification ID 1
                return "VirusTotal identified the URL as suspicious or a phishing threat";
            } else {
                notificationManager.notify(4, builder4.build()); // Notification ID 4
                return "VirusTotal report shows the URL is Harmless";
            }

        } catch (JSONException e) {
            Log.e("NotifyResultURL", "JSON parsing error", e);
            return "Failed to process JSON data";
        }
    }

}
