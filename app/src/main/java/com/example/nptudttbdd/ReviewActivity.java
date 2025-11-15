package com.example.nptudttbdd;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;

public class ReviewActivity extends AppCompatActivity {

    public static final String EXTRA_PLACE_ID = "extra_place_id";

    private TravelDataRepository repository;
    private Place place;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);
        String placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        if (placeId == null) {
            finish();
            return;
        }
        place = repository.getPlaceOrThrow(placeId);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView imgPlace = findViewById(R.id.imgPlace);
        TextView tvPlaceName = findViewById(R.id.tvPlaceName);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        EditText edtReview = findViewById(R.id.edtReview);
        MaterialButton btnSubmit = findViewById(R.id.btnSubmitReview);

        imgPlace.setImageResource(place.getImageResId());
        tvPlaceName.setText(place.getName());

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String content = edtReview.getText().toString().trim();
            if (rating <= 0f) {
                Toast.makeText(this, R.string.review_error_rating, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(content)) {
                edtReview.setError(getString(R.string.review_error_content));
                edtReview.requestFocus();
                return;
            }
            repository.addReview(place.getId(), rating, content);
            showSuccessMessage(rating, content);
        });
    }

    private void showSuccessMessage(float rating, @NonNull String content) {
        String message = getString(R.string.review_success_message,
                place.getName(),
                rating,
                content);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}