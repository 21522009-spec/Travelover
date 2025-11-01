package com.example.nptudttbdd;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etOtp;
    private Button btnReset;
    private ProgressBar progressBar;
    private TextView tvOtpInfo;
    private TextView tvResendOtp;
    private TextView tvBackToLogin;
    private FirebaseAuthManager authManager;
    private boolean otpEmailConfigured;

    private Stage currentStage = Stage.REQUEST_OTP;
    private String currentEmail = "";

    private enum Stage {
        REQUEST_OTP,
        VERIFY_OTP
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.edtEmailForgot);
        etOtp = findViewById(R.id.edtOtp);
        btnReset = findViewById(R.id.btnSendReset);
        progressBar = findViewById(R.id.progressBar);
        tvOtpInfo = findViewById(R.id.tvOtpInfo);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        authManager = new FirebaseAuthManager();
        otpEmailConfigured = authManager.isOtpEmailConfigured();

        if (otpEmailConfigured) {
            btnReset.setText(R.string.forgot_password_send_otp_button);
        } else {
            btnReset.setText(R.string.forgot_password_send_reset_link_button);
            etOtp.setVisibility(View.GONE);
            tvOtpInfo.setVisibility(View.GONE);
            tvResendOtp.setVisibility(View.GONE);
        }
        btnReset.setOnClickListener(v -> {
            if (currentStage == Stage.REQUEST_OTP) {
                requestOtp();
            } else {
                verifyOtp();
            }
        });
        tvBackToLogin.setOnClickListener(v -> finish());
        tvResendOtp.setOnClickListener(v -> {
            if (currentStage == Stage.VERIFY_OTP && !progressBar.isShown()) {
                requestOtp();
            }
        });
    }

    private void requestOtp() {
        if (!AuthInputValidator.ensureValidEmail(
                etEmail,
                "Vui lòng nhập email!",
                "Email không hợp lệ!")) {
            return;
        }

        setLoading(true);
        currentEmail = AuthInputValidator.getTrimmedText(etEmail);

        if (!otpEmailConfigured) {
            authManager.sendPasswordReset(currentEmail, new FirebaseAuthManager.CompletionCallback() {
                @Override
                public void onComplete() {
                    setLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this,
                            R.string.forgot_password_reset_email_sent,
                            Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(@NonNull String message) {
                    setLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        authManager.requestPasswordResetOtp(currentEmail, new FirebaseAuthManager.OtpRequestCallback() {
            @Override
            public void onOtpSent() {
                setLoading(false);
                currentStage = Stage.VERIFY_OTP;
                showOtpControls(true);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Đã gửi mã OTP. Vui lòng kiểm tra hộp thư của bạn.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp() {
        if (!AuthInputValidator.ensureRequired(etOtp, "Vui lòng nhập mã OTP!")) {
            return;
        }

        String otp = AuthInputValidator.getTrimmedText(etOtp);
        String email = currentEmail.isEmpty() ? AuthInputValidator.getTrimmedText(etEmail) : currentEmail;
        if (email.isEmpty()) {
            Toast.makeText(this, "Email không hợp lệ. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        authManager.verifyOtpAndSendResetEmail(email, otp, new FirebaseAuthManager.CompletionCallback() {
            @Override
            public void onComplete() {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this,
                        R.string.forgot_password_reset_email_sent,
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

    private void showOtpControls(boolean visible) {
        etOtp.setVisibility(visible ? View.VISIBLE : View.GONE);
        tvOtpInfo.setVisibility(visible ? View.VISIBLE : View.GONE);
        tvResendOtp.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            btnReset.setText(R.string.forgot_password_verify_otp_button);
            tvOtpInfo.setText(getString(R.string.forgot_password_otp_sent, currentEmail));
            etOtp.setText("");
            etEmail.setEnabled(false);
        } else {
            btnReset.setText(R.string.forgot_password_send_otp_button);
            etEmail.setEnabled(true);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnReset.setEnabled(!loading);
        etEmail.setEnabled(!loading && currentStage == Stage.REQUEST_OTP);
        etOtp.setEnabled(!loading);
        tvResendOtp.setEnabled(!loading);
        tvResendOtp.setAlpha(loading ? 0.5f : 1f);
    }
}