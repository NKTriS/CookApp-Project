package com.example.cookapp.repository;

import android.content.Context;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.FavoriteDto;
import com.example.cookapp.api.dto.ToggleFavoriteRequest;
import com.example.cookapp.api.dto.ToggleFavoriteResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;

    public UserRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(ApiService.class);
    }

    public interface StateCallback<T> {
        void onResult(Resource<T> result);
    }

    /** Load danh sách yêu thích kèm Recipe title + image_url */
    public void getFavorites(StateCallback<List<FavoriteDto>> callback) {
        callback.onResult(Resource.loading(null));
        apiService.getFavorites().enqueue(new Callback<List<FavoriteDto>>() {
            @Override
            public void onResponse(Call<List<FavoriteDto>> call, Response<List<FavoriteDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi tải yêu thích: " + response.code(), null));
                }
            }
            @Override
            public void onFailure(Call<List<FavoriteDto>> call, Throwable t) {
                callback.onResult(Resource.error("Mất kết nối: " + t.getMessage(), null));
            }
        });
    }

    /** Kiểm tra 1 recipe có trong favorites của user không */
    public void checkFavorite(int recipeId, StateCallback<Boolean> callback) {
        apiService.checkFavorite(recipeId).enqueue(new Callback<ToggleFavoriteResponse>() {
            @Override
            public void onResponse(Call<ToggleFavoriteResponse> call, Response<ToggleFavoriteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body().isFavorite));
                } else {
                    callback.onResult(Resource.success(false));
                }
            }
            @Override
            public void onFailure(Call<ToggleFavoriteResponse> call, Throwable t) {
                // Offline: không biết trạng thái — trả false, không crash
                callback.onResult(Resource.success(false));
            }
        });
    }

    /** Toggle yêu thích và trả về trạng thái mới */
    public void toggleFavorite(int recipeId, StateCallback<ToggleFavoriteResponse> callback) {
        apiService.toggleFavorite(new ToggleFavoriteRequest(recipeId)).enqueue(new Callback<ToggleFavoriteResponse>() {
            @Override
            public void onResponse(Call<ToggleFavoriteResponse> call, Response<ToggleFavoriteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi cập nhật yêu thích: " + response.code(), null));
                }
            }
            @Override
            public void onFailure(Call<ToggleFavoriteResponse> call, Throwable t) {
                callback.onResult(Resource.error("Mất kết nối: " + t.getMessage(), null));
            }
        });
    }
}
