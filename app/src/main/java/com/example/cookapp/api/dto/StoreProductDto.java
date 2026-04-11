package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;

public class StoreProductDto {
    @SerializedName("id")
    public int id;

    @SerializedName("ingredient_name")
    public String ingredientName;

    @SerializedName("product_name")
    public String productName;

    @SerializedName("unit")
    public String unit;

    @SerializedName("price_dong")
    public int priceDong;

    @SerializedName("store_name")
    public String storeName;

    @SerializedName("store_logo_url")
    public String storeLogoUrl;

    @SerializedName("image_url")
    public String imageUrl;

    @SerializedName("in_stock")
    public boolean inStock;

    @SerializedName("rating")
    public float rating;
}
