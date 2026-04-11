package com.example.cookapp;

import com.google.gson.annotations.SerializedName;

public class Review {
    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("recipe_id")
    private int recipeId;

    @SerializedName("author")
    private String authorName;

    private int rating;
    private String date;

    @SerializedName("comment")
    private String content;

    @SerializedName("created_at")
    private String createdAt;

    private boolean isCurrentUser;

    // Nested Recipe (from admin API include)
    @SerializedName("Recipe")
    private RecipeRef recipe;

    public static class RecipeRef {
        public int id;
        public String title;
    }

    public int getRecipeId() { return recipeId; }
    public String getRecipeName() {
        return recipe != null ? recipe.title : null;
    }


    // No-arg constructor for Gson
    public Review() {}

    public Review(String id, String authorName, int rating, String date, String content) {
        this.id = id;
        this.authorName = authorName;
        this.rating = rating;
        this.date = date;
        this.content = content;
        this.isCurrentUser = false;
    }

    public Review(String id, String authorName, int rating, String date, String content, boolean isCurrentUser) {
        this.id = id;
        this.authorName = authorName;
        this.rating = rating;
        this.date = date;
        this.content = content;
        this.isCurrentUser = isCurrentUser;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public boolean isCurrentUser() { return isCurrentUser; }
    public void setCurrentUser(boolean currentUser) { this.isCurrentUser = currentUser; }
}
