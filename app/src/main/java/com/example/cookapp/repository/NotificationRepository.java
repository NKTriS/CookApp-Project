package com.example.cookapp.repository;

import android.content.Context;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.NotificationDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationRepository {
    private final ApiService apiService;

    public NotificationRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(ApiService.class);
    }

    public interface StateCallback<T> {
        void onResult(Resource<T> result);
    }

    public void getNotifications(StateCallback<List<NotificationDto>> callback) {
        callback.onResult(Resource.loading(null));
        apiService.getNotifications().enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(Call<List<NotificationDto>> call, Response<List<NotificationDto>> response) {
                if(response.isSuccessful()) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi thông báo", null));
                }
            }

            @Override
            public void onFailure(Call<List<NotificationDto>> call, Throwable t) {
                callback.onResult(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
    }

    public void markAsRead(int notifId, StateCallback<NotificationDto> callback) {
        apiService.markNotificationAsRead(notifId).enqueue(new Callback<NotificationDto>() {
            @Override
            public void onResponse(Call<NotificationDto> call, Response<NotificationDto> response) {
                if(response.isSuccessful()) {
                    callback.onResult(Resource.success(response.body()));
                }
            }
            @Override
            public void onFailure(Call<NotificationDto> call, Throwable t) {
            }
        });
    }
}
