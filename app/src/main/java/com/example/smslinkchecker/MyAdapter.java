package com.example.smslinkchecker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private Context context;
    private ArrayList id, sender_id, url_id;

    public MyAdapter(Context context, ArrayList id, ArrayList sender_id, ArrayList url_id) {
        this.context = context;
        this.id = id;
        this.sender_id = sender_id;
        this.url_id = url_id;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.userentry, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.id.setText(String.valueOf(id.get(position)));
        holder.sender_id.setText(String.valueOf(sender_id.get(position)));
        holder.url_id.setText(String.valueOf(url_id.get(position)));
    }

    @Override
    public int getItemCount() {
        return id.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView id, sender_id, url_id;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.txtID);
            sender_id = itemView.findViewById(R.id.txtSender);
            url_id = itemView.findViewById(R.id.txtUrl);

        }
    }
}
