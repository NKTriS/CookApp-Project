package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cookapp.data.local.entity.ShoppingListEntity;
import com.example.cookapp.data.local.entity.ShoppingListItemEntity;

import java.util.List;

@Dao
public interface ShoppingListDao {

    // ── List management ──────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertList(ShoppingListEntity list);

    /** @deprecated Use getLatestListByUser(userId) for user-scoped access */
    @Deprecated
    @Query("SELECT * FROM shopping_lists ORDER BY id DESC LIMIT 1")
    ShoppingListEntity getLatestList();

    /** Get the most recent shopping list that belongs to a specific user */
    @Query("SELECT * FROM shopping_lists WHERE user_id = :userId ORDER BY id DESC LIMIT 1")
    ShoppingListEntity getLatestListByUser(int userId);

    // ── Item management ──────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItem(ShoppingListItemEntity item);

    @Update
    void updateItem(ShoppingListItemEntity item);

    @Query("DELETE FROM shopping_list_items WHERE id = :itemId")
    void deleteItem(int itemId);

    @Query("SELECT * FROM shopping_list_items WHERE shopping_list_id = :listId ORDER BY id ASC")
    List<ShoppingListItemEntity> getItemsByListId(int listId);

    /** Find an existing item by name (case-insensitive) to support quantity merging */
    @Query("SELECT * FROM shopping_list_items WHERE shopping_list_id = :listId AND LOWER(ingredient_name) = LOWER(:name) LIMIT 1")
    ShoppingListItemEntity getItemByName(int listId, String name);

    /** Clear all items in a list */
    @Query("DELETE FROM shopping_list_items WHERE shopping_list_id = :listId")
    void clearList(int listId);
}
