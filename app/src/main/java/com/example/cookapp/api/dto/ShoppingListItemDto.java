package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO thuần Java cho ShoppingListItem từ API
 * Tách biệt với Room entity ShoppingListItemEntity để tránh Room annotation conflict.
 */
public class ShoppingListItemDto {
    public int id;

    @SerializedName("shopping_list_id")
    public int shopping_list_id;

    @SerializedName("ingredient_name")
    public String ingredient_name;

    public float quantity;
    public String unit;
    public boolean checked;
}
