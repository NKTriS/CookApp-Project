package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipe_ingredients", primaryKeys = {"recipe_id", "ingredient_id"})
public class RecipeIngredientEntity {
    public int recipe_id;
    public int ingredient_id;
    public float quantity;
    public String unit;
}
