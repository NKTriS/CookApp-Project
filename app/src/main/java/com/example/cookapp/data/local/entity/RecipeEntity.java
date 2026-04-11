package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipes")
public class RecipeEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String description;
    public String image_url;
    public int cook_time;
    public String difficulty;
    public int servings;
    public int calories;
    public int category_id;
    public int diet_type_id;
    public String video_url;  // nullable — null nếu không có video
    public String video_thumbnail_url;
    public String video_title;

    public String imageSource;
    public boolean imageVerified;
    public String normalizedName;

    // Filters (Phase 1)
    public boolean isVegetarian;
    public boolean isKeto;
    public boolean isLowCarb;
    public boolean isEatClean;

    public boolean isGlutenFree;
    public boolean isDairyFree;
    public boolean isSeafoodFree;
    public boolean isPeanutFree;
}
