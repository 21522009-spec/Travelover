package com.example.nptudttbdd;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PasswordResetOtp {

    private String otpHash;
    private long expiresAt;
    private boolean used;

    public PasswordResetOtp() {
    }

    public PasswordResetOtp(@NonNull String otpHash, long expiresAt, boolean used) {
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    @NonNull
    public String getOtpHash() {
        return otpHash == null ? "" : otpHash;
    }

    public void setOtpHash(@Nullable String otpHash) {
        this.otpHash = otpHash;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}