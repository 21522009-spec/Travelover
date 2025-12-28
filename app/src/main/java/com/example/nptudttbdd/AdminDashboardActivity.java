package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.MaterialToolbar;

public class AdminDashboardActivity extends AppCompatActivity {

    private TravelDataRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        ChatButtonManager.attach(this);

        repository = TravelDataRepository.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        btnViewReports.setOnClickListener(v -> startActivity(AdminReportsActivity.newIntent(this)));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu chứa nút Đăng xuất
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            // Xử lý đăng xuất về LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
