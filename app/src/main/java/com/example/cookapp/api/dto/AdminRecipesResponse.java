package com.example.cookapp.api.dto;

import com.example.cookapp.Recipe;
import java.util.List;

public class AdminRecipesResponse {
    public int total;
    public int page;
    public int totalPages;
    public List<Recipe> recipes;
}
