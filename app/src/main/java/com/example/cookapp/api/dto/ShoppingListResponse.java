package com.example.cookapp.api.dto;

import java.util.List;

/**
 * Response từ GET /api/shopping-list
 * Dùng ShoppingListItemDto (thuần DTO) thay vì Room entity.
 */
public class ShoppingListResponse {
    public int id;
    public int user_id;
    public List<ShoppingListItemDto> items;
}
