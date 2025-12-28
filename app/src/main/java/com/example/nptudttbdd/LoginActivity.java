package com.example.nptudttbdd;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String OWNER_USERNAME = "owner";
    private static final String OWNER_PASSWORD = "owner";
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView btnRegister, btnForgot;

    private ProgressBar progressBar;

    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new FirebaseAuthManager();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgot = findViewById(R.id.btnForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        if (authManager.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
        btnForgot.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
        );
    }

    private void loginUser() {
        String emailInput = AuthInputValidator.getTrimmedText(etEmail);
        String passwordInput = AuthInputValidator.getTrimmedText(etPassword);

        if (isAdminCredentials(emailInput, passwordInput)) {
            openAdminDashboard();
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

        setLoading(true);

        authManager.loginUser(emailInput, passwordInput, new FirebaseAuthManager.LoginCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser firebaseUser, UserProfile profile) {
                setLoading(false);
                // Kiểm tra trạng thái phê duyệt của tài khoản
                if (profile != null && !profile.isApproved()) {
                    String msg;
                    if ("owner".equals(profile.getRole())) {
                        msg = "Tài khoản của bạn chưa được quản trị viên phê duyệt.";
                    } else {
                        msg = "Tài khoản của bạn đã bị khóa.";
                    }
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                    authManager.logout();  // Đăng xuất tài khoản chưa được phép
                    return;
                }
                // Chuyển hướng theo vai trò tài khoản sau khi đăng nhập thành công
                if (profile != null && "owner".equals(profile.getRole())) {
                    String name = profile.getName();
                    String welcome = (name == null || name.isEmpty())
                            ? "Đăng nhập thành công!"
                            : "Chào mừng " + name + " trở lại!";
                    Toast.makeText(LoginActivity.this, welcome, Toast.LENGTH_SHORT).show();
                    // Mở giao diện Owner (OwnerDashboardActivity) và truyền tên Owner
                    Intent ownerIntent = new Intent(LoginActivity.this, OwnerDashboardActivity.class);
                    ownerIntent.putExtra("ownerName", profile.getName());
                    startActivity(ownerIntent);
                    finish();
                    return;
                }
                // Mặc định: tài khoản user (khách)
                String name = profile != null ? profile.getName() : null;
                String message = (name == null || name.isEmpty())
                        ? "Đăng nhập thành công!"
                        : "Chào mừng " + name + " trở lại!";
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        btnForgot.setEnabled(!loading);
    }

    private boolean isAdminCredentials(@NonNull String username, @NonNull String password) {
        return ADMIN_USERNAME.equalsIgnoreCase(username)
                && ADMIN_PASSWORD.equals(password);
    }

    private boolean isOwnerCredentials(@NonNull String username, @NonNull String password) {
        return OWNER_USERNAME.equalsIgnoreCase(username)
                && OWNER_PASSWORD.equals(password);
    }

    private void openAdminDashboard() {
        Toast.makeText(this, R.string.admin_login_success, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
        finish();
    }
}


