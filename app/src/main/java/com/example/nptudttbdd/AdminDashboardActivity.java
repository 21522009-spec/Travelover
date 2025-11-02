package com.example.nptudttbdd;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;


public class AdminDashboardActivity extends AppCompatActivity {

    private TravelDataRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        repository = TravelDataRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        TextView tvUserCount = findViewById(R.id.tvUserCount);
        TextView tvPlaceCount = findViewById(R.id.tvPlaceCount);
        TextView tvReportCount = findViewById(R.id.tvReportCount);
        Button btnManageUsers = findViewById(R.id.btnManageUsers);
        Button btnManagePlaces = findViewById(R.id.btnManagePlaces);
        Button btnViewReports = findViewById(R.id.btnViewReports);

        updateStatisticCards(tvUserCount, tvPlaceCount, tvReportCount);

        btnManageUsers.setOnClickListener(v -> startActivity(AdminManageUsersActivity.newIntent(this)));
        btnManagePlaces.setOnClickListener(v -> startActivity(AdminManagePlacesActivity.newIntent(this)));
        btnViewReports.setOnClickListener(v -> showReportDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvUserCount = findViewById(R.id.tvUserCount);
        TextView tvPlaceCount = findViewById(R.id.tvPlaceCount);
        TextView tvReportCount = findViewById(R.id.tvReportCount);
        updateStatisticCards(tvUserCount, tvPlaceCount, tvReportCount);
    }

    private void updateStatisticCards(TextView tvUserCount, TextView tvPlaceCount, TextView tvReportCount) {
        tvUserCount.setText(String.valueOf(repository.getTotalUsers()));
        tvPlaceCount.setText(String.valueOf(repository.getTotalPlaces()));
        tvReportCount.setText(String.valueOf(repository.getTotalReports()));
    }

    private void showReportDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_report_dialog_title)
                .setMessage(R.string.admin_report_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}