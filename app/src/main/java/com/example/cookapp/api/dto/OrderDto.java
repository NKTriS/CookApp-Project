package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;

public class OrderDto {
    public int id;

    @SerializedName("user_id")
    public int userId;

    @SerializedName("customerName")
    public String customerName;

    public String phone;
    public String address;

    @SerializedName("totalPrice")
    public long totalPrice;

    @SerializedName("shippingFee")
    public long shippingFee;

    @SerializedName("itemsSummary")
    public String itemsSummary;

    public String status;

    @SerializedName("paymentMethod")
    public String paymentMethod;

    public String note;

    @SerializedName("cancelReason")
    public String cancelReason;

    @SerializedName("cancelledAt")
    public String cancelledAt;

    @SerializedName("paymentStatus")
    public String paymentStatus;

    @SerializedName("paymentCode")
    public String paymentCode;

    @SerializedName("paidAt")
    public String paidAt;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;
}
