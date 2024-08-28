package com.example.smslinkchecker;

import android.content.Context;
import android.content.Intent;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessageFragment extends Fragment implements RecyclerViewInterface {
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
    ArrayList<String> ID, Sender, URL, JSONResponse;
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
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.RV_Messages);
        adapter = new MyAdapter(getContext(), ID, Sender, URL,JSONResponse, imageURL, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        displayData();

        ImageView internet = view.findViewById(R.id.IV_Internet);
        TextView txtInternet = view.findViewById(R.id.txtInternet);

        if (isInternetConnected(getContext())) {
            System.out.println("Internet Connected");
            internet.setVisibility(View.GONE);
            txtInternet.setVisibility(View.GONE);
        } else {
            System.out.println("Internet Not Connected");
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

    }

    private void displayData() {
        Cursor cursor = DB.getdata();
        TextView txtdata = getView().findViewById(R.id.txtdata);
        if (cursor.getCount() == 0) {
            txtdata.setText("No Data Found");
//            Toast.makeText(getContext(), "No Data Found", Toast.LENGTH_SHORT).show();
        } else {
            txtdata.setText("");
            while (cursor.moveToNext()) { // Based on your Database column!!!
                ID.add(cursor.getString(0));
                Sender.add(cursor.getString(2));
                URL.add(cursor.getString(1));
                JSONResponse.add(cursor.getString(7));
                imageURL.add(cursor.getBlob(6));
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

}
