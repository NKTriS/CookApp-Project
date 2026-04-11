package com.example.cookapp.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * GHN Shipping Fee API v2
 * Base URL: https://dev-online-gateway.ghn.vn/ (sandbox)
 * Docs: https://api.ghn.vn/home/docs/detail?id=76
 *
 * Để chạy thật: đăng ký tài khoản dev tại https://dev.ghn.vn
 *   → lấy Token + ShopId từ Dashboard
 *   → thay vào GhnConfig.TOKEN và GhnConfig.SHOP_ID
 */
public interface GhnApiService {

    /**
     * Tính phí ship.
     * Header: Token (từ GHN Dashboard), ShopId
     * Body: from_district_id, to_district_id, to_ward_code, weight, service_type_id
     */
    @POST("shiip/public-api/v2/shipping-order/fee")
    Call<GhnFeeResponse> calculateFee(
        @Header("Token")  String token,
        @Header("ShopId") String shopId,
        @Body GhnFeeRequest body
    );
}
