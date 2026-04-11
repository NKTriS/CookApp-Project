package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shopping_list_items")
public class ShoppingListItemEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int shopping_list_id;
    public String ingredient_name;
    public float quantity;
    public String unit;
    public boolean checked;
}
