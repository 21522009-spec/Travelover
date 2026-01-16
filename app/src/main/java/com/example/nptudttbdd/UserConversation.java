package com.example.nptudttbdd;

import androidx.annotation.NonNull;

/**
 * Conversation model for USER portal (chat with owner).
 */
public class UserConversation {

    private final String id;
    private final String title;
    private String lastMessage;
    private String lastTime;
    private boolean hasNewMessage;

    public UserConversation(@NonNull String id,
                            @NonNull String title,
                            @NonNull String lastMessage,
                            @NonNull String lastTime,
                            boolean hasNewMessage) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.lastTime = lastTime;
        this.hasNewMessage = hasNewMessage;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(@NonNull String lastMessage) {
        this.lastMessage = lastMessage;
    }

    @NonNull
    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(@NonNull String lastTime) {
        this.lastTime = lastTime;
    }

    public boolean hasNewMessage() {
        return hasNewMessage;
    }

    public void setHasNewMessage(boolean hasNewMessage) {
        this.hasNewMessage = hasNewMessage;
    }
}
