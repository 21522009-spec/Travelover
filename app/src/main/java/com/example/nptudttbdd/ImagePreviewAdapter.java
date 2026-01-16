package com.example.nptudttbdd;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.VH> {

    public interface Callback {
        void onRemove(int position);
        void onClick(int position);
    }

    private final Callback callback;
    private final List<ImagePreviewItem> items = new ArrayList<>();

    public ImagePreviewAdapter(@NonNull Callback callback) {
        this.callback = callback;
    }

    public void submit(@NonNull List<ImagePreviewItem> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    public List<ImagePreviewItem> getItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_preview, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        ImageView img;
        ImageButton btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgThumb);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(@NonNull ImagePreviewItem item, int position) {
            if (item.isLocal()) {
                Glide.with(img).load(item.localUri).centerCrop().into(img);
            } else if (!TextUtils.isEmpty(item.remoteUrl)) {
                // remoteUrl can be http(s) or data URI base64
                ImageLoader.load(img, item.remoteUrl, R.drawable.ic_placeholder);
            } else {
                img.setImageResource(R.drawable.ic_placeholder);
            }

            img.setOnClickListener(v -> callback.onClick(position));
            btnRemove.setOnClickListener(v -> callback.onRemove(position));
        }
    }
}
