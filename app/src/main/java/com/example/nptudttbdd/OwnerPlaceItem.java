package com.example.nptudttbdd;

import androidx.annotation.Keep;

import java.util.List;

/**
 * Firebase model for node: Places/{placeId}
 */
@Keep
public class OwnerPlaceItem {
    public String id;
    public String name;
    public String location;
    public long pricePerNight;
    public float rating;
    public int ratingCount;
    public String description;
    public List<String> amenities;
    public String ownerId;

    // Image fields
    public String imageUrl;          // cover image
    public List<String> imageUrls;   // gallery

    public boolean active = true;

    public OwnerPlaceItem() {
        // required for Firebase
    }
}
