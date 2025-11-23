package com.example.nptudttbdd;

import androidx.annotation.NonNull;

public class PlaceReview {

    private final String id;
    private final String placeId;
    private final float rating;
    private final String content;
    private final long createdAt;

    public PlaceReview(@NonNull String id,
                       @NonNull String placeId,
                       float rating,
                       @NonNull String content,
                       long createdAt) {
        this.id = id;
        this.placeId = placeId;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getPlaceId() {
        return placeId;
    }

    public float getRating() {
        return rating;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}