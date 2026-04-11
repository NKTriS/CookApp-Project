package com.example.cookapp.api.dto;

public class ToggleFavoriteRequest {
    public int recipe_id;
    public ToggleFavoriteRequest(int recipe_id) {
        this.recipe_id = recipe_id;
    }
}
