package com.example.cookapp.api;

import com.google.gson.annotations.SerializedName;

/**
 * GHN Calculate Fee — Response wrapper
 * {
 *   "code": 200,
 *   "message": "Success",
 *   "data": {
 *     "total": 25000,
 *     "service_fee": 20000,
 *     "insurance_fee": 0,
 *     "pick_station_fee": 0,
 *     "coupon_value": 0,
 *     "r2s_fee": 0,
 *     "return_again": 0,
 *     "document_return": 0,
 *     "double_check": 0,
 *     "cod_fee": 0,
 *     "pick_remote_areas_fee": 0,
 *     "deliver_remote_areas_fee": 0,
 *     "cod_failed_fee": 0
 *   }
 * }
 */
public class GhnFeeResponse {

    @SerializedName("code")
    public int code;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("total")
        public long total;          // Tổng phí ship (VND)

        @SerializedName("service_fee")
        public long serviceFee;

        @SerializedName("insurance_fee")
        public long insuranceFee;
    }

    /** Trả về phí ship tổng, hoặc -1 nếu API lỗi */
    public long getShippingFee() {
        if (code == 200 && data != null) return data.total;
        return -1;
    }

    public boolean isSuccess() {
        return code == 200 && data != null;
    }
}
