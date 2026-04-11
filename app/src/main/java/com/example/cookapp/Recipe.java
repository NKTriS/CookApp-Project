package com.example.cookapp;

import com.google.gson.annotations.SerializedName;

/**
 * Model công thức nấu ăn — ánh xạ từ API response (Gson)
 * và cũng dùng trực tiếp trong RecyclerView adapter.
 */
public class Recipe {
    // ── Core fields (API + adapter) ───────────────────────────────────────
    private int id;
    private String title;

    @SerializedName("calories")
    private String calories;

    @SerializedName("cook_time")
    private String time;

    @SerializedName("image_url")
    private String imageUrl;

    // Category đơn (backward compat — primary category)
    @SerializedName("Category")
    private CategoryInfo category;

    // Mảng nhiều danh mục (M-N) từ API
    @SerializedName("categories")
    private java.util.List<CategoryInfo> categories;

    public static class CategoryInfo {
        @SerializedName("name")
        public String name;
        @SerializedName("id")
        public int id;
    }

    // ── Detail fields ─────────────────────────────────────────────────────
    private String description;
    private String difficulty;
    private int servings;

    @SerializedName("video_url")
    private String videoUrl;

    @SerializedName("video_thumbnail_url")
    private String videoThumbnailUrl;

    @SerializedName("matchPercentage")
    private Integer matchPercentage; // Dữ liệu AI-2 (tùy chọn)

    // ── Nạp trực tiếp Mảng DietTypes (Tags) từ BE ────────────────────────────────
    @SerializedName("dietTypes")
    private java.util.List<DietTypeInfo> dietTypes;

    public static class DietTypeInfo {
        @SerializedName("name")
        public String name;
        @SerializedName("id")
        public int id;
    }

    public boolean hasTag(int tagId) {
        if (dietTypes == null || dietTypes.isEmpty()) return false;
        for (DietTypeInfo d : dietTypes) {
            if (d.id == tagId) return true;
        }
        return false;
    }

    // Các ID tag cứng từ Backend Seed
    // 1: Ăn chay, 2: Keto, 3: Low-carb, 4: Eat-clean, 5: Không Gluten, 6: Không Bơ Sữa, 7: Không Hải Sản, 8: Không Đậu Phộng
    public boolean isVegetarian()  { return hasTag(1); }
    public boolean isKeto()        { return hasTag(2); }
    public boolean isLowCarb()     { return hasTag(3); }
    public boolean isEatClean()    { return hasTag(4); }
    public boolean isGlutenFree()  { return hasTag(5); }
    public boolean isDairyFree()   { return hasTag(6); }
    public boolean isSeafoodFree() { return hasTag(7); }
    public boolean isPeanutFree()  { return hasTag(8); }

    // ── Nạp trực tiếp Ingredients từ BE ────────────────────────────────
    @SerializedName("ingredients")
    private java.util.List<IngredientInfo> ingredients;

    public static class IngredientInfo {
        @SerializedName("name")
        public String name;
        @SerializedName("RecipeIngredient")
        public RecipeIngredientInfo pivot; // Dữ liệu của bảng trung gian
    }

    public static class RecipeIngredientInfo {
        @SerializedName("quantity")
        public String quantity;
        @SerializedName("unit")
        public String unit;
    }

    // ── No-arg constructor for Gson ───────────────────────────────────────
    public Recipe() {}

    /** Constructor dùng trong RecipeAdapter / RecipeListActivity */
    public Recipe(int id, String title, String calories, String time, String imageUrl) {
        this.id       = id;
        this.title    = title;
        this.calories = calories;
        this.time     = time;
        this.imageUrl = imageUrl;
    }

    /** Constructor dùng trong RecipeRepository fallback (từ Room entities) */
    public Recipe(String idStr, String title, String description, String cookTime,
                  String difficulty, int servings, String calories, String imageUrl) {
        try { this.id = Integer.parseInt(idStr); } catch (NumberFormatException ignored) {}
        this.title       = title;
        this.description = description;
        this.time        = cookTime;
        this.difficulty  = difficulty;
        this.servings    = servings;
        this.calories    = calories;
        this.imageUrl    = imageUrl;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCalories() { return calories; }
    public void setCalories(String calories) { this.calories = calories; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // getLikes() / getComments() removed — không dùng trong RecipeAdapter

    public String getCategoryName() {
        // Ưu tiên mảng categories (M-N), fallback về primary Category
        if (categories != null && !categories.isEmpty()) {
            return categories.get(0).name;
        }
        return category != null ? category.name : null;
    }

    /** Trả về tất cả danh mục cách nhau bằng " · " để hiển thị lên card */
    public String getCategoriesString() {
        if (categories != null && !categories.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < categories.size(); i++) {
                if (i > 0) sb.append(" · ");
                sb.append(categories.get(i).name);
            }
            return sb.toString();
        }
        return getCategoryName();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getVideoThumbnailUrl() { return videoThumbnailUrl; }
    public void setVideoThumbnailUrl(String v) { this.videoThumbnailUrl = v; }

    public java.util.List<IngredientInfo> getIngredients() { return ingredients; }

    public Integer getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(Integer matchPercentage) { this.matchPercentage = matchPercentage; }

}
