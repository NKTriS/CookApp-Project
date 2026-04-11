package com.example.cookapp.api.dto;

/** DTO cho nguyên liệu trả về từ GET /api/recipes/:id/ingredients */
public class RecipeIngredientDto {
    public int id;
    public String name;
    public RecipeIngredientThrough RecipeIngredient; // Sequelize through table data

    public static class RecipeIngredientThrough {
        public float quantity;
        public String unit;
    }

    // Convenience helpers
    public float getQuantity() {
        return RecipeIngredient != null ? RecipeIngredient.quantity : 0;
    }

    public String getUnit() {
        return RecipeIngredient != null ? RecipeIngredient.unit : "";
    }
}
