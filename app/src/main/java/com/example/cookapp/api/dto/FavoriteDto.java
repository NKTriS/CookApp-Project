package com.example.cookapp.api.dto;

import com.example.cookapp.Recipe;
import com.google.gson.annotations.SerializedName;

/**
 * Favorite item trả về từ GET /api/favorites (có kèm Recipe)
 */
public class FavoriteDto {
    public int id;
    public int user_id;
    public int recipe_id;

    @SerializedName("Recipe")
    public RecipeSummary recipe;

    public static class RecipeSummary {
        public int id;
        public String title;
        public String image_url;
        public int cook_time;
        public String difficulty;
        public int servings;
        public String calories;
        public String video_url;
    }
}
