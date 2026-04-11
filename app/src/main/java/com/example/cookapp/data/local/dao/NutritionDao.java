package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cookapp.data.local.entity.NutritionFactEntity;

@Dao
public interface NutritionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNutritionFact(NutritionFactEntity fact);

    @Query("DELETE FROM nutrition_facts WHERE recipe_id = :recipeId")
    void deleteNutritionFactByRecipe(int recipeId);

    @Query("SELECT * FROM nutrition_facts WHERE recipe_id = :recipeId LIMIT 1")
    NutritionFactEntity getNutritionByRecipe(int recipeId);

    @Query("DELETE FROM nutrition_facts")
    void deleteAll();
}
