package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipe_steps")
public class RecipeStepEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int recipe_id;
    public int step_number;
    public String title;
    public String instruction;
    public int timer_seconds;
    public int video_start_time;
}
