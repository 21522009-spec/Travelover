package com.example.nptudttbdd;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
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

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {
    private static final String TAG = "UserProfileActivity";

    private FirebaseAuthManager authManager;

    private EditText edtFullName;
    private EditText edtEmail;
    private EditText edtPhone;
    private EditText edtAddress;
    private CircleImageView imgAvatar;

    private UserProfile currentProfile;
    private Uri selectedAvatarUri;

    // Chọn ảnh từ thư viện
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ChatButtonManager.attach(this);

        authManager = new FirebaseAuthManager();

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
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

    private boolean validateRequired(EditText editText) {
        if (TextUtils.isEmpty(editText.getText().toString().trim())) {
            editText.setError(getString(R.string.profile_error_required));
            editText.requestFocus();
            return false;
        }
        return true;
    }

    private void openImagePicker() {
        if (pickImageLauncher != null) {
            pickImageLauncher.launch("image/*");
        }
    }

    private void handleAvatarSelection(@NonNull Uri uri) {
        selectedAvatarUri = uri;

        // Preview bằng Glide để chắc chắn hiển thị
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(imgAvatar);
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

        // Nếu user vừa chọn ảnh mới => ưu tiên preview
        if (selectedAvatarUri != null) {
            Glide.with(this)
                    .load(selectedAvatarUri)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(imgAvatar);
            return;
        }

        // Ưu tiên Base64 (không cần Storage)
        if (!TextUtils.isEmpty(profile.getAvatarBase64())) {
            try {
                byte[] data = Base64.decode(profile.getAvatarBase64(), Base64.NO_WRAP);
                Glide.with(this)
                        .asBitmap()
                        .load(data)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(imgAvatar);
                return;
            } catch (Exception e) {
                Log.e(TAG, "decode avatarBase64 failed", e);
            }
        }

        // Fallback: nếu trước đó bạn từng lưu avatarUrl
        if (!TextUtils.isEmpty(profile.getAvatarUrl())) {
            Glide.with(this)
                    .load(profile.getAvatarUrl())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    private void bindFallbackUser(@NonNull FirebaseUser firebaseUser) {
        String fullName = TextUtils.isEmpty(firebaseUser.getDisplayName())
                ? ""
                : firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail() == null ? "" : firebaseUser.getEmail();

        // Giữ constructor như file cũ để không vỡ project
        currentProfile = new UserProfile(firebaseUser.getUid(), fullName, email, "user", "");
        currentProfile.setPhone("");
        currentProfile.setAddress("");
        currentProfile.setAvatarUrl("");
        currentProfile.setAvatarBase64("");

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
            currentProfile = new UserProfile(
                    firebaseUser.getUid(),
                    "",
                    firebaseUser.getEmail() == null ? "" : firebaseUser.getEmail(),
                    "user",
                    ""
            );
            currentProfile.setAvatarUrl("");
            currentProfile.setAvatarBase64("");
        } else if (TextUtils.isEmpty(currentProfile.getUid())) {
            currentProfile.setUid(firebaseUser.getUid());
        }

        currentProfile.setName(edtFullName.getText().toString().trim());
        currentProfile.setPhone(edtPhone.getText().toString().trim());
        currentProfile.setAddress(edtAddress.getText().toString().trim());

        if (selectedAvatarUri != null) {
            // Không upload Storage nữa -> encode Base64 và lưu vào Realtime DB
            encodeAvatarToBase64AndSave(currentProfile);
        } else {
            persistProfile(currentProfile);
        }
    }

    private void encodeAvatarToBase64AndSave(@NonNull UserProfile profile) {
        try {
            Bitmap bitmap = decodeSampledBitmapFromUri(selectedAvatarUri, 512, 512);
            if (bitmap == null) {
                throw new IllegalStateException("bitmap null");
            }

            // Scale avatar về 256x256 cho nhẹ
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true);

            // Nén JPEG để giảm dung lượng
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] bytes = baos.toByteArray();

            String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

            profile.setAvatarBase64(b64);
            profile.setAvatarUrl(""); // không dùng Storage
            selectedAvatarUri = null;

            persistProfile(profile);

        } catch (Exception e) {
            Log.e(TAG, "encodeAvatarToBase64AndSave failed", e);
            Toast.makeText(this, R.string.profile_update_error, Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeSampledBitmapFromUri(@NonNull Uri uri, int reqWidth, int reqHeight) {
        try {
            // 1) đọc bounds
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, options);
            }

            // 2) tính sample size
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;

            // 3) decode thật
            try (InputStream is2 = getContentResolver().openInputStream(uri)) {
                if (is2 == null) return null;
                return BitmapFactory.decodeStream(is2, null, options);
            }
        } catch (Exception e) {
            Log.e(TAG, "decodeSampledBitmapFromUri failed", e);
            return null;
        }
    }

    private int calculateInSampleSize(@NonNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(inSampleSize, 1);
    }

    private void persistProfile(@NonNull UserProfile profile) {
        authManager.saveUserProfile(profile, new FirebaseAuthManager.CompletionCallback() {
            @Override
            public void onComplete() {
                Toast.makeText(UserProfileActivity.this, R.string.profile_update_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull String message) {
                Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== ĐỔI MẬT KHẨU (giữ như cũ) =====

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
            positiveButton.setOnClickListener(v -> attemptChangePassword(
                    dialog, positiveButton, progressBar, edtCurrentPassword, edtNewPassword, edtConfirmPassword
            ));
        });

        dialog.show();
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
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            positiveButton.setEnabled(true);
                            String message = e.getLocalizedMessage();
                            edtNewPassword.setError(!TextUtils.isEmpty(message)
                                    ? message
                                    : getString(R.string.profile_change_password_generic_error));
                            edtNewPassword.requestFocus();
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    positiveButton.setEnabled(true);
                    edtCurrentPassword.setError(getString(R.string.profile_change_password_error_invalid_current));
                    edtCurrentPassword.requestFocus();
                });
    }


}


