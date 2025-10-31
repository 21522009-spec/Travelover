package com.example.nptudttbdd;

import androidx.annotation.NonNull;

public class UserAccount {
    private final String id;
    private final String name;
    private final String email;
    private final String phone;
    private boolean locked;

    public UserAccount(@NonNull String id,
                       @NonNull String name,
                       @NonNull String email,
                       @NonNull String phone,
                       boolean locked) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.locked = locked;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public String getPhone() {
        return phone;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}