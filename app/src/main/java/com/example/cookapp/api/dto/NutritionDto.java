package com.example.cookapp.api.dto;

/** DTO cho thông tin dinh dưỡng từ GET /api/recipes/:id/nutrition */
public class NutritionDto {
    public int id;
    public int recipe_id;
    public float calories;
    public float protein;
    public float fat;
    public float carbs;
    public float fiber;
    public float sugar;
    public float sodium;
}
