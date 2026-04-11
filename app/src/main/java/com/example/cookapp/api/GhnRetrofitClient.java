package com.example.cookapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client riêng cho GHN API (base URL khác backend chính).
 *
 * ── Cấu hình để chạy thật ──────────────────────────────────────────────
 * 1. Đăng ký tại https://dev.ghn.vn → lấy Token + ShopId
 * 2. Điền vào TOKEN và SHOP_ID bên dưới
 * 3. Môi trường sandbox (dev) là mặc định, để produce đổi BASE_URL
 *
 * BASE_URL Sandbox : https://dev-online-gateway.ghn.vn/
 * BASE_URL Production: https://online-gateway.ghn.vn/
 */
public class GhnRetrofitClient {

    // ── Điền key thật tại đây khi có account GHN Dev ──────────────────
    public static final String TOKEN   = "YOUR_GHN_TOKEN";    // từ GHN Dashboard
    public static final String SHOP_ID = "YOUR_GHN_SHOP_ID";  // ID shop trong GHN

    // District ID của kho hàng (cửa hàng gốc) — ví dụ: Quận 1 HCM = 1442
    public static final int FROM_DISTRICT_ID = 1442;

    private static final String BASE_URL = "https://dev-online-gateway.ghn.vn/";

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    public static GhnApiService getService() {
        return getClient().create(GhnApiService.class);
    }

    /** Kiểm tra đã cấu hình key thật chưa */
    public static boolean isConfigured() {
        return !TOKEN.equals("YOUR_GHN_TOKEN") && !SHOP_ID.equals("YOUR_GHN_SHOP_ID");
    }
}
