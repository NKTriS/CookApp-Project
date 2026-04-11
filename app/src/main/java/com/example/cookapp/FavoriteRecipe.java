package com.example.cookapp;

public class FavoriteRecipe {
    private String id;
    private String name;
    private int imgResId;
    private String imageUrl;  // URL ảnh từ API (ưu tiên hơn imgResId)

    public FavoriteRecipe(String id, String name, int imgResId) {
        this.id = id;
        this.name = name;
        this.imgResId = imgResId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    /** Alias getTitle() cho tương thích với code mới */
    public String getTitle() { return name; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getImgResId() { return imgResId; }
    public void setImgResId(int imgResId) { this.imgResId = imgResId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
