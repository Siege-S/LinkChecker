package com.example.smslinkchecker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private Context context;
    private ArrayList id, sender_id, url_id, JSONResponse_id, date_id, analysis_id;
    private ArrayList<byte[]> imageURL_id;

    private final RecyclerViewInterface recyclerViewInterface;

    public MyAdapter(Context context, ArrayList id, ArrayList sender_id, ArrayList url_id, ArrayList JSONResponse_id, ArrayList<byte[]> imageURL_id,ArrayList date_id, ArrayList analysis_id , RecyclerViewInterface recyclerViewInterface) {
        this.context = context;
        this.id = id;
        this.sender_id = sender_id;
        this.url_id = url_id;
        this.JSONResponse_id = JSONResponse_id;
        this.imageURL_id = imageURL_id;
        this.date_id = date_id;
        this.analysis_id = analysis_id;
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
            holder.JSONResponse_id.setText(String.valueOf(JSONResponse_id.get(position)));
            holder.txtDate.setText(String.valueOf(date_id.get(position)));
//            holder.txtAnalsis.setText(String.valueOf(analysis_id.get(position)));

            // Convert byte array to bitmap and set it to the ImageView
            byte[] image = imageURL_id.get(position);
            if (image != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                holder.imageView.setImageBitmap(bitmap);
            } else {
                holder.imageView.setImageBitmap(null); // Or a placeholder image
            }

            String analysisResult = analysis_id.get(position).toString();
            // Set analysis text and color based on analysis
            if (analysisResult.equals("3")) {
                holder.txtAnalsis.setText("URL is Malicious and Suspicious");
                holder.txtAnalsis.setTextColor(ContextCompat.getColor(context, R.color.red));
                holder.IV_analysis.setImageResource(R.drawable.warning_red);
            } else if (analysisResult.equals("1")) {
                holder.txtAnalsis.setText("URL is Malicious");
                holder.txtAnalsis.setTextColor(ContextCompat.getColor(context, R.color.red));
                holder.IV_analysis.setImageResource(R.drawable.warning_red);
            } else if (analysisResult.equals("2")) {
                holder.txtAnalsis.setText("URL is Suspicious");
                holder.txtAnalsis.setTextColor(ContextCompat.getColor(context, R.color.red));
                holder.IV_analysis.setImageResource(R.drawable.warning_red);
            } else {
                holder.txtAnalsis.setText("URL is Harmless");
                holder.txtAnalsis.setTextColor(ContextCompat.getColor(context, R.color.green));
                holder.IV_analysis.setImageResource(R.drawable.check_green);
            }

            // Check for adult content and apply blur effect
            if (isAdultContent(url_id.get(position).toString())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  holder.imageView.setRenderEffect(RenderEffect.createBlurEffect(50, 50, Shader.TileMode.MIRROR));
                  holder.clickLayout.setVisibility(View.VISIBLE);
                }
            } else {
                // Ensure non-adult content doesn't retain blur or overlay
                holder.clickLayout.setVisibility(View.GONE); // Hide overlay
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    holder.imageView.setRenderEffect(null);
                }
            }

        } catch (Exception e) {
            Log.e("MyAdapter", "Error binding view holder", e);
        }

        // Set click listener to remove blur effect on click
        holder.clickLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.clickLayout.setVisibility(View.GONE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    holder.imageView.setRenderEffect(null);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return id.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView id, sender_id, url_id, JSONResponse_id, txtAnalsis, txtDate;
        ImageView imageView, IV_analysis;

        LinearLayout clickLayout;
        public ViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            // Added From Database
            id = itemView.findViewById(R.id.txtID);
            sender_id = itemView.findViewById(R.id.txtSender);
            url_id = itemView.findViewById(R.id.txtUrl);
            imageView = itemView.findViewById(R.id.IV_ImageUrl);
            JSONResponse_id = itemView.findViewById(R.id.txtJSON);
            txtDate = itemView.findViewById(R.id.txtDate);

            // Added for analysis
            txtAnalsis = itemView.findViewById(R.id.txtAnalysis);
            IV_analysis = itemView.findViewById(R.id.IV_analysis);

            // Added for click event
            clickLayout = itemView.findViewById(R.id.clickLayout);
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
    public boolean isAdultContent(String url) {
        // A list of keywords for adult content
        String[] adultKeywords = {"porn", "xxx", "adult", "sex", "erotic", "18+", "xvideo", "hentai"};

        // Check if the URL contains any of the adult keywords
        for (String keyword : adultKeywords) {
            if (url.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
