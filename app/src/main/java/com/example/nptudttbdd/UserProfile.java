package com.example.nptudttbdd;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UserProfile {

    private String uid;
    private String name;
    private String email;
    private String role;
    private String avatarUrl;

    // Bắt buộc cần constructor rỗng cho Firebase
    public UserProfile() {
    }

    public UserProfile(@NonNull String uid,
                       @NonNull String name,
                       @NonNull String email,
                       @Nullable String role,
                       @Nullable String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

    @NonNull
    public String getUid() {
        return uid == null ? "" : uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @NonNull
    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @NonNull
    public String getRole() {
        return role == null ? "" : role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @NonNull
    public String getAvatarUrl() {
        return avatarUrl == null ? "" : avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}