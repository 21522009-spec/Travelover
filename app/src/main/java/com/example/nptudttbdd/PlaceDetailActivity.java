package com.example.nptudttbdd;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class PlaceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "extra_place_id";

    private TravelDataRepository repository;
    private Place place;
    private RatingBar ratingBar;
    private TextView tvRatingValue;
    private ImageView btnFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        repository = TravelDataRepository.getInstance(this);

        String placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        if (placeId == null) {
            finish();
            return;
        }

        place = repository.getPlaceOrThrow(placeId);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView imgPlace = findViewById(R.id.imgPlace);
        TextView tvPlaceName = findViewById(R.id.tvPlaceName);
        TextView tvLocation = findViewById(R.id.tvLocation);
        ratingBar = findViewById(R.id.ratingBar);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        TextView tvDescription = findViewById(R.id.tvDescription);
        MaterialButton btnBookRoom = findViewById(R.id.btnBookRoom);
        MaterialButton btnWriteReview = findViewById(R.id.btnWriteReview);
        btnFavorite = findViewById(R.id.btnFavorite);

        btnBack.setOnClickListener(v -> finish());

        imgPlace.setImageResource(place.getImageResId());
        tvPlaceName.setText(place.getName());
        tvLocation.setText(place.getLocation());
        updateRatingSection();
        updateFavoriteIcon();

        String description = place.getDescription();
        if (!place.getAmenities().isEmpty()) {
            String amenities = getString(R.string.place_detail_amenities_prefix,
                    TextUtils.join(", ", place.getAmenities()));
            description = description + "\n\n" + amenities;
        }
        tvDescription.setText(description);

        btnFavorite.setOnClickListener(v -> {
            boolean isFavorite = repository.toggleFavorite(place.getId());
            updateFavoriteIcon();
            int messageRes = isFavorite ? R.string.favorite_added : R.string.favorite_removed;
            Toast.makeText(this,
                            getString(messageRes, place.getName()),
                            Toast.LENGTH_SHORT)
                    .show();
        });

        btnBookRoom.setOnClickListener(v -> {
            Intent intent = new Intent(PlaceDetailActivity.this, BookingActivity.class);
            intent.putExtra(BookingActivity.EXTRA_PLACE_ID, place.getId());
            startActivity(intent);
        });

        btnWriteReview.setOnClickListener(v -> {
            Intent intent = new Intent(PlaceDetailActivity.this, ReviewActivity.class);
            intent.putExtra(ReviewActivity.EXTRA_PLACE_ID, place.getId());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRatingSection();
        updateFavoriteIcon();
    }

    private void updateRatingSection() {
        float rating = place.getRating();
        ratingBar.setRating(rating);
        tvRatingValue.setText(String.format(Locale.getDefault(),
                "%.1f/5 (%d đánh giá)",
                rating,
                place.getRatingCount()));
    }

    private void updateFavoriteIcon() {
        if (btnFavorite == null) {
            return;
        }
        boolean isFavorite = repository.isFavorite(place.getId());
        int colorRes = isFavorite ? R.color.favorite_star_active : R.color.favorite_star_inactive;
        ColorStateList tint = ColorStateList.valueOf(ContextCompat.getColor(this, colorRes));
        ImageViewCompat.setImageTintList(btnFavorite, tint);
    }
}