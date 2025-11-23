package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PlaceAdapter.OnPlaceClickListener {

    private TravelDataRepository repository;
    private PlaceAdapter placeAdapter;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewPlaces);
        placeAdapter = new PlaceAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(placeAdapter);

        edtSearch = findViewById(R.id.edtSearch);
        ImageButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> openSearchWithCurrentQuery());
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                openSearchWithCurrentQuery();
                return true;
            }
            return false;
        });

        setupBottomNavigation();
        renderPlaces(repository.getPlaces());
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderPlaces(repository.getPlaces());
    }

    private void renderPlaces(@NonNull List<Place> places) {
        if (places.isEmpty()) {
            Toast.makeText(this, R.string.message_no_places, Toast.LENGTH_SHORT).show();
        }
        placeAdapter.submitList(places);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                renderPlaces(repository.getPlaces());
                return true;
            } else if (id == R.id.nav_favorite) {
                startActivity(new Intent(MainActivity.this, FavoritePlacesActivity.class));
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void openSearchWithCurrentQuery() {
        CharSequence currentText = edtSearch.getText();
        String query = currentText != null ? currentText.toString().trim() : "";
        if (query.isEmpty()) {
            edtSearch.requestFocus();
            return;
        }
        openSearchScreen(query);
    }

    private void openSearchScreen(@NonNull String query) {
        Intent intent = new Intent(this, SearchResultActivity.class);
        intent.putExtra(SearchResultActivity.EXTRA_QUERY, query.trim());
        startActivity(intent);
    }
    private void openPlaceDetail(@NonNull Place place) {
        Intent intent = new Intent(this, PlaceDetailActivity.class);
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
        startActivity(intent);
    }

    @Override
    public void onPlaceClick(@NonNull Place place) {
        openPlaceDetail(place);
    }
}