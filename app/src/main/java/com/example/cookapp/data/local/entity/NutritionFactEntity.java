package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "nutrition_facts")
public class NutritionFactEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int recipe_id;
    public int calories;
    public float protein;
    public float fat;
    public float carbs;
    public float fiber;
    public float sugar;
    public float sodium;
}
