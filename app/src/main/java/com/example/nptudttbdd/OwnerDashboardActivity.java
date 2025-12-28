package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class OwnerDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        // Greeting TextView for owner
        TextView tvWelcome = findViewById(R.id.tvOwnerWelcome);
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        if (!currentUid.isEmpty()) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
            usersRef.child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    UserProfile profile = snapshot.getValue(UserProfile.class);
                    if (profile != null) {
                        tvWelcome.setText("Xin chào, " + profile.getName() + "!");
                    } else {
                        tvWelcome.setText("Xin chào!");
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    new AlertDialog.Builder(OwnerDashboardActivity.this)
                            .setTitle("Doanh thu theo tháng")
                            .setMessage("Không thể tải dữ liệu doanh thu: " + error.getMessage()
                                    + "\n\nKiểm tra Firebase Rules cho /Payments (Permission denied).")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });
        }

        TextView tvRevenue = findViewById(R.id.tvRevenue);
        TextView tvTotalRooms = findViewById(R.id.tvTotalRooms);
        TextView tvBookedRooms = findViewById(R.id.tvBookedRooms);
        TextView tvAvailableRooms = findViewById(R.id.tvAvailableRooms);
        TextView tvCleaningRooms = findViewById(R.id.tvCleaningRooms);
        TextView tvMaintenanceRooms = findViewById(R.id.tvMaintenanceRooms);
        Button btnViewDetails = findViewById(R.id.btnViewDetails);
        Button btnOwnerMessages = findViewById(R.id.btnOwnerMessages);
        Button btnOwnerPortal = findViewById(R.id.btnOwnerPortal);
        Button btnAddPlace = findViewById(R.id.btnAddPlace);
        Button btnManagePlaces = findViewById(R.id.btnManagePlaces);

        // Load initial dashboard stats
        updateDashboardStats(tvRevenue, tvTotalRooms, tvBookedRooms, tvAvailableRooms, tvCleaningRooms, tvMaintenanceRooms);

        btnViewDetails.setOnClickListener(v -> showRevenueDetailDialog());
        btnAddPlace.setOnClickListener(v ->
                startActivity(new Intent(OwnerDashboardActivity.this, AddPlaceActivity.class))
        );
        btnManagePlaces.setOnClickListener(v ->
                startActivity(new Intent(OwnerDashboardActivity.this, OwnerManagePlacesActivity.class))
        );

        btnOwnerMessages.setOnClickListener(v ->
                startActivity(new Intent(OwnerDashboardActivity.this, OwnerConversationsActivity.class))
        );
        // Replace the back-to-dashboard button with a logout button
        btnOwnerPortal.setText(R.string.owner_portal_back_to_login);
        btnOwnerPortal.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(OwnerDashboardActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats whenever activity resumes
        TextView tvRevenue = findViewById(R.id.tvRevenue);
        TextView tvTotalRooms = findViewById(R.id.tvTotalRooms);
        TextView tvBookedRooms = findViewById(R.id.tvBookedRooms);
        TextView tvAvailableRooms = findViewById(R.id.tvAvailableRooms);
        TextView tvCleaningRooms = findViewById(R.id.tvCleaningRooms);
        TextView tvMaintenanceRooms = findViewById(R.id.tvMaintenanceRooms);
        updateDashboardStats(tvRevenue, tvTotalRooms, tvBookedRooms, tvAvailableRooms, tvCleaningRooms, tvMaintenanceRooms);
    }

    private void updateDashboardStats(TextView tvRevenue,
                                      TextView tvTotalRooms,
                                      TextView tvBookedRooms,
                                      TextView tvAvailableRooms,
                                      TextView tvCleaningRooms,
                                      TextView tvMaintenanceRooms) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        if (currentUid.isEmpty()) {
            return;
        }
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        // Query places owned by current user
        Query placesQuery = dbRef.child("Places").orderByChild("ownerId").equalTo(currentUid);
        placesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot placesSnapshot) {
                final int[] placeCount = {0};
                for (DataSnapshot placeData : placesSnapshot.getChildren()) {
                    placeCount[0] += 1;
                }
                // Query bookings for current user's places
                Query bookingsQuery = dbRef.child("Bookings").orderByChild("ownerId").equalTo(currentUid);
                bookingsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot bookingsSnapshot) {
                        int bookingCount = 0;
                        for (DataSnapshot bookingData : bookingsSnapshot.getChildren()) {
                            bookingCount += 1;
                        }
                        // Compute dashboard figures
                        int totalRooms = placeCount[0];
                        int bookedRooms = Math.min(placeCount[0], bookingCount);
                        int availableRooms = Math.max(0, totalRooms - bookedRooms);
                        tvTotalRooms.setText(String.valueOf(totalRooms));
                        tvBookedRooms.setText(String.valueOf(bookedRooms));
                        tvAvailableRooms.setText(String.valueOf(availableRooms));
                        // Static values for cleaning and maintenance (no dynamic data)
                        tvCleaningRooms.setText("2");
                        tvMaintenanceRooms.setText("1");
                        // Query total revenue from payments
                        Query paymentsQuery = dbRef.child("Payments").orderByChild("ownerId").equalTo(currentUid);
                        paymentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot paymentsSnapshot) {
                                long totalRevenue = 0;
                                for (DataSnapshot paymentData : paymentsSnapshot.getChildren()) {
                                    Long amount = paymentData.child("amount").getValue(Long.class);
                                    if (amount != null) {
                                        totalRevenue += amount;
                                    }
                                }
                                tvRevenue.setText(TravelDataRepository.formatCurrency(totalRevenue));
                            }
                            @Override
                            public void onCancelled(DatabaseError error) {
                                // Handle error if needed
                            }
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Handle error if needed
                    }
                });
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error if needed
            }
        });
    }

    private void showRevenueDetailDialog() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        if (currentUid.isEmpty()) {
            new AlertDialog.Builder(OwnerDashboardActivity.this)
                    .setTitle("Doanh thu theo tháng")
                    .setMessage("Chưa có dữ liệu doanh thu.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        Query paymentsQuery = dbRef.child("Payments").orderByChild("ownerId").equalTo(currentUid);
        paymentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot paymentsSnapshot) {
                Map<String, Long> monthlyRevenue = new LinkedHashMap<>();
                for (DataSnapshot paymentData : paymentsSnapshot.getChildren()) {
                    Long amount = paymentData.child("amount").getValue(Long.class);
                    Long timestamp = paymentData.child("timestamp").getValue(Long.class);
                    if (amount != null && timestamp != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(timestamp);
                        int month = cal.get(Calendar.MONTH) + 1;
                        int year = cal.get(Calendar.YEAR);
                        String key = month + "/" + year;
                        monthlyRevenue.put(key, monthlyRevenue.getOrDefault(key, 0L) + amount);
                    }
                }
                if (monthlyRevenue.isEmpty()) {
                    new AlertDialog.Builder(OwnerDashboardActivity.this)
                            .setTitle("Doanh thu theo tháng")
                            .setMessage("Chưa có dữ liệu doanh thu.")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    StringBuilder message = new StringBuilder();
                    for (Map.Entry<String, Long> entry : monthlyRevenue.entrySet()) {
                        message.append("Tháng ").append(entry.getKey()).append(": ")
                                .append(TravelDataRepository.formatCurrency(entry.getValue()))
                                .append("\n");
                    }
                    new AlertDialog.Builder(OwnerDashboardActivity.this)
                            .setTitle("Doanh thu theo tháng")
                            .setMessage(message.toString())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error if needed
            }
        });
    }
}