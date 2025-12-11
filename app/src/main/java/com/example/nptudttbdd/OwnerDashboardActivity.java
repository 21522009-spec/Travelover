package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class OwnerDashboardActivity extends AppCompatActivity {

    private TravelDataRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        repository = TravelDataRepository.getInstance(this);

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

        updateCards(tvRevenue, tvTotalRooms, tvBookedRooms, tvAvailableRooms, tvCleaningRooms, tvMaintenanceRooms);

        btnViewDetails.setOnClickListener(v -> showRevenueDetailDialog());
        btnAddPlace.setOnClickListener(v ->
                startActivity(new Intent(OwnerDashboardActivity.this, AddPlaceActivity.class))
        );
        btnViewDetails.setOnLongClickListener(v -> {
            startActivity(new Intent(OwnerDashboardActivity.this, OwnerConversationsActivity.class));
            return true;
        });
        btnOwnerMessages.setOnClickListener(v ->
                startActivity(new Intent(OwnerDashboardActivity.this, OwnerConversationsActivity.class))
        );
        btnOwnerPortal.setOnClickListener(v ->
                startActivity(new Intent(OwnerDashboardActivity.this, OwnerPortalActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvRevenue = findViewById(R.id.tvRevenue);
        TextView tvTotalRooms = findViewById(R.id.tvTotalRooms);
        TextView tvBookedRooms = findViewById(R.id.tvBookedRooms);
        TextView tvAvailableRooms = findViewById(R.id.tvAvailableRooms);
        TextView tvCleaningRooms = findViewById(R.id.tvCleaningRooms);
        TextView tvMaintenanceRooms = findViewById(R.id.tvMaintenanceRooms);
        updateCards(tvRevenue, tvTotalRooms, tvBookedRooms, tvAvailableRooms, tvCleaningRooms, tvMaintenanceRooms);
    }

    private void updateCards(TextView tvRevenue,
                             TextView tvTotalRooms,
                             TextView tvBookedRooms,
                             TextView tvAvailableRooms,
                             TextView tvCleaningRooms,
                             TextView tvMaintenanceRooms) {
        tvRevenue.setText(TravelDataRepository.formatCurrency(2_500_000));
        tvTotalRooms.setText(String.valueOf(repository.getTotalPlaces() * 3));
        tvBookedRooms.setText(String.valueOf(repository.getTotalPlaces() + 2));
        tvAvailableRooms.setText(String.valueOf(Math.max(0, repository.getTotalPlaces() - 1)));
        tvCleaningRooms.setText("2");
        tvMaintenanceRooms.setText("1");
    }

    private void showRevenueDetailDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.owner_revenue_dialog_title)
                .setMessage(R.string.owner_revenue_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}