package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response DTO cho GET /api/payment/status/:orderId
 */
public class PaymentStatusResponse {
    @SerializedName("paymentStatus")
    public String paymentStatus;  // "pending" | "paid" | "expired" | "none"

    @SerializedName("paymentCode")
    public String paymentCode;

    @SerializedName("paidAt")
    public String paidAt;
}
