package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diet_types")
public class DietTypeEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
}
