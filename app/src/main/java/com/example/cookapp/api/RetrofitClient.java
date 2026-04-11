package com.example.cookapp.api;

import android.content.Context;

import com.example.cookapp.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Sử dụng IP tự động phát hiện (Emulator vs Real Device)
    private static final String BASE_URL = com.example.cookapp.utils.NetworkConfig.getBaseUrl() + "/";
    private static Retrofit retrofit = null;
    private static Context appContext = null;

    public static Retrofit getClient(Context context) {
        // Lưu ApplicationContext để tránh memory leak và token luôn fresh
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }

        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        // Đọc token mới nhất từ SharedPreferences mỗi request
                        String token = new SessionManager(appContext).getAuthToken();

                        if (token != null && !token.isEmpty()) {
                            Request requestWithAuth = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(requestWithAuth);
                        }

                        return chain.proceed(original);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    /** Gọi khi logout để buộc tạo mới Retrofit instance với token trống */
    public static void reset() {
        retrofit = null;
    }
}
