package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ingredients")
public class IngredientEntity {
    @PrimaryKey
    public int id;

    public String name;
    public String imageUrl;
    public String unit;      // e.g. "100g", "1 quả"
    public int    priceDong; // price in VND

    public String imageSource;
    public boolean imageVerified;
    public String normalizedName;
}
