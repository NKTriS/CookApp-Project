package com.example.cookapp.repository;

import android.content.Context;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.ShoppingListResponse;
import com.example.cookapp.api.dto.SyncShoppingListRequest;
import com.example.cookapp.api.dto.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingRepository {
    private final ApiService apiService;

    public ShoppingRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(ApiService.class);
    }

    public interface StateCallback<T> {
        void onResult(Resource<T> result);
    }

    public void getShoppingList(StateCallback<ShoppingListResponse> callback) {
        callback.onResult(Resource.loading(null));
        apiService.getShoppingList().enqueue(new Callback<ShoppingListResponse>() {
            @Override
            public void onResponse(Call<ShoppingListResponse> call, Response<ShoppingListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi đồng bộ danh sách: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<ShoppingListResponse> call, Throwable t) {
                callback.onResult(Resource.error("Lỗi kết nối mạng: " + t.getMessage(), null));
            }
        });
    }

    public void syncShoppingList(SyncShoppingListRequest ds, StateCallback<GenericResponse> callback) {
        apiService.syncShoppingList(ds).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Sync failed", null));
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                callback.onResult(Resource.error(t.getMessage(), null));
            }
        });
    }

    /** Thêm 1 item vào shopping list API (deduplication do server xử lý) */
    public void addShoppingItem(SyncShoppingListRequest.ShoppingItem item, StateCallback<GenericResponse> callback) {
        apiService.addShoppingItem(item).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Add item failed: " + response.code(), null));
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                callback.onResult(Resource.error(t.getMessage(), null));
            }
        });
    }
}
