package com.example.smslinkchecker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> {
    private List<ScanResult> scanResults;

    public ScanResultAdapter(List<ScanResult> scanResults) {
        this.scanResults = scanResults;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.scan_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult result = scanResults.get(position);
        holder.tvEngineName.setText(result.getEngineName());
        holder.tvMethod.setText(result.getMethod());
        holder.tvCategory.setText(result.getCategory());
        holder.tvResult.setText(result.getResult());

        // Set the numbering
        holder.tvCount.setText(String.valueOf(position + 1)); // Numbering starts at 1
        // Set color based on the result
        if (result.getResult().equalsIgnoreCase("clean") || result.getResult().equalsIgnoreCase("harmless")) {
            holder.tvCategory.setTextColor(Color.GREEN);
            holder.tvResult.setTextColor(Color.GREEN);
        } else if (result.getResult().equalsIgnoreCase("unrated") || result.getResult().equalsIgnoreCase("undetected")) {
            holder.tvCategory.setTextColor(Color.GRAY);
            holder.tvResult.setTextColor(Color.GRAY);
        } else if (result.getResult().equalsIgnoreCase("malicious") || result.getResult().equalsIgnoreCase("phishing")) {
            holder.tvCategory.setTextColor(Color.RED);
            holder.tvResult.setTextColor(Color.RED);
        } else if (result.getResult().equalsIgnoreCase("suspicious")) {
            holder.tvCategory.setTextColor(Color.YELLOW);
            holder.tvResult.setTextColor(Color.YELLOW);
        } else {
            holder.tvResult.setTextColor(Color.WHITE); // Default color for other results
        }
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEngineName;
        TextView tvMethod;
        TextView tvCategory;
        TextView tvResult;
        TextView tvCount;

        public ViewHolder(View itemView) {
            super(itemView);
            tvEngineName = itemView.findViewById(R.id.tv_engine_name);
            tvMethod = itemView.findViewById(R.id.tv_method);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvResult = itemView.findViewById(R.id.tv_result);
            tvCount = itemView.findViewById(R.id.tv_count);
        }
    }
}

