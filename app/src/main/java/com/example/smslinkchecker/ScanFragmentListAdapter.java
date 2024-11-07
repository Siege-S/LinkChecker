package com.example.smslinkchecker;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ScanFragmentListAdapter extends RecyclerView.Adapter<ScanFragmentListAdapter.ViewHolder> {
    private ArrayList<String> detectedURL;

    public ScanFragmentListAdapter(ArrayList<String> detectedURL) {
        this.detectedURL = detectedURL;
    }

    // Update the data in the adapter
    public void updateData(ArrayList<String> newData) {
        detectedURL.clear();
        detectedURL.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScanFragmentListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.scanfragment_detected_url_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanFragmentListAdapter.ViewHolder holder, int position) {
        String url = detectedURL.get(position);
        holder.txtDetectedURL.setText(url);
    }

    @Override
    public int getItemCount() {
        return detectedURL.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDetectedURL;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDetectedURL = itemView.findViewById(R.id.txtDetectedURL);
        }
    }
}
