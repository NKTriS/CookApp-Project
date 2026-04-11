package com.example.cookapp.repository;

import android.content.Context;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.AuthRequest;
import com.example.cookapp.api.dto.AuthResponse;
import com.example.cookapp.api.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService apiService;

    public AuthRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(ApiService.class);
    }

    public interface AuthCallback {
        void onResult(Resource<AuthResponse> result);
    }

    public void login(String email, String password, AuthCallback callback) {
        callback.onResult(Resource.loading(null));
        apiService.login(new AuthRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi đăng nhập: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onResult(Resource.error("Không thể kết nối máy chủ: " + t.getMessage(), null));
            }
        });
    }

    public void register(String email, String pass, String name, String phone, String address, AuthCallback callback) {
        callback.onResult(Resource.loading(null));
        RegisterRequest req = new RegisterRequest(email, pass, name, phone, address);
        apiService.register(req).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi đăng ký: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onResult(Resource.error("Không thể kết nối máy chủ: " + t.getMessage(), null));
            }
        });
    }
}
