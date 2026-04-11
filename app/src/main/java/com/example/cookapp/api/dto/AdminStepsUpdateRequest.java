package com.example.cookapp.api.dto;

import java.util.List;

public class AdminStepsUpdateRequest {
    public List<RecipeStepDto> steps;

    public AdminStepsUpdateRequest(List<RecipeStepDto> steps) {
        this.steps = steps;
    }
}
