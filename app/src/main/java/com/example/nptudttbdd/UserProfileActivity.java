package com.example.nptudttbdd;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {
    private static final String TAG = "UserProfileActivity";

    private FirebaseAuthManager authManager;
    private StorageReference avatarStorageRef;
    private EditText edtFullName;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtAddress;
    private CircleImageView imgAvatar;
    private UserProfile currentProfile;
    private Uri selectedAvatarUri;

    private ActivityResultLauncher<String[]> pickImageLauncher;
    private Uri persistedAvatarPermissionUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ChatButtonManager.attach(this);

        authManager = new FirebaseAuthManager();
        avatarStorageRef = buildAvatarStorageReference();

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                handleAvatarSelection(uri);
            }
        });

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

        btnChangeAvatar.setOnClickListener(v -> openImagePicker());

        btnUpdateInfo.setOnClickListener(v -> {
            if (!validateRequired(edtFullName) || !validateRequired(edtPhone) || !validateRequired(edtAddress)) {
                return;
            }
            saveProfileChanges();
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            authManager.logout();
            Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePersistedAvatarPermission();
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null, false);
        TextInputEditText edtCurrentPassword = dialogView.findViewById(R.id.edtCurrentPassword);
        TextInputEditText edtNewPassword = dialogView.findViewById(R.id.edtNewPassword);
        TextInputEditText edtConfirmPassword = dialogView.findViewById(R.id.edtConfirmPassword);
        View progressBar = dialogView.findViewById(R.id.progressChangePassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.profile_change_password_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.profile_change_password_confirm, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> attemptChangePassword(dialog,
                    positiveButton,
                    progressBar,
                    edtCurrentPassword,
                    edtNewPassword,
                    edtConfirmPassword));
        });

        dialog.show();
    }

    private void openImagePicker() {
        if (pickImageLauncher != null) {
            pickImageLauncher.launch(new String[]{"image/*"});
        }
    }

    private void handleAvatarSelection(@NonNull Uri uri) {
        releasePersistedAvatarPermission();
        int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
            persistedAvatarPermissionUri = uri;
        } catch (SecurityException ignored) {
        }
        selectedAvatarUri = uri;
        imgAvatar.setImageURI(uri);
    }

    private void releasePersistedAvatarPermission() {
        if (persistedAvatarPermissionUri == null) {
            return;
        }
        int releaseFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            getContentResolver().releasePersistableUriPermission(persistedAvatarPermissionUri, releaseFlags);
        } catch (SecurityException ignored) {
        }
        persistedAvatarPermissionUri = null;
    }

    @NonNull
    private StorageReference buildAvatarStorageReference() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            String bucket = app.getOptions().getStorageBucket();
            if (!TextUtils.isEmpty(bucket)) {
                String normalizedBucket = bucket.startsWith("gs://") ? bucket : "gs://" + bucket;
                try {
                    return storage.getReferenceFromUrl(normalizedBucket).child("user_avatars");
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Invalid storage bucket " + bucket + ", falling back to default", e);
                }
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "FirebaseApp is not initialized yet", e);
        }
        return storage.getReference().child("user_avatars");
    }

    private void attemptChangePassword(@NonNull AlertDialog dialog,
                                       @NonNull Button positiveButton,
                                       @NonNull View progressBar,
                                       @NonNull TextInputEditText edtCurrentPassword,
                                       @NonNull TextInputEditText edtNewPassword,
                                       @NonNull TextInputEditText edtConfirmPassword) {
        String currentPassword = edtCurrentPassword.getText() != null
                ? edtCurrentPassword.getText().toString().trim() : "";
        String newPassword = edtNewPassword.getText() != null
                ? edtNewPassword.getText().toString().trim() : "";
        String confirmPassword = edtConfirmPassword.getText() != null
                ? edtConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(currentPassword)) {
            edtCurrentPassword.setError(getString(R.string.profile_change_password_error_current_required));
            edtCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            edtNewPassword.setError(getString(R.string.profile_change_password_error_new_required));
            edtNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            edtNewPassword.setError(getString(R.string.profile_change_password_error_length));
            edtNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.profile_change_password_error_mismatch));
            edtConfirmPassword.requestFocus();
            return;
        }

        FirebaseUser firebaseUser = authManager.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, R.string.profile_change_password_error_user, Toast.LENGTH_SHORT).show();
            return;
        }

        String email = firebaseUser.getEmail();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.profile_change_password_error_email, Toast.LENGTH_SHORT).show();
            return;
        }

        positiveButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> firebaseUser.updatePassword(newPassword)
                        .addOnSuccessListener(unused1 -> {
                            progressBar.setVisibility(View.GONE);
                            positiveButton.setEnabled(true);
                            dialog.dismiss();
                            Toast.makeText(this, R.string.profile_change_password_success, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> handlePasswordChangeError(progressBar, positiveButton, edtNewPassword, e)))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    positiveButton.setEnabled(true);
                    edtCurrentPassword.setError(getString(R.string.profile_change_password_error_invalid_current));
                    edtCurrentPassword.requestFocus();
                });
    }

    private void handlePasswordChangeError(@NonNull View progressBar,
                                           @NonNull Button positiveButton,
                                           @NonNull TextInputEditText edtNewPassword,
                                           @NonNull Exception e) {
        progressBar.setVisibility(View.GONE);
        positiveButton.setEnabled(true);
        String message = e.getLocalizedMessage();
        edtNewPassword.setError(!TextUtils.isEmpty(message) ? message : getString(R.string.profile_change_password_generic_error));
        edtNewPassword.requestFocus();
    }
    private void loadUserProfile(@NonNull FirebaseUser firebaseUser) {
        authManager.getUserProfileReference(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserProfile profile = snapshot.getValue(UserProfile.class);
                        if (profile != null) {
                            if (TextUtils.isEmpty(profile.getUid())) {
                                profile.setUid(firebaseUser.getUid());
                            }
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
        currentProfile = profile;
        edtFullName.setText(profile.getName());
        edtEmail.setText(profile.getEmail());
        edtPhone.setText(profile.getPhone());
        edtAddress.setText(profile.getAddress());

        selectedAvatarUri = null;
        if (TextUtils.isEmpty(profile.getAvatarUrl())) {
            imgAvatar.setImageResource(R.drawable.default_avatar);
        } else {
            Glide.with(this)
                    .load(profile.getAvatarUrl())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(imgAvatar);
        }
    }

    private void bindFallbackUser(@NonNull FirebaseUser firebaseUser) {
        String fullName = TextUtils.isEmpty(firebaseUser.getDisplayName())
                ? ""
                : firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail() == null ? "" : firebaseUser.getEmail();
        currentProfile = new UserProfile(firebaseUser.getUid(),
                fullName,
                email,
                "user",
                "");
        edtFullName.setText(fullName);
        edtEmail.setText(email);
        edtPhone.setText("");
        edtAddress.setText("");
        imgAvatar.setImageResource(R.drawable.default_avatar);
        selectedAvatarUri = null;
    }

    private void saveProfileChanges() {
        FirebaseUser firebaseUser = authManager.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, R.string.profile_update_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentProfile == null) {
            currentProfile = new UserProfile(firebaseUser.getUid(),
                    "",
                    firebaseUser.getEmail() == null ? "" : firebaseUser.getEmail(),
                    "user",
                    "");
        } else if (TextUtils.isEmpty(currentProfile.getUid())) {
            currentProfile.setUid(firebaseUser.getUid());
        }

        currentProfile.setName(edtFullName.getText().toString().trim());
        currentProfile.setPhone(edtPhone.getText().toString().trim());
        currentProfile.setAddress(edtAddress.getText().toString().trim());

        if (selectedAvatarUri != null) {
            uploadAvatarAndSave(currentProfile);
        } else {
            persistProfile(currentProfile);
        }
    }

    private void uploadAvatarAndSave(@NonNull UserProfile profile) {
        String uid = profile.getUid();
        if (uid.isEmpty()) {
            Toast.makeText(this, R.string.profile_update_error, Toast.LENGTH_SHORT).show();
            return;
        }

        StorageReference userAvatarRef = avatarStorageRef.child(uid + ".jpg");
        userAvatarRef.putFile(selectedAvatarUri)
                .addOnSuccessListener(taskSnapshot -> userAvatarRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            profile.setAvatarUrl(uri.toString());
                            selectedAvatarUri = null;
                            persistProfile(profile);
                        })
                        .addOnFailureListener(e -> Toast.makeText(UserProfileActivity.this,
                                getString(R.string.profile_update_error),
                                Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(UserProfileActivity.this,
                        getString(R.string.profile_update_error),
                        Toast.LENGTH_SHORT).show());
    }

    private void persistProfile(@NonNull UserProfile profile) {
        authManager.saveUserProfile(profile, new FirebaseAuthManager.CompletionCallback() {
            @Override
            public void onComplete() {
                Toast.makeText(UserProfileActivity.this,
                        R.string.profile_update_success,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull String message) {
                Toast.makeText(UserProfileActivity.this,
                        message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}