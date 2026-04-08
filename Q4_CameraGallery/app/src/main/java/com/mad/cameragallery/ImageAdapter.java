package com.mad.cameragallery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    public interface OnImageClickListener {
        void onImageClicked(ImageItem imageItem);
    }

    private final List<ImageItem> items = new ArrayList<>();
    private final OnImageClickListener listener;

    public ImageAdapter(OnImageClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ImageItem> imageItems) {
        items.clear();
        items.addAll(imageItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem item = items.get(position);
        holder.nameText.setText(item.getDisplayName());
        holder.metaText.setText(item.getGalleryMeta());

        Glide.with(holder.thumbnailView)
                .load(item.getUri())
                .centerCrop()
                .into(holder.thumbnailView);

        holder.itemView.setOnClickListener(view -> listener.onImageClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnailView;
        private final TextView nameText;
        private final TextView metaText;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailView = itemView.findViewById(R.id.thumbnailView);
            nameText = itemView.findViewById(R.id.nameText);
            metaText = itemView.findViewById(R.id.metaText);
        }
    }
}
