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
    private final float rating;
    private final int ratingCount;
    private final String description;
    private final @DrawableRes int imageResId;
    private final List<String> amenities;

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
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.description = description;
        this.imageResId = imageResId;
        this.amenities = new ArrayList<>(amenities);
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

    public float getRating() {
        return rating;
    }

    public int getRatingCount() {
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
}