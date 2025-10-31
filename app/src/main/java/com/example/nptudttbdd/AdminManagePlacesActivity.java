package com.example.nptudttbdd;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class AdminManagePlacesActivity extends AppCompatActivity {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, AdminManagePlacesActivity.class);
    }

    private TravelDataRepository repository;
    private LinearLayout placeListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_places);

        repository = TravelDataRepository.getInstance(this);
        placeListContainer = findViewById(R.id.placeList);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        renderPlaces();
    }

    private void renderPlaces() {
        List<Place> places = repository.getPlaces();
        placeListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Place place : places) {
            View itemView = inflater.inflate(R.layout.item_admin_place, placeListContainer, false);
            bindPlace(itemView, place);
            placeListContainer.addView(itemView);
        }
    }

    private void bindPlace(View itemView, Place place) {
        ImageView imgPlace = itemView.findViewById(R.id.imgPlace);
        TextView tvName = itemView.findViewById(R.id.tvName);
        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
        TextView tvPrice = itemView.findViewById(R.id.tvPrice);
        Button btnDelete = itemView.findViewById(R.id.btnDelete);
        EditText etReason = itemView.findViewById(R.id.etDeleteReason);
        Button btnConfirmDelete = itemView.findViewById(R.id.btnConfirmDelete);

        imgPlace.setImageResource(place.getImageResId());
        tvName.setText(place.getName());
        tvLocation.setText(place.getLocation());
        tvPrice.setText(getString(R.string.admin_place_price_template,
                TravelDataRepository.formatCurrency(place.getPricePerNight())));

        btnDelete.setOnClickListener(v -> {
            if (etReason.getVisibility() == View.VISIBLE) {
                etReason.setVisibility(View.GONE);
                btnConfirmDelete.setVisibility(View.GONE);
            } else {
                etReason.setVisibility(View.VISIBLE);
                btnConfirmDelete.setVisibility(View.VISIBLE);
            }
        });

        btnConfirmDelete.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                etReason.setError(getString(R.string.admin_place_reason_error));
                etReason.requestFocus();
                return;
            }
            repository.removePlace(place.getId());
            Toast.makeText(this,
                    getString(R.string.admin_place_deleted, place.getName()),
                    Toast.LENGTH_SHORT).show();
            renderPlaces();
        });
    }
}