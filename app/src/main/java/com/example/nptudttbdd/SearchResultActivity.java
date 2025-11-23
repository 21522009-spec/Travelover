package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SearchResultActivity extends AppCompatActivity implements PlaceAdapter.OnPlaceClickListener {

    public static final String EXTRA_QUERY = "extra_query";

    private TravelDataRepository repository;
    private PlaceAdapter placeAdapter;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        repository = TravelDataRepository.getInstance(this);

        RecyclerView recyclerView = findViewById(R.id.recyclerSearchResults);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        placeAdapter = new PlaceAdapter(this);
        recyclerView.setAdapter(placeAdapter);

        edtSearch = findViewById(R.id.edtSearch);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String initialQuery = getIntent().getStringExtra(EXTRA_QUERY);
        if (initialQuery != null) {
            String sanitizedQuery = initialQuery.trim();
            edtSearch.setText(sanitizedQuery);
            edtSearch.setSelection(sanitizedQuery.length());
            performSearch(sanitizedQuery);
        } else {
            renderResults(repository.getPlaces());
        }

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                CharSequence currentText = edtSearch.getText();
                performSearch(currentText != null ? currentText.toString() : "");
                return true;
            }
            return false;
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s != null ? s.toString() : "");
            }


            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void performSearch(@NonNull String query) {
        List<Place> results = repository.searchPlaces(query);
        renderResults(results);
        if (results.isEmpty()) {
            Toast.makeText(this, R.string.message_no_search_result, Toast.LENGTH_SHORT).show();
        }
    }

    private void renderResults(@NonNull List<Place> places) {
        placeAdapter.submitList(places);
    }

    @Override
    public void onPlaceClick(@NonNull Place place) {
        Intent intent = new Intent(this, PlaceDetailActivity.class);
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
        startActivity(intent);
    }
}