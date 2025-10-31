package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

public class UserProfileActivity extends AppCompatActivity {

    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        authManager = new FirebaseAuthManager();

        ImageButton btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        EditText edtFullName = findViewById(R.id.edtFullName);
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPhone = findViewById(R.id.edtPhone);
        EditText edtAddress = findViewById(R.id.edtAddress);
        Button btnUpdateInfo = findViewById(R.id.btnUpdateInfo);
        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        Button btnLogout = findViewById(R.id.btnLogout);

        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            if (!TextUtils.isEmpty(user.getDisplayName())) {
                edtFullName.setText(user.getDisplayName());
            }
            edtEmail.setText(user.getEmail());
        }

        btnChangeAvatar.setOnClickListener(v -> Toast.makeText(this,
                R.string.profile_change_avatar_message,
                Toast.LENGTH_SHORT).show());

        btnUpdateInfo.setOnClickListener(v -> {
            if (!validateRequired(edtFullName) || !validateRequired(edtPhone) || !validateRequired(edtAddress)) {
                return;
            }
            Toast.makeText(this, R.string.profile_update_success, Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private boolean validateRequired(EditText editText) {
        if (TextUtils.isEmpty(editText.getText().toString().trim())) {
            editText.setError(getString(R.string.profile_error_required));
            editText.requestFocus();
            return false;
        }
        return true;
    }

    private void showChangePasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_change_password_title)
                .setMessage(R.string.profile_change_password_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}