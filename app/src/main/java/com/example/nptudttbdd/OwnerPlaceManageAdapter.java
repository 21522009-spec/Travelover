package com.example.nptudttbdd;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OwnerPlaceManageAdapter extends RecyclerView.Adapter<OwnerPlaceManageAdapter.VH> {

    public interface Callback {
        void onEdit(@NonNull OwnerPlaceItem item);
        void onDelete(@NonNull OwnerPlaceItem item);
        void onToggleActive(@NonNull OwnerPlaceItem item, boolean active);
    }

    private final Callback callback;
    private final List<OwnerPlaceItem> items = new ArrayList<>();

    public OwnerPlaceManageAdapter(@NonNull Callback callback) {
        this.callback = callback;
    }

    public void submitList(@NonNull List<OwnerPlaceItem> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_owner_place_manage, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {

        private final ImageView imgPlace;
        private final TextView tvName;
        private final TextView tvLocation;
        private final TextView tvPrice;
        private final SwitchMaterial switchActive;
        private final TextView tvStatus;
        private final MaterialButton btnEdit;
        private final MaterialButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            imgPlace = itemView.findViewById(R.id.imgPlace);
            tvName = itemView.findViewById(R.id.tvName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            switchActive = itemView.findViewById(R.id.switchActive);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(@NonNull OwnerPlaceItem item) {
            tvName.setText(item.name == null ? "" : item.name);
            tvLocation.setText(item.location == null ? "" : item.location);

            String priceText = String.format(Locale.getDefault(),
                    "%s / đêm",
                    TravelDataRepository.formatCurrency(item.pricePerNight));
            tvPrice.setText(priceText);

            // imageUrl can be http(s) OR data URI base64
            ImageLoader.load(imgPlace, item.imageUrl, R.drawable.ic_placeholder);

            boolean active = item.active;
            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(active);
            tvStatus.setText(active
                    ? itemView.getContext().getString(R.string.owner_place_status_active)
                    : itemView.getContext().getString(R.string.owner_place_status_inactive));

            switchActive.setOnCheckedChangeListener((buttonView, isChecked) ->
                    callback.onToggleActive(item, isChecked));

            btnEdit.setOnClickListener(v -> callback.onEdit(item));
            btnDelete.setOnClickListener(v -> callback.onDelete(item));
        }
    }
}
