package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shopping_lists")
public class ShoppingListEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int user_id;       // Owner of this list
    public long created_at;   // Timestamp
}
