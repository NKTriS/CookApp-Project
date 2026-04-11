package com.example.cookapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.SyncShoppingListRequest;
import com.example.cookapp.api.dto.GenericResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Singleton quản lý giỏ hàng.
 * Giỏ hàng được persist vào SharedPreferences để không bị mất khi app bị kill.
 */
public class CartManager {
    private static CartManager instance;
    private static final String PREF_NAME = "CookApp_Cart";
    private static final String KEY_CART_ITEMS = "cart_items";

    private final List<CartItem> cartItems;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final ApiService apiService;
    private final Context appContext;

    private CartManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        apiService = RetrofitClient.getClient(appContext).create(ApiService.class);
        cartItems = loadFromDisk();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }

    /**
     * Backward compatibility: cho phép gọi getInstance() không cần context.
     * CHỈ dùng SAU khi getInstance(context) đã được gọi ít nhất 1 lần.
     */
    public static synchronized CartManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CartManager chưa được khởi tạo! Gọi getInstance(Context) trước.");
        }
        return instance;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void addOrUpdateItem(String id, String name, int price, int qtyChange) {
        addOrUpdateItem(id, name, "", price, qtyChange);
    }

    public void addOrUpdateItem(String id, String name, String unit, int price, int qtyChange) {
        for (CartItem item : cartItems) {
            if (item.getId().equals(id)) {
                int newQty = item.getQty() + qtyChange;
                if (newQty <= 0) {
                    cartItems.remove(item);
                } else {
                    item.setQty(newQty);
                }
                saveToDisk();
                return;
            }
        }

        // Chưa có → thêm mới nếu qty > 0
        if (qtyChange > 0) {
            cartItems.add(new CartItem(id, name, unit, price, qtyChange));
            saveToDisk();
        }
    }

    public void addMultipleItems(List<CartItem> items) {
        boolean changed = false;
        for (CartItem newItem : items) {
            boolean found = false;
            for (CartItem item : cartItems) {
                if (item.getId().equals(newItem.getId())) {
                    item.setQty(item.getQty() + newItem.getQty());
                    found = true;
                    changed = true;
                    break;
                }
            }
            if (!found && newItem.getQty() > 0) {
                cartItems.add(newItem);
                changed = true;
            }
        }
        if (changed) {
            saveToDisk();
        }
    }

    public void removeItem(String id) {
        cartItems.removeIf(item -> item.getId().equals(id));
        saveToDisk();
    }

    public void clearCart() {
        cartItems.clear();
        saveToDisk();
    }

    public void replaceEntireCart(List<CartItem> newItems) {
        cartItems.clear();
        cartItems.addAll(newItems);
        saveToDisk();
    }

    // ── Persistence ─────────────────────────────────────────────────────────────

    private void saveToDisk() {
        String json = gson.toJson(cartItems);
        prefs.edit().putString(KEY_CART_ITEMS, json).apply();

        // ── SYNC LÊN BACKEND (Sáp nhập Giỏ hàng & Danh sách đi chợ) ──
        if (new SessionManager(appContext).isLoggedIn()) {
            List<SyncShoppingListRequest.ShoppingItem> dtos = new ArrayList<>();
            for (CartItem item : cartItems) {
                dtos.add(new SyncShoppingListRequest.ShoppingItem(
                        item.getName(), item.getQty(), item.getUnit(), false, item.getPrice()
                ));
            }
            apiService.syncShoppingList(new SyncShoppingListRequest(dtos))
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> res) {
                            if (!res.isSuccessful()) {
                                android.util.Log.w("CartManager", "Sync failed: HTTP " + res.code());
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            android.util.Log.w("CartManager", "Sync failed: " + t.getMessage());
                        }
                    });
        }
    }

    private List<CartItem> loadFromDisk() {
        String json = prefs.getString(KEY_CART_ITEMS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<ArrayList<CartItem>>() {}.getType();
            List<CartItem> loaded = gson.fromJson(json, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
