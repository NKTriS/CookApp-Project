package com.example.cookapp.api.dto;

/** DTO cho từng bước nấu trả về từ GET /api/recipes/:id/steps */
public class RecipeStepDto {
    public int id;
    public int recipe_id;
    public int step_number;
    public String title;
    public String instruction;
    public int timer_seconds;
    public int video_start_time;
}
