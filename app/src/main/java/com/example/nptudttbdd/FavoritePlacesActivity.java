package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FavoritePlacesActivity extends AppCompatActivity implements PlaceAdapter.OnPlaceClickListener {

    private TravelDataRepository repository;
    private PlaceAdapter placeAdapter;
    private TextView tvEmptyFavorites;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_places);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerViewFavorites);
        tvEmptyFavorites = findViewById(R.id.tvEmptyFavorites);

        placeAdapter = new PlaceAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(placeAdapter);

        btnBack.setOnClickListener(v -> finish());

        renderFavorites(repository.getFavoritePlaces());
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderFavorites(repository.getFavoritePlaces());
    }

    private void renderFavorites(@NonNull List<Place> favorites) {
        placeAdapter.submitList(favorites);
        boolean isEmpty = favorites.isEmpty();
        tvEmptyFavorites.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPlaceClick(@NonNull Place place) {
        Intent intent = new Intent(this, PlaceDetailActivity.class);
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
        startActivity(intent);
    }
}