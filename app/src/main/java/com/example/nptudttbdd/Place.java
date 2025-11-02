package com.example.nptudttbdd;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Place implements Serializable {

    private final String id;
    private final String name;
    private final String location;
    private final long pricePerNight;
    private float rating;
    private int ratingCount;
    private float ratingTotal;
    private final String description;
    private final @DrawableRes int imageResId;
    private final List<String> amenities;
    private final List<PlaceReview> reviews = new ArrayList<>();

    public Place(@NonNull String id,
                 @NonNull String name,
                 @NonNull String location,
                 long pricePerNight,
                 float rating,
                 int ratingCount,
                 @NonNull String description,
                 @DrawableRes int imageResId,
                 @NonNull List<String> amenities) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.pricePerNight = pricePerNight;
        this.rating = clampRating(rating);
        this.ratingCount = Math.max(0, ratingCount);
        this.ratingTotal = this.rating * this.ratingCount;
        this.description = description;
        this.imageResId = imageResId;
        this.amenities = new ArrayList<>(amenities);
    }

    private static float clampRating(float rating) {
        return Math.max(0f, Math.min(5f, rating));
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
    public String getLocation() {
        return location;
    }

    public long getPricePerNight() {
        return pricePerNight;
    }

    public synchronized float getRating() {
        return rating;
    }

    public synchronized int getRatingCount() {
        return ratingCount;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @DrawableRes
    public int getImageResId() {
        return imageResId;
    }

    @NonNull
    public List<String> getAmenities() {
        return new ArrayList<>(amenities);
    }

    public synchronized void addReview(@NonNull PlaceReview review) {
        reviews.add(review);
        ratingTotal += clampRating(review.getRating());
        ratingCount += 1;
        if (ratingCount == 0) {
            rating = 0f;
        } else {
            rating = ratingTotal / ratingCount;
        }
    }

    @NonNull
    public synchronized List<PlaceReview> getReviews() {
        return new ArrayList<>(reviews);
    }
}