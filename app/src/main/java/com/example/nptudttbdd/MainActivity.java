package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PlaceAdapter.OnPlaceClickListener {

    private TravelDataRepository repository;
    private PlaceAdapter placeAdapter;
    private EditText edtSearch;

    // Sync places created by Owner (from Realtime Database) so User can see and book them.
    private DatabaseReference placesRef;
    private Query activePlacesQuery;
    private ValueEventListener placesListener;
    private final HashSet<String> syncedPlaceIds = new HashSet<>();

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

        placesRef = FirebaseDatabase.getInstance().getReference("Places");
        activePlacesQuery = placesRef.orderByChild("active").equalTo(true);

        renderPlaces(repository.getPlaces());
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListeningPlaces();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListeningPlaces();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderPlaces(repository.getPlaces());
    }

    private void startListeningPlaces() {
        if (placesListener != null) return;

        placesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashSet<String> newIds = new HashSet<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.child("id").getValue(String.class);
                    if (TextUtils.isEmpty(id)) id = child.getKey();
                    if (TextUtils.isEmpty(id)) continue;

                    String name = child.child("name").getValue(String.class);
                    String location = child.child("location").getValue(String.class);
                    Long price = child.child("pricePerNight").getValue(Long.class);
                    String description = child.child("description").getValue(String.class);

                    Double ratingD = child.child("rating").getValue(Double.class);
                    Long ratingCountL = child.child("ratingCount").getValue(Long.class);

                    float rating = ratingD == null ? 0f : ratingD.floatValue();
                    int ratingCount = ratingCountL == null ? 0 : ratingCountL.intValue();

                    // imageUrl can be http(s) OR data URI base64
                    String imageUrl = child.child("imageUrl").getValue(String.class);
                    if (TextUtils.isEmpty(imageUrl)) {
                        DataSnapshot urls = child.child("imageUrls");
                        if (urls.exists()) {
                            for (DataSnapshot u : urls.getChildren()) {
                                String s = u.getValue(String.class);
                                if (!TextUtils.isEmpty(s)) {
                                    imageUrl = s;
                                    break;
                                }
                            }
                        }
                    }
                    if (imageUrl == null) imageUrl = "";

                    ArrayList<String> amenities = new ArrayList<>();
                    DataSnapshot amenSnap = child.child("amenities");
                    if (amenSnap.exists()) {
                        for (DataSnapshot a : amenSnap.getChildren()) {
                            String s = a.getValue(String.class);
                            if (!TextUtils.isEmpty(s)) amenities.add(s);
                        }
                    }
                    if (amenities.isEmpty()) {
                        // fallback: use sample template
                        try {
                            amenities.addAll(repository.getPlaces().get(0).getAmenities());
                        } catch (Exception ignored) {
                            amenities.add("Wifi miễn phí");
                        }
                    }

                    Place place = new Place(
                            id,
                            name == null ? "Phòng cho thuê" : name,
                            location == null ? "" : location,
                            price == null ? 0L : price,
                            rating,
                            ratingCount,
                            description == null ? "" : description,
                            R.drawable.ic_placeholder,
                            amenities,
                            imageUrl
                    );

                    repository.upsertPlace(place);
                    newIds.add(id);
                }

                // Remove places that are no longer active / removed in DB.
                for (String oldId : new HashSet<>(syncedPlaceIds)) {
                    if (!newIds.contains(oldId)) {
                        repository.removePlace(oldId);
                    }
                }
                syncedPlaceIds.clear();
                syncedPlaceIds.addAll(newIds);

                renderPlaces(repository.getPlaces());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Không thể tải phòng từ server: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        activePlacesQuery.addValueEventListener(placesListener);
    }

    private void stopListeningPlaces() {
        if (activePlacesQuery != null && placesListener != null) {
            activePlacesQuery.removeEventListener(placesListener);
        }
        placesListener = null;
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
