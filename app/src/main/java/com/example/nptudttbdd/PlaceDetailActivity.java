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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        ChatButtonManager.attach(this);

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
        MaterialButton btnChatOwner = findViewById(R.id.btnChatOwner);
        MaterialButton btnWriteReview = findViewById(R.id.btnWriteReview);
        btnFavorite = findViewById(R.id.btnFavorite);

        btnBack.setOnClickListener(v -> finish());

        // Unified image loader: URL / data URI / local file path / fallback res.
        String img = place.getImagePath();
        if (!TextUtils.isEmpty(img)) {
            ImageLoader.load(imgPlace, img, place.getImageResId());
        } else {
            imgPlace.setImageResource(place.getImageResId());
        }

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

        btnChatOwner.setOnClickListener(v -> startChatWithOwner());

        btnWriteReview.setOnClickListener(v -> {
            Intent intent = new Intent(PlaceDetailActivity.this, ReviewActivity.class);
            intent.putExtra(ReviewActivity.EXTRA_PLACE_ID, place.getId());
            startActivity(intent);
        });
    }

    private void startChatWithOwner() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để nhắn tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String placeId = place.getId();

        // Get ownerId from Firebase Places/{placeId}/ownerId
        DatabaseReference ownerIdRef = FirebaseDatabase.getInstance()
                .getReference("Places")
                .child(placeId)
                .child("ownerId");

        ownerIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String ownerId = snapshot.getValue(String.class);
                if (ownerId == null || ownerId.isEmpty()) {
                    Toast.makeText(PlaceDetailActivity.this,
                            "Không tìm thấy chủ phòng trên Firebase cho địa điểm này.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String conversationId = placeId + "_" + userId;
                DatabaseReference conversationRef = FirebaseDatabase.getInstance()
                        .getReference("Conversations")
                        .child(conversationId);

                conversationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot convSnap) {
                        if (!convSnap.exists()) {
                            // Create new conversation
                            String title = "Chat phòng: " + place.getName();
                            long now = System.currentTimeMillis();
                            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(new Date());

                            Map<String, Object> convData = new HashMap<>();
                            convData.put("userId", userId);
                            convData.put("ownerId", ownerId);
                            convData.put("placeId", placeId);
                            convData.put("title", title);
                            convData.put("lastMessage", "");
                            convData.put("lastTime", timeStr);
                            convData.put("lastTimestamp", now);
                            convData.put("hasNewForOwner", false);
                            convData.put("hasNewForUser", false);
                            convData.put("hasNewMessage", false);
                            conversationRef.setValue(convData);
                        }

                        // Open user chat screen
                        Intent intent = new Intent(PlaceDetailActivity.this, UserMessagesActivity.class);
                        intent.putExtra(UserMessagesActivity.EXTRA_CONVERSATION_ID, conversationId);
                        intent.putExtra(UserMessagesActivity.EXTRA_CONVERSATION_TITLE,
                                "Chat phòng: " + place.getName());
                        startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PlaceDetailActivity.this,
                                "Không thể tạo hội thoại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PlaceDetailActivity.this,
                        "Không thể lấy thông tin chủ phòng. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
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
