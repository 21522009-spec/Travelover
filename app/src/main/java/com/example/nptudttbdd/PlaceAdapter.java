package com.example.nptudttbdd;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(@NonNull Place place);
    }

    private final OnPlaceClickListener listener;
    private final List<Place> places = new ArrayList<>();

    public PlaceAdapter(@NonNull OnPlaceClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Place> data) {
        places.clear();
        places.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        holder.bind(places.get(position));
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgPlace;
        private final TextView tvName;
        private final TextView tvLocation;
        private final TextView tvPrice;
        private final TextView tvRatingValue;
        private final RatingBar ratingBar;

        PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlace = itemView.findViewById(R.id.imgPlace);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvLocation = itemView.findViewById(R.id.tvPlaceLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRatingValue = itemView.findViewById(R.id.tvRatingValue);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }

        void bind(Place place) {
            String img = place.getImagePath();
            if (!TextUtils.isEmpty(img)) {
                ImageLoader.load(imgPlace, img, place.getImageResId());
            } else {
                imgPlace.setImageResource(place.getImageResId());
            }

            tvName.setText(place.getName());
            tvLocation.setText(place.getLocation());
            String priceText = String.format(Locale.getDefault(),
                    "%s / đêm",
                    TravelDataRepository.formatCurrency(place.getPricePerNight()));
            tvPrice.setText(priceText);
            ratingBar.setRating(place.getRating());
            tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));

            itemView.setOnClickListener(v -> listener.onPlaceClick(place));
        }
    }
}
