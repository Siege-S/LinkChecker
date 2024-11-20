package com.example.smslinkchecker;

import static android.R.layout.simple_spinner_item;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RenderEffect;
import android.graphics.Shader;
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
import java.util.concurrent.atomic.AtomicInteger;

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
        String date = Date.get(position);
        byte[] imageBytes = imageURL.get(position);  // This is a byte array

        DetailMessageFragment_new fragment = DetailMessageFragment_new.newInstance(id, sender, url, jsonResponse, date, imageBytes);
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
    ArrayList<String> ID, Sender, URL, JSONResponse, Date, Analysis;
    ArrayList<byte[]> imageURL;
    DBHelper DB;
    MyAdapter adapter;

    Spinner spin_Date;
    Spinner spin_Result;
    View layoutSpinnerButton;
    View layoutOfflineProcess;
    View layoutProcessText;
    View layoutSortFilter;
    Button btnScanOffline;
    Button btnRemoveItem;
    Spinner spin_url;
    ImageView internet;
    TextView txtInternet;
    TextView txtDetectedURL;
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
        Analysis = new ArrayList<>();
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
        adapter = new MyAdapter(getContext(), ID, Sender, URL,JSONResponse, imageURL, Date, Analysis, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Check internet connection
        internet = view.findViewById(R.id.IV_Internet);
        txtInternet = view.findViewById(R.id.txtInternet);
        layoutOfflineProcess = view.findViewById(R.id.layoutOfflineProcess);
        layoutProcessText = view.findViewById(R.id.layoutProcessText);
        layoutSpinnerButton = view.findViewById(R.id.layoutSpinnerButton);
        layoutSortFilter = view.findViewById(R.id.layoutSortFilter);
        txtDetectedURL = view.findViewById(R.id.txtDetectedURL);
        layoutProcessText.setVisibility(View.GONE);
        int count = dbHelper.getOfflineDataCount();
        txtDetectedURL.setText("When Offline LinkGuard Detected "+ count + " URL");
        if(count == 0){
            layoutOfflineProcess.setVisibility(View.GONE);
        } else {
            layoutOfflineProcess.setVisibility(View.VISIBLE);
        }
        if (isInternetConnected(getContext())) {
            System.out.println("Internet Connected");
            layoutSpinnerButton.setVisibility(View.VISIBLE);
            internet.setVisibility(View.GONE);
            txtInternet.setVisibility(View.GONE);
        } else {
            System.out.println("Internet Not Connected");
            layoutSpinnerButton.setVisibility(View.GONE);
            internet.setVisibility(View.VISIBLE);
            txtInternet.setVisibility(View.VISIBLE);
        }

        // Set up the swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });

        // Offline Feature
        spin_url = view.findViewById(R.id.spin_url);
        btnScanOffline = view.findViewById(R.id.btnScanOffline);
        btnRemoveItem = view.findViewById(R.id.btnRemoveItem);
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
                        ArrayAdapter<String> newAdapter = new ArrayAdapter<>(getContext(), simple_spinner_item, updatedUrls);
                        newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        int count = dbHelper.getOfflineDataCount();
                        txtDetectedURL.setText("When Offline LinkGuard Detected "+ count + " URL");
                        if(count == 0){
                            refreshData();
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
                // Disable while OfflineScanning is in progress
                btnScanOffline.setEnabled(false);
                btnRemoveItem.setEnabled(false);
                spin_Date.setEnabled(false);
                spin_Result.setEnabled(false);
                layoutProcessText.setVisibility(View.VISIBLE); // yung umiikot
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    recyclerView.setRenderEffect(RenderEffect.createBlurEffect(30, 30, Shader.TileMode.MIRROR));
                    layoutSortFilter.setRenderEffect(RenderEffect.createBlurEffect(10, 10, Shader.TileMode.MIRROR));
                }
                // true to disable touch events
                recyclerView.setOnTouchListener((view, event) -> true);
                Toast.makeText(getContext(), "Please wait while LinkGuard is processing the data", Toast.LENGTH_LONG).show();

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setBottomNavigationEnabled(false);
                }
                processOfflineData(context);
            }
        }); // Offline Feature

        // Sort & Filter Feature
        spin_Date = view.findViewById(R.id.spin_Date);
        spin_Result = view.findViewById(R.id.spin_Result);

        ArrayAdapter<CharSequence> adapterSpinDate = ArrayAdapter.createFromResource(getContext(), R.array.linkguard_Date, R.layout.spinner_item);
        adapterSpinDate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_Date.setAdapter(adapterSpinDate);

        ArrayAdapter<CharSequence> adapterSpinResult = ArrayAdapter.createFromResource(getContext(), R.array.linkguard_Result, R.layout.spinner_item);
        adapterSpinResult.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_Result.setAdapter(adapterSpinResult);

        spin_Date.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                filterData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                filterData();
            }
        });
        spin_Result.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterData();
            }
        });

    }
    private void filterData() {
        String selectedDateSortOrder = spin_Date.getSelectedItem().toString();  // "Recent", "Old", or "All"
        String selectedResultFilter = spin_Result.getSelectedItem().toString(); // Result type or "All"

        if(selectedResultFilter.equals("Malicious and Suspicious")) {
            selectedResultFilter = "3";
        } else if (selectedResultFilter.equals("Malicious")) {
            selectedResultFilter = "1";
        } else if (selectedResultFilter.equals("Suspicious")) {
            selectedResultFilter = "2";
        } else if (selectedResultFilter.equals("Harmless")) {
            selectedResultFilter = "0";
        }
        // Fetch filtered and sorted data from the database
        Cursor cursor = DB.getFilteredData(selectedDateSortOrder, selectedResultFilter);
        updateRecyclerView(cursor);
    }

    private void updateRecyclerView(Cursor cursor) {
        if (getView() == null) return; // Check if the fragment's view is available

        TextView txtdata = getView().findViewById(R.id.txtdata);

        // Clear previous data to avoid duplication
        ID.clear();
        Sender.clear();
        URL.clear();
        JSONResponse.clear();
        imageURL.clear();
        Date.clear();
        Analysis.clear();

        if (cursor.getCount() == 0) {
            txtdata.setText("No Data Found");
        } else {
            txtdata.setText("");  // Clear any previous text
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ID.add(cursor.getString(0));           // Column 0: ID
                    Sender.add(cursor.getString(2));       // Column 2: Sender
                    URL.add(cursor.getString(1));          // Column 1: URL
                    JSONResponse.add(cursor.getString(6)); // Column 6: JSONResponse
                    imageURL.add(cursor.getBlob(5));       // Column 5: ImageURL
                    Date.add(cursor.getString(7));         // Column 7: Timestamp
                    Analysis.add(cursor.getString(4));     // Column 4: Analysis

                } while (cursor.moveToNext());
            }
        }
        cursor.close();
        // Notify the adapter that the data set has changed to refresh the RecyclerView
        adapter.notifyDataSetChanged();
    }

    private void processOfflineData(Context context) {
        DBHelper dbHelper = new DBHelper(context);
        Cursor cursor = dbHelper.getOfflineData();
        recyclerView.setOnTouchListener((view, event) -> true);
        if (cursor != null) {
            int totalUrls = cursor.getCount();  // Total number of URLs to process
            final AtomicInteger remainingUrls = new AtomicInteger(totalUrls);

            if (cursor.moveToFirst()) {
                int urlIndex = cursor.getColumnIndex("url");
                int senderIndex = cursor.getColumnIndex("sender");
                int idIndex = cursor.getColumnIndex("id");

                if (urlIndex != -1 && idIndex != -1) {
                    do {
                        SmsListener smsListener = new SmsListener();
                        String url = cursor.getString(urlIndex);
                        String sender = cursor.getString(senderIndex);
                        int id = cursor.getInt(idIndex);

                        ExecutorService executorService = Executors.newSingleThreadExecutor();
                        executorService.execute(() -> {
                            try {
                                String apiUrl = smsListener.SnapshotmachineAPI(url);
                                String analysisId = smsListener.processUrls(context, url);

                                if (analysisId != null) {
                                    String analysisResultJSON = smsListener.getAnalysis(analysisId);

                                    if (analysisResultJSON != null) {
                                        InputStream in = new URL(apiUrl).openStream();
                                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                                        byte[] image = smsListener.getBitmapAsByteArray(bitmap);

                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            String analysis = smsListener.NotifyResult(context, url, analysisResultJSON);
                                            dbHelper.insertData(url, sender, apiUrl, analysis, image, analysisResultJSON);
                                            dbHelper.deleteRecordById(id);

                                            // Decrement the remaining URLs count
                                            if (remainingUrls.decrementAndGet() == 0) {
                                                if (getActivity() instanceof MainActivity) {
                                                    ((MainActivity) getActivity()).setBottomNavigationEnabled(true);

                                                }
                                                // Enable after OfflineScan is completed
                                                btnScanOffline.setEnabled(true);
                                                btnRemoveItem.setEnabled(true);
                                                layoutProcessText.setVisibility(View.GONE); // yung umiikot
                                                layoutOfflineProcess.setVisibility(View.GONE);
                                                recyclerView.setOnTouchListener((view, event) -> false); // true to disable touch events
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    recyclerView.setRenderEffect(null);
                                                    layoutSortFilter.setRenderEffect(null);
                                                }
                                                refreshData(); // Call refreshData when all URLs are processed
                                            }
                                        });
                                    }
                                } else {
                                    // Failure case
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (getActivity() instanceof MainActivity) {
                                            ((MainActivity) getActivity()).setBottomNavigationEnabled(true);
                                        }

                                        if (remainingUrls.decrementAndGet() == 0) {
                                            // Enable after OfflineScan is completed
                                            btnScanOffline.setEnabled(true);
                                            btnRemoveItem.setEnabled(true);
                                            spin_Date.setEnabled(true);
                                            spin_Result.setEnabled(true);
                                            layoutProcessText.setVisibility(View.GONE); // yung umiikot
                                            layoutOfflineProcess.setVisibility(View.GONE);
                                            recyclerView.setOnTouchListener((view, event) -> false); // true to disable touch events
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                recyclerView.setRenderEffect(null);
                                                layoutSortFilter.setRenderEffect(null);
                                            }
                                            refreshData();
                                        }
                                    });
                                }
                            } catch (IOException | NoSuchAlgorithmException e) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (getActivity() instanceof MainActivity) {
                                        ((MainActivity) getActivity()).setBottomNavigationEnabled(true);
                                    }

                                    if (remainingUrls.decrementAndGet() == 0) {
                                        // Enable after OfflineScan is completed
                                        btnScanOffline.setEnabled(true);
                                        btnRemoveItem.setEnabled(true);
                                        spin_Date.setEnabled(true);
                                        spin_Result.setEnabled(true);
                                        layoutProcessText.setVisibility(View.GONE); // yung umiikot
                                        layoutOfflineProcess.setVisibility(View.GONE);
                                        recyclerView.setOnTouchListener((view, event) -> false); // true to disable touch events
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            recyclerView.setRenderEffect(null);
                                            layoutSortFilter.setRenderEffect(null);
                                        }
                                        refreshData();
                                    }
                                });
                                Log.e("SmsListener", "Error: " + e.getMessage());
                            } catch (JSONException e) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (getActivity() instanceof MainActivity) {
                                        ((MainActivity) getActivity()).setBottomNavigationEnabled(true);
                                    }

                                    if (remainingUrls.decrementAndGet() == 0) {
                                        // Enable after OfflineScan is completed
                                        btnScanOffline.setEnabled(true);
                                        btnRemoveItem.setEnabled(true);
                                        spin_Date.setEnabled(true);
                                        spin_Result.setEnabled(true);
                                        layoutProcessText.setVisibility(View.GONE); // yung umiikot
                                        layoutOfflineProcess.setVisibility(View.GONE);
                                        recyclerView.setOnTouchListener((view, event) -> false); // true to disable touch events
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            recyclerView.setRenderEffect(null);
                                            layoutSortFilter.setRenderEffect(null);
                                        }
                                        refreshData();
                                    }
                                });
                                throw new RuntimeException(e);
                            } finally {
                                executorService.shutdown();
                            }
                        });
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setBottomNavigationEnabled(true);
                }
                // Enable after OfflineScan is completed
                btnScanOffline.setEnabled(true);
                btnRemoveItem.setEnabled(true);
                spin_Date.setEnabled(true);
                spin_Result.setEnabled(true);
                layoutProcessText.setVisibility(View.GONE); // yung umiikot
                layoutOfflineProcess.setVisibility(View.GONE);
                recyclerView.setOnTouchListener((view, event) -> false); // true to disable touch events
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    recyclerView.setRenderEffect(null);
                    layoutSortFilter.setRenderEffect(null);
                }
                refreshData(); // Call refreshData when all URLs are processed
            });
            Log.e("Error", "Cursor is null or empty");
        }
    }


    private void refreshData() {
//        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
//        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//
//        MessageFragment messageFragment = new MessageFragment(); // or MessageFragment.newInstance() if you have arguments
//
//        fragmentTransaction.replace(R.id.frame_layout, messageFragment);
//        // fragmentTransaction.addToBackStack(null); // Optional: add the transaction to the back stack so the user can navigate back
//        fragmentTransaction.commit();

        // Check Offline Detected URL
        int count = DB.getOfflineDataCount();
        txtDetectedURL.setText("When Offline LinkGuard Detected "+ count + " URL");
        if(count == 0){
            layoutOfflineProcess.setVisibility(View.GONE);
        } else {
            layoutOfflineProcess.setVisibility(View.VISIBLE);
        }
        // Fetch URLs from SQLite
        ArrayList<String> offlineURL = DB.getOfflineUrls();
        // Create an ArrayAdapter with the retrieved URLs
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, offlineURL);
        // Specify the layout for dropdown choices
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set the adapter to the spinner
        spin_url.setAdapter(adapter);
        // Check Internet
        if (isInternetConnected(getContext())) {
            System.out.println("Internet Connected");
            layoutSpinnerButton.setVisibility(View.VISIBLE);
            internet.setVisibility(View.GONE);
            txtInternet.setVisibility(View.GONE);
        } else {
            System.out.println("Internet Not Connected");
            layoutSpinnerButton.setVisibility(View.GONE);
            layoutProcessText.setVisibility(View.GONE);
            internet.setVisibility(View.VISIBLE);
            txtInternet.setVisibility(View.VISIBLE);
        }
        // Check Data
        String selectedDateSortOrder = spin_Date.getSelectedItem().toString();  // "Recent", "Old", or "All"
        String selectedResultFilter = spin_Result.getSelectedItem().toString(); // Result type or "All"
        if(selectedResultFilter.equals("Malicious and Suspicious")) {
            selectedResultFilter = "3";
        } else if (selectedResultFilter.equals("Malicious")) {
            selectedResultFilter = "1";
        } else if (selectedResultFilter.equals("Suspicious")) {
            selectedResultFilter = "2";
        } else if (selectedResultFilter.equals("Harmless")) {
            selectedResultFilter = "0";
        }
        Cursor cursor = DB.getFilteredData(selectedDateSortOrder, selectedResultFilter);
        updateRecyclerView(cursor);
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

}
