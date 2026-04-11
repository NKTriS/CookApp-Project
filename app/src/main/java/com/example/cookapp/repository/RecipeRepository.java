package com.example.cookapp.repository;

import android.content.Context;
import android.util.Log;

import com.example.cookapp.Recipe;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.data.local.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeRepository {

    private final ApiService apiService;
    private final AppDatabase localDb;

    public RecipeRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(ApiService.class);
        localDb = AppDatabase.getDatabase(context);
    }

    public interface RecipeListCallback {
        void onResult(Resource<List<Recipe>> result);
    }
    
    public interface RecipeDetailCallback {
        void onResult(Resource<Recipe> result);
    }

    public void getAllRecipes(RecipeListCallback callback) {
        callback.onResult(Resource.loading(null));

        apiService.getAllRecipes().enqueue(new Callback<List<Recipe>>() {
            @Override
            public void onResponse(Call<List<Recipe>> call, Response<List<Recipe>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Recipe> remoteRecipes = response.body();
                    // Sync to Local Room DB
                    Executors.newSingleThreadExecutor().execute(() -> {
                        List<com.example.cookapp.data.local.entity.RecipeEntity> entities = new ArrayList<>();
                        for (Recipe r : remoteRecipes) {
                            com.example.cookapp.data.local.entity.RecipeEntity e = new com.example.cookapp.data.local.entity.RecipeEntity();
                            e.id = r.getId();
                            e.title = r.getTitle();
                            e.description = r.getDescription();
                            try { e.cook_time = Integer.parseInt(r.getTime()); } catch (Exception ignored) {}
                            e.difficulty = r.getDifficulty();
                            e.servings = r.getServings();
                            try { e.calories = Integer.parseInt(r.getCalories()); } catch (Exception ignored) {}
                            e.image_url = r.getImageUrl();
                            e.video_url = r.getVideoUrl();
                            e.video_thumbnail_url = r.getVideoThumbnailUrl();
                            entities.add(e);
                        }
                        localDb.recipeDao().insertRecipes(entities);
                        Log.d("SYNC", "Synced " + entities.size() + " recipes to Room.");
                    });
                    callback.onResult(Resource.success(remoteRecipes));
                } else {
                    fallbackToLocal(callback, null);
                }
            }

            @Override
            public void onFailure(Call<List<Recipe>> call, Throwable t) {
                Log.e("API_ERR", "Fetch recipes failed. Fallback to Local", t);
                fallbackToLocal(callback, t);
            }
        });
    }

    private void fallbackToLocal(RecipeListCallback callback, Throwable t) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<com.example.cookapp.data.local.entity.RecipeEntity> entities = localDb.recipeDao().getAllRecipes();
            List<Recipe> mappedList = new ArrayList<>();
            for (com.example.cookapp.data.local.entity.RecipeEntity e : entities) {
                Recipe r = new Recipe(
                        String.valueOf(e.id),
                        e.title,
                        e.description,
                        String.valueOf(e.cook_time),
                        e.difficulty,
                        e.servings,
                        String.valueOf(e.calories),
                        e.image_url);
                r.setVideoUrl(e.video_url);
                r.setVideoThumbnailUrl(e.video_thumbnail_url);
                mappedList.add(r);
            }
            if (!mappedList.isEmpty()) {
                callback.onResult(Resource.success(mappedList));
            } else {
                callback.onResult(Resource.error("Lỗi: " + (t != null ? t.getMessage() : "Offline"), null));
            }
        });
    }

    public void getRecipesByCategory(int categoryId, RecipeListCallback callback) {
        callback.onResult(Resource.loading(null));

        apiService.getRecipesByCategory(categoryId).enqueue(new Callback<List<Recipe>>() {
            @Override
            public void onResponse(Call<List<Recipe>> call, Response<List<Recipe>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Recipe> remoteRecipes = response.body();
                    // Sync to Local Room DB (Category specific)
                    Executors.newSingleThreadExecutor().execute(() -> {
                        List<com.example.cookapp.data.local.entity.RecipeEntity> entities = new ArrayList<>();
                        for (Recipe r : remoteRecipes) {
                            com.example.cookapp.data.local.entity.RecipeEntity e = new com.example.cookapp.data.local.entity.RecipeEntity();
                            e.id = r.getId();
                            e.title = r.getTitle();
                            e.description = r.getDescription();
                            try { e.cook_time = Integer.parseInt(r.getTime()); } catch (Exception ignored) {}
                            e.difficulty = r.getDifficulty();
                            e.servings = r.getServings();
                            try { e.calories = Integer.parseInt(r.getCalories()); } catch (Exception ignored) {}
                            e.image_url = r.getImageUrl();
                            e.video_url = r.getVideoUrl();
                            e.video_thumbnail_url = r.getVideoThumbnailUrl();
                            // Note: we can't easily set primary category_id here without more info, 
                            // but Room will keep existing category mapping if we don't overwrite it.
                            entities.add(e);
                        }
                        localDb.recipeDao().insertRecipes(entities);
                    });
                    callback.onResult(Resource.success(remoteRecipes));
                } else {
                    fallbackToLocalByCategory(categoryId, callback, null);
                }
            }

            @Override
            public void onFailure(Call<List<Recipe>> call, Throwable t) {
                fallbackToLocalByCategory(categoryId, callback, t);
            }
        });
    }

    private void fallbackToLocalByCategory(int categoryId, RecipeListCallback callback, Throwable t) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<com.example.cookapp.data.local.entity.RecipeEntity> entities = localDb.recipeDao().getRecipesByCategory(categoryId);
            List<Recipe> mappedList = new ArrayList<>();
            for (com.example.cookapp.data.local.entity.RecipeEntity e : entities) {
                Recipe r = new Recipe(
                        String.valueOf(e.id),
                        e.title,
                        e.description,
                        String.valueOf(e.cook_time),
                        e.difficulty,
                        e.servings,
                        String.valueOf(e.calories),
                        e.image_url);
                r.setVideoUrl(e.video_url);
                r.setVideoThumbnailUrl(e.video_thumbnail_url);
                mappedList.add(r);
            }
            if (!mappedList.isEmpty()) {
                callback.onResult(Resource.success(mappedList));
            } else {
                callback.onResult(Resource.error("Lỗi: " + (t != null ? t.getMessage() : "Offline"), null));
            }
        });
    }

    public void getRecipeDetail(int id, RecipeDetailCallback callback) {
        callback.onResult(Resource.loading(null));
        apiService.getRecipeDetail(id).enqueue(new Callback<Recipe>() {
            @Override
            public void onResponse(Call<Recipe> call, Response<Recipe> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi lấy chi tiết công thức", null));
                }
            }

            @Override
            public void onFailure(Call<Recipe> call, Throwable t) {
                callback.onResult(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
    }
}
