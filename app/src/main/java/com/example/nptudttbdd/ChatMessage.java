package com.example.nptudttbdd;

import androidx.annotation.NonNull;

public class ChatMessage {

    public enum Sender {
        USER,
        ADMIN,
        OWNER,
        GUEST
    }

    private final String id;
    private final Sender sender;
    private final String message;
    private final long timestamp;

    public ChatMessage(@NonNull String id,
                       @NonNull Sender sender,
                       @NonNull String message) {
        this(id, sender, message, System.currentTimeMillis());
    }

    public ChatMessage(@NonNull String id,
                       @NonNull Sender sender,
                       @NonNull String message,
                       long timestamp) {
        this.id = id;
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public Sender getSender() {
        return sender;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}