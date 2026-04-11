package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int user_id;
    public int recipe_id;
}
