package com.example.nptudttbdd;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OwnerManagePlacesActivity extends AppCompatActivity implements OwnerPlaceManageAdapter.Callback {

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, OwnerManagePlacesActivity.class);
    }

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;

    private OwnerPlaceManageAdapter adapter;

    private DatabaseReference placesRef;
    private ValueEventListener placesListener;
    private Query ownerPlacesQuery;

    private String ownerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_manage_places);
        ChatButtonManager.attach(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        fabAdd = findViewById(R.id.fabAdd);

        adapter = new OwnerPlaceManageAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ownerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (ownerId.trim().isEmpty()) {
            Toast.makeText(this, "Bạn cần đăng nhập Owner để tiếp tục.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        placesRef = FirebaseDatabase.getInstance().getReference("Places");

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddPlaceActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListening();
    }

    private void startListening() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ownerPlacesQuery = placesRef.orderByChild("ownerId").equalTo(ownerId);

        placesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<OwnerPlaceItem> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    OwnerPlaceItem item = child.getValue(OwnerPlaceItem.class);
                    if (item == null) continue;
                    if (item.id == null || item.id.trim().isEmpty()) item.id = child.getKey();
                    list.add(item);
                }

                Collections.sort(list, new Comparator<OwnerPlaceItem>() {
                    @Override
                    public int compare(OwnerPlaceItem a, OwnerPlaceItem b) {
                        String an = a.name == null ? "" : a.name;
                        String bn = b.name == null ? "" : b.name;
                        return an.compareToIgnoreCase(bn);
                    }
                });

                adapter.submitList(list);
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OwnerManagePlacesActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        ownerPlacesQuery.addValueEventListener(placesListener);
    }

    private void stopListening() {
        if (ownerPlacesQuery != null && placesListener != null) {
            ownerPlacesQuery.removeEventListener(placesListener);
        }
        placesListener = null;
        ownerPlacesQuery = null;
    }

    @Override
    public void onEdit(@NonNull OwnerPlaceItem item) {
        if (item.id == null || item.id.trim().isEmpty()) return;
        Intent intent = new Intent(this, OwnerEditPlaceActivity.class);
        intent.putExtra(OwnerEditPlaceActivity.EXTRA_PLACE_ID, item.id);
        startActivity(intent);
    }

    @Override
    public void onDelete(@NonNull OwnerPlaceItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.owner_place_delete_title)
                .setMessage(R.string.owner_place_delete_message)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (item.id == null || item.id.trim().isEmpty()) return;
                    placesRef.child(item.id).removeValue()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, R.string.owner_place_deleted, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Không thể xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onToggleActive(@NonNull OwnerPlaceItem item, boolean active) {
        if (item.id == null || item.id.trim().isEmpty()) return;
        placesRef.child(item.id).child("active").setValue(active)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không thể cập nhật trạng thái: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
