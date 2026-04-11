package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminRecipeCreateDto {
    public String title;
    public String description;
    public String imageUrl;
    public int cookTime;
    public String difficulty;
    public int servings;
    public int calories;
    
    public List<Integer> categoryIds;
    public List<Integer> dietTypeIds;
    
    public NutritionFactsDto nutritionFacts;
    public List<StepDto> steps;
    public List<IngredientDto> ingredients;

    public static class NutritionFactsDto {
        public int calories;
        public int protein;
        public int fat;
        public int carbs;
        public float fiber;
        public float sugar;
        public int sodium;
    }

    public static class StepDto {
        public String title;
        public String instruction;
        public int timerSeconds;
        public int videoStartTime;
    }

    public static class IngredientDto {
        public int ingredientId;
        public String ingredientName;
        public int quantity;
        public String unit;
    }
}
