package com.example.nptudttbdd;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.UUID;

public class AddPlaceActivity extends AppCompatActivity {

    private TravelDataRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);

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

            Place place = new Place(
                    UUID.randomUUID().toString(),
                    name,
                    location,
                    price,
                    4.5f,
                    0,
                    description.isEmpty() ? getString(R.string.add_place_default_description) : description,
                    R.drawable.ic_placeholder,
                    amenities);

            repository.addPlace(place);
            Toast.makeText(this, R.string.add_place_success, Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        });
    }
}