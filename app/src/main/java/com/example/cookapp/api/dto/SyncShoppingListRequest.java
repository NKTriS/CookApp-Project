package com.example.cookapp.api.dto;

import java.util.List;

public class SyncShoppingListRequest {

    public List<ShoppingItem> items;

    public SyncShoppingListRequest(List<ShoppingItem> items) {
        this.items = items;
    }

    /** Lightweight item DTO (no Room annotations). */
    public static class ShoppingItem {
        public String ingredient_name;
        public float  quantity;
        public String unit;
        public boolean checked;
        public int price;

        public ShoppingItem(String ingredient_name, float quantity, String unit, boolean checked, int price) {
            this.ingredient_name = ingredient_name;
            this.quantity = quantity;
            this.unit = unit;
            this.checked = checked;
            this.price = price;
        }
    }
}
