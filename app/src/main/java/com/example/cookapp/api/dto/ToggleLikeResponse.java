package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;

public class ToggleLikeResponse {
    @SerializedName("isLiked")
    private boolean isLiked;

    @SerializedName("likesCount")
    private int likesCount;

    public boolean isLiked() { return isLiked; }
    public int getLikesCount() { return likesCount; }
}
