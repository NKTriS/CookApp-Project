package com.example.cookapp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.example.cookapp.api.GhnFeeRequest;
import com.example.cookapp.api.GhnFeeResponse;
import com.example.cookapp.api.GhnRetrofitClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper tính phí ship.
 *
 * Luồng:
 *   1. Nếu GhnRetrofitClient đã có token thật → gọi GHN API
 *   2. Nếu chưa có key (demo) → dùng mock fee theo quận/thành phố
 *
 * Để tích hợp GHN thật:
 *   → Điền TOKEN & SHOP_ID vào GhnRetrofitClient.java
 *   → Cung cấp to_district_id từ GHN district API
 */
public class ShippingHelper {

    public interface ShippingFeeCallback {
        void onResult(long feeVnd, String label);
        void onError(String message);
    }

    /**
     * Tính phí ship dựa trên địa chỉ người nhận.
     * - Có GHN key → gọi API thật
     * - Không có key → tính mock theo vùng
     *
     * @param addressText địa chỉ text từ người dùng
     * @param weightGram  tổng khối lượng đơn hàng (gram)
     */
    public static void calculateFee(Context ctx, String addressText, int weightGram,
                                    ShippingFeeCallback callback) {
        if (GhnRetrofitClient.isConfigured()) {
            int toDistrictId = guessDistrictId(addressText);
            callGhnApi(toDistrictId, weightGram, callback);
        } else {
            // Mock: tính phí theo vùng địa lý từ text địa chỉ
            long mockFee = mockFeeByAddress(addressText);
            String label = mockFee <= 15_000 ? "Nội thành" :
                           mockFee <= 25_000 ? "Ngoại thành" : "Tỉnh khác";
            callback.onResult(mockFee, label);
        }
    }

    /** Gọi GHN API v2 tính phí thật */
    private static void callGhnApi(int toDistrictId, int weightGram,
                                   ShippingFeeCallback callback) {
        GhnFeeRequest req = new GhnFeeRequest(
            GhnRetrofitClient.FROM_DISTRICT_ID,
            toDistrictId,
            Math.max(weightGram, 200)  // GHN tối thiểu 200g
        );
        GhnRetrofitClient.getService().calculateFee(
            GhnRetrofitClient.TOKEN,
            GhnRetrofitClient.SHOP_ID,
            req
        ).enqueue(new Callback<GhnFeeResponse>() {
            @Override
            public void onResponse(Call<GhnFeeResponse> call, Response<GhnFeeResponse> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess()) {
                    callback.onResult(res.body().getShippingFee(), "GHN Chuẩn");
                } else {
                    // API trả lỗi → fallback mock
                    callback.onResult(mockFeeByAddress(""), "Ước tính");
                }
            }
            @Override
            public void onFailure(Call<GhnFeeResponse> call, Throwable t) {
                callback.onResult(mockFeeByAddress(""), "Ước tính");
            }
        });
    }

    /**
     * Mock phí ship theo từ khóa địa chỉ.
     * Phù hợp cho demo — giả lập đúng cấu trúc pricing GHN.
     */
    public static long mockFeeByAddress(String address) {
        if (address == null) return 25_000;
        String lower = address.toLowerCase();

        // Nội thành HCM / HN
        if (lower.contains("quận 1") || lower.contains("hoàn kiếm") ||
            lower.contains("ba đình") || lower.contains("q1") ||
            lower.contains("hoan kiem") || lower.contains("district 1")) {
            return 15_000;
        }
        // Ngoại thành HCM / HN
        if (lower.contains("hồ chí minh") || lower.contains("ho chi minh") ||
            lower.contains("tp.hcm") || lower.contains("tphcm") ||
            lower.contains("hà nội") || lower.contains("ha noi") ||
            lower.contains("hcm") || lower.contains("hanoi")) {
            return 22_000;
        }
        // Tỉnh lân cận
        if (lower.contains("bình dương") || lower.contains("đồng nai") ||
            lower.contains("long an") || lower.contains("hưng yên") ||
            lower.contains("bắc ninh") || lower.contains("hải dương")) {
            return 28_000;
        }
        // Tỉnh xa / miền Trung, Bắc, Nam
        if (lower.contains("đà nẵng") || lower.contains("da nang") ||
            lower.contains("huế") || lower.contains("hội an") ||
            lower.contains("nha trang") || lower.contains("đà lạt")) {
            return 35_000;
        }
        // Mặc định — tỉnh khác
        return 25_000;
    }

    /**
     * Ước đoán GHN District ID từ text địa chỉ.
     * Dùng khi có GHN token nhưng chưa implement full district picker.
     */
    private static int guessDistrictId(String address) {
        if (address == null) return 1442; // default: Q1 HCM
        String lower = address.toLowerCase();

        // Map một số quận phổ biến → GHN district ID
        Map<String, Integer> map = new HashMap<>();
        map.put("quận 1",   1442); map.put("q1",      1442);
        map.put("quận 2",   1444); map.put("q2",      1444);
        map.put("quận 3",   1443); map.put("q3",      1443);
        map.put("quận 7",   1450); map.put("q7",      1450);
        map.put("bình thạnh", 1454);
        map.put("gò vấp",   1451);
        map.put("tân bình", 1456);
        map.put("hoàn kiếm", 1489);
        map.put("ba đình",  1490);
        map.put("đống đa",  1491);
        map.put("hai bà trưng", 1492);

        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        return 1442; // fallback
    }

    // ──────────────────────────────────────────────────────────────────────
    // Geocoder helper — convert lat/lng → địa chỉ text (chạy trên thread pool)
    // ──────────────────────────────────────────────────────────────────────

    public interface GeocoderCallback {
        void onAddress(String fullAddress, String city);
        void onFailed();
    }

    public static void reverseGeocode(Context ctx, double lat, double lng, GeocoderCallback cb) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(ctx, new Locale("vi", "VN"));
                List<Address> list = geocoder.getFromLocation(lat, lng, 1);
                if (list != null && !list.isEmpty()) {
                    Address addr = list.get(0);
                    // Ghép địa chỉ đầy đủ
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(addr.getAddressLine(i));
                    }
                    String city = addr.getAdminArea() != null ? addr.getAdminArea() :
                                  (addr.getLocality() != null ? addr.getLocality() : "");
                    cb.onAddress(sb.toString(), city);
                } else {
                    cb.onFailed();
                }
            } catch (IOException e) {
                cb.onFailed();
            }
        });
    }
}
