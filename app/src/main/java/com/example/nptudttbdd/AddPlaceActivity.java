package com.example.nptudttbdd;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPlaceActivity extends AppCompatActivity {

    private TravelDataRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        EditText edtName = findViewById(R.id.edtPlaceName);
        EditText edtLocation = findViewById(R.id.edtPlaceLocation);
        EditText edtPrice = findViewById(R.id.edtPrice);
        EditText edtDescription = findViewById(R.id.edtDescription);
        ImageView imgPreview = findViewById(R.id.imgPreview);
        Button btnChooseImage = findViewById(R.id.btnChooseImage);
        Button btnSave = findViewById(R.id.btnSavePlace);

        imgPreview.setImageResource(R.drawable.ic_placeholder);

        btnBack.setOnClickListener(v -> finish());
        btnChooseImage.setOnClickListener(v -> Toast.makeText(this,
                R.string.add_place_choose_image_message,
                Toast.LENGTH_SHORT).show());

        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String location = edtLocation.getText().toString().trim();
            String priceText = edtPrice.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) || TextUtils.isEmpty(priceText)) {
                Toast.makeText(this, R.string.add_place_error_required, Toast.LENGTH_SHORT).show();
                return;
            }

            long price;
            try {
                price = Long.parseLong(priceText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.add_place_error_price, Toast.LENGTH_SHORT).show();
                return;
            }

            // Use existing place as reference for amenities
            Place reference;
            try {
                reference = repository.getPlaceOrThrow("place_giang_dien");
            } catch (IllegalArgumentException ignored) {
                reference = repository.getPlaces().isEmpty() ? null : repository.getPlaces().get(0);
            }
            ArrayList<String> amenities = reference != null
                    ? new ArrayList<>(reference.getAmenities())
                    : new ArrayList<>();
            if (amenities.isEmpty()) {
                amenities.add("Wifi miễn phí");
            }

            if (description.isEmpty()) {
                description = getString(R.string.add_place_default_description);
            }

            // Determine unique ID for new place
            DatabaseReference placesRef = FirebaseDatabase.getInstance().getReference("Places");
            String placeId = placesRef.push().getKey();
            if (placeId == null || placeId.isEmpty()) {
                placeId = UUID.randomUUID().toString();
            }

            // Create Place object and update local list
            Place place = new Place(
                    placeId,
                    name,
                    location,
                    price,
                    0.0f,
                    0,
                    description,
                    R.drawable.ic_placeholder,
                    amenities);
            repository.addPlace(place);

            // Save new place to Firebase Realtime Database
            String ownerId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "";
            Map<String, Object> placeData = new HashMap<>();
            placeData.put("id", placeId);
            placeData.put("name", name);
            placeData.put("location", location);
            placeData.put("pricePerNight", price);
            placeData.put("rating", 0);
            placeData.put("ratingCount", 0);
            placeData.put("description", description);
            placeData.put("amenities", amenities);
            placeData.put("ownerId", ownerId);
            placesRef.child(placeId).setValue(placeData);

            Toast.makeText(this, R.string.add_place_success, Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        });
    }
}
