package com.example.cookapp.api;

import com.google.gson.annotations.SerializedName;

/**
 * GHN Calculate Fee — Request body
 * POST /v2/shipping-order/fee
 *
 * Các district_id của Hà Nội & TP.HCM:
 *   Quận 1 HCM         = 1442
 *   Quận Hoàn Kiếm HN  = 1489
 *   Quận Ba Đình HN    = 1490
 *   ... (full list tại https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/district)
 *
 * service_type_id:
 *   2 = Chuẩn (2-3 ngày)
 *   5 = Nhanh (1 ngày)
 */
public class GhnFeeRequest {

    @SerializedName("from_district_id")
    public int fromDistrictId;

    @SerializedName("to_district_id")
    public int toDistrictId;

    @SerializedName("to_ward_code")
    public String toWardCode;      // mã phường/xã (optional)

    @SerializedName("service_type_id")
    public int serviceTypeId = 2;  // mặc định Chuẩn

    @SerializedName("weight")
    public int weight;              // gram

    @SerializedName("length")
    public int length = 30;

    @SerializedName("width")
    public int width = 20;

    @SerializedName("height")
    public int height = 10;

    @SerializedName("insurance_value")
    public int insuranceValue = 0;

    public GhnFeeRequest(int fromDistrictId, int toDistrictId, int weightGram) {
        this.fromDistrictId = fromDistrictId;
        this.toDistrictId   = toDistrictId;
        this.weight         = weightGram;
    }
}
