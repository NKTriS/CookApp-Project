package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cookapp.data.local.entity.IngredientEntity;
import com.example.cookapp.data.local.entity.RecipeEntity;
import com.example.cookapp.data.local.entity.RecipeIngredientEntity;
import com.example.cookapp.data.local.entity.RecipeStepEntity;

import java.util.List;

@Dao
public interface RecipeDao {

    @Query("SELECT * FROM recipes")
    List<RecipeEntity> getAllRecipes();

    @Query("SELECT * FROM categories ORDER BY id ASC")
    List<com.example.cookapp.data.local.entity.CategoryEntity> getAllCategories();

    @Query("SELECT * FROM ingredients")
    List<IngredientEntity> getAllIngredients();

    @Query("SELECT * FROM ingredients WHERE id = :ingredientId LIMIT 1")
    IngredientEntity getIngredientById(int ingredientId);

    @Query("SELECT DISTINCT r.* FROM recipes r " +
           "LEFT JOIN recipe_ingredients ri ON r.id = ri.recipe_id " +
           "LEFT JOIN ingredients i ON ri.ingredient_id = i.id " +
           "WHERE r.title LIKE :q OR i.name LIKE :q " +
           "ORDER BY r.id ASC")
    List<RecipeEntity> searchRecipes(String q);

    @Query("SELECT * FROM recipes WHERE cook_time <= :maxMinutes ORDER BY cook_time ASC")
    List<RecipeEntity> getRecipesByMaxTime(int maxMinutes);

    @Query("SELECT * FROM recipes WHERE difficulty = :difficulty ORDER BY id ASC")
    List<RecipeEntity> getRecipesByDifficulty(String difficulty);

    @Query("SELECT * FROM recipes WHERE diet_type_id = :dietTypeId ORDER BY id ASC")
    List<RecipeEntity> getRecipesByDiet(int dietTypeId);

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    RecipeEntity getRecipeById(int id);

    @Query("SELECT * FROM recipe_steps WHERE recipe_id = :recipeId ORDER BY step_number ASC")
    List<RecipeStepEntity> getStepsByRecipeId(int recipeId);

    @Query("SELECT * FROM recipe_ingredients WHERE recipe_id = :recipeId GROUP BY ingredient_id")
    List<RecipeIngredientEntity> getIngredientsByRecipeId(int recipeId);

    @Query("SELECT * FROM recipes WHERE category_id = :categoryId")
    List<RecipeEntity> getRecipesByCategory(int categoryId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipes(List<RecipeEntity> recipes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipe(RecipeEntity recipe);

    @Query("DELETE FROM recipe_steps WHERE recipe_id = :recipeId")
    void deleteStepsByRecipe(int recipeId);

    @Query("DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId")
    void deleteIngredientsByRecipe(int recipeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStep(RecipeStepEntity step);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSteps(List<RecipeStepEntity> steps);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategory(com.example.cookapp.data.local.entity.CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertIngredient(IngredientEntity ingredient);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertIngredients(List<IngredientEntity> ingredients);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipeIngredient(RecipeIngredientEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipeIngredients(List<RecipeIngredientEntity> items);

    @Query("DELETE FROM ingredients")
    void deleteAllIngredients();



    @Query("DELETE FROM recipes")
    void deleteAllRecipes();

    @Query("DELETE FROM recipe_steps")
    void deleteAllSteps();

    @Query("DELETE FROM recipe_ingredients")
    void deleteAllRecipeIngredients();

    @Query("SELECT COUNT(*) FROM recipes")
    int countRecipes();
}
