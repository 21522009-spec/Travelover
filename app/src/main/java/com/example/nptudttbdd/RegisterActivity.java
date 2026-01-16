package com.example.nptudttbdd;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {
    private EditText etEmail, etPassword, etConfirmPassword, etName;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private RadioGroup radioRole;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.edtEmail);
        etPassword = findViewById(R.id.edtPassword);
        etConfirmPassword = findViewById(R.id.edtConfirmPassword);
        etName = findViewById(R.id.edtFullName);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        radioRole = findViewById(R.id.radioRole);
        authManager = new FirebaseAuthManager();

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        if (!AuthInputValidator.ensureRequired(etName, "Vui lòng nhập họ tên!")) {
            return;
        }
        if (!AuthInputValidator.ensureValidEmail(
                etEmail,
                "Vui lòng nhập email!",
                "Email không hợp lệ!")) {
            return;
        }
        if (!AuthInputValidator.ensureRequired(etPassword, "Vui lòng nhập mật khẩu!")) {
            return;
        }
        if (!AuthInputValidator.ensureRequired(etConfirmPassword, "Vui lòng xác nhận mật khẩu!")) {
            return;
        }

        String password = AuthInputValidator.getTrimmedText(etPassword);
        String confirm = AuthInputValidator.getTrimmedText(etConfirmPassword);
        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Mật khẩu không trùng khớp!");
            etConfirmPassword.requestFocus();
            return;
        }

        setProcessing(true);
        String email = AuthInputValidator.getTrimmedText(etEmail);
        String name = AuthInputValidator.getTrimmedText(etName);
        int selectedId = radioRole.getCheckedRadioButtonId();
        final String role = (selectedId == R.id.radioOwner) ? "owner" : "user";

        authManager.registerUser(name, email, password, new FirebaseAuthManager.RegisterCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser firebaseUser, @NonNull UserProfile profile) {
                    if ("owner".equals(role)) {
                    // Cập nhật vai trò và approved=true trên Firebase
                    authManager.getUserProfileReference(firebaseUser.getUid())
                            .child("role").setValue("owner");
                    authManager.getUserProfileReference(firebaseUser.getUid())
                            .child("approved").setValue(true);
                    // Cập nhật đối tượng profile trong ứng dụng
                    profile.setRole("owner");
                    profile.setApproved(true);
                    // Thêm tài khoản owner vào danh sách (locked = false)
                    TravelDataRepository.getInstance(getApplicationContext())
                            .addUser(new UserAccount(firebaseUser.getUid(), name, email, "", false));
                    Toast.makeText(RegisterActivity.this,
                            "Đăng ký thành công! Tài khoản owner đã được kích hoạt.",
                            Toast.LENGTH_SHORT).show();
                    } else {
                        // For regular users, mark as approved and add to local repository
                        authManager.getUserProfileReference(firebaseUser.getUid())
                                .child("approved").setValue(true);
                        TravelDataRepository.getInstance(getApplicationContext())
                                .addUser(new UserAccount(firebaseUser.getUid(),
                                        name, email, "", false));
                        Toast.makeText(RegisterActivity.this,
                                "Đăng ký thành công!",
                                Toast.LENGTH_SHORT).show();
                    }
                setProcessing(false);
                finish();
            }

            @Override
            public void onError(@NonNull String message) {
                setProcessing(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setProcessing(boolean processing) {
        btnRegister.setEnabled(!processing);
        if (processing) {
            btnRegister.setAlpha(0.6f);
        } else {
            btnRegister.setAlpha(1f);
        }
    }
}
