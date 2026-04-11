package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;

public class ProfileStatsDto {
    @SerializedName("posts")
    private int posts;

    @SerializedName("reviews")
    private int reviews;

    @SerializedName("favorites")
    private int favorites;

    public int getPosts() { return posts; }
    public int getReviews() { return reviews; }
    public int getFavorites() { return favorites; }
}
