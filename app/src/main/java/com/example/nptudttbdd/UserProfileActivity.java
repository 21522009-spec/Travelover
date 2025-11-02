package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private FirebaseAuthManager authManager;
    private EditText edtFullName;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtAddress;
    private CircleImageView imgAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        authManager = new FirebaseAuthManager();
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        ImageButton btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        imgAvatar = findViewById(R.id.imgAvatar);
        imgAvatar.setImageResource(R.drawable.default_avatar);
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        Button btnUpdateInfo = findViewById(R.id.btnUpdateInfo);
        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        Button btnLogout = findViewById(R.id.btnLogout);

        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            loadUserProfile(user);
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

    private void loadUserProfile(@NonNull FirebaseUser firebaseUser) {
        authManager.getUserProfileReference(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserProfile profile = snapshot.getValue(UserProfile.class);
                        if (profile != null) {
                            bindProfile(profile);
                        } else {
                            bindFallbackUser(firebaseUser);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        bindFallbackUser(firebaseUser);
                    }
                });
    }

    private void bindProfile(@NonNull UserProfile profile) {
        edtFullName.setText(profile.getName());
        edtEmail.setText(profile.getEmail());

        if (TextUtils.isEmpty(profile.getAvatarUrl())) {
            imgAvatar.setImageResource(R.drawable.default_avatar);
        } else {
            imgAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    private void bindFallbackUser(@NonNull FirebaseUser firebaseUser) {
        if (!TextUtils.isEmpty(firebaseUser.getDisplayName())) {
            edtFullName.setText(firebaseUser.getDisplayName());
        }
        edtEmail.setText(firebaseUser.getEmail());
        imgAvatar.setImageResource(R.drawable.default_avatar);
    }
}