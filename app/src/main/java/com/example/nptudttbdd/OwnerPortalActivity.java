package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OwnerPortalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_portal);

        TextView tvWelcome = findViewById(R.id.tvOwnerWelcome);
        tvWelcome.setText(getString(R.string.owner_portal_welcome));

        Button btnOpenDashboard = findViewById(R.id.btnOwnerDashboard);
        Button btnOpenMessages = findViewById(R.id.btnOwnerMessages);
        Button btnBackToLogin = findViewById(R.id.btnOwnerBackToLogin);

        btnOpenDashboard.setOnClickListener(v ->
                startActivity(new Intent(OwnerPortalActivity.this, OwnerDashboardActivity.class))
        );

        btnOpenMessages.setOnClickListener(v ->
                startActivity(new Intent(OwnerPortalActivity.this, OwnerMessagesActivity.class))
        );

        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(OwnerPortalActivity.this, LoginActivity.class));
            finish();
        });
    }
}