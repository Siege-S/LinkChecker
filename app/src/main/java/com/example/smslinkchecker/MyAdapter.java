package com.example.smslinkchecker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private Context context;
    private ArrayList id, sender_id, url_id;
    private ArrayList<byte[]> imageURL_id;

    private final RecyclerViewInterface recyclerViewInterface;
    public MyAdapter(Context context, ArrayList id, ArrayList sender_id, ArrayList url_id, ArrayList<byte[]> imageURl_id, RecyclerViewInterface recyclerViewInterface) {
        this.context = context;
        this.id = id;
        this.sender_id = sender_id;
        this.url_id = url_id;
        this.imageURL_id = imageURl_id;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.userentry, parent, false);
        return new ViewHolder(v, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            holder.id.setText(String.valueOf(id.get(position)));
            holder.sender_id.setText(String.valueOf(sender_id.get(position)));
            holder.url_id.setText(String.valueOf(url_id.get(position)));

            // Convert byte array to bitmap and set it to the ImageView
            byte[] image = imageURL_id.get(position);
            if (image != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                holder.imageView.setImageBitmap(bitmap);
            } else {
                holder.imageView.setImageBitmap(null); // Or a placeholder image
            }
        } catch (Exception e) {
            Log.e("MyAdapter", "Error binding view holder", e);
        }
    }


    @Override
    public int getItemCount() {
        return id.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView id, sender_id, url_id;
        ImageView imageView;
        public ViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            id = itemView.findViewById(R.id.txtID);
            sender_id = itemView.findViewById(R.id.txtSender);
            url_id = itemView.findViewById(R.id.txtUrl);
            imageView = itemView.findViewById(R.id.IV_ImageUrl);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(recyclerViewInterface != null) {
                        int pos = getAdapterPosition();

                        if(pos != RecyclerView.NO_POSITION) {
                            recyclerViewInterface.onItemClick(pos);
                        }
                    }
                }
            });
        }
    }
}
