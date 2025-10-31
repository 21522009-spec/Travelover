package com.example.nptudttbdd;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnReset;
    private ProgressBar progressBar;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.edtEmailForgot);
        btnReset = findViewById(R.id.btnSendReset);
        progressBar = findViewById(R.id.progressBar);
        authManager = new FirebaseAuthManager();

        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        if (!AuthInputValidator.ensureValidEmail(
                etEmail,
                "Vui lòng nhập email!",
                "Email không hợp lệ!")) {
            return;
        }

        setLoading(true);

        String email = AuthInputValidator.getTrimmedText(etEmail);

        authManager.sendPasswordReset(email, new FirebaseAuthManager.CompletionCallback() {
            @Override
            public void onComplete() {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Đã gửi email đặt lại mật khẩu! Vui lòng kiểm tra hộp thư.",
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnReset.setEnabled(!loading);
    }
}