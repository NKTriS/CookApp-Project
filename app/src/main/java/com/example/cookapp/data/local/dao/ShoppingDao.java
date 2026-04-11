package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cookapp.data.local.entity.ShoppingListEntity;
import com.example.cookapp.data.local.entity.ShoppingListItemEntity;

import java.util.List;

@Dao
public interface ShoppingDao {

    @Insert
    long createShoppingList(ShoppingListEntity list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertShoppingItems(List<ShoppingListItemEntity> items);

    @Query("SELECT * FROM shopping_list_items ORDER BY checked ASC, id DESC")
    List<ShoppingListItemEntity> getActiveShoppingItems();

    @Query("UPDATE shopping_list_items SET checked = :isChecked WHERE id = :itemId")
    void updateShoppingItemState(int itemId, boolean isChecked);

    @Query("DELETE FROM shopping_list_items WHERE checked = 1")
    void clearCheckedItems();

    @Query("DELETE FROM shopping_list_items")
    void deleteAll();

    @Query("DELETE FROM shopping_lists")
    void deleteAllLists();
}
