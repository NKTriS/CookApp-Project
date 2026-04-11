package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body cho POST /api/orders
 */
public class CreateOrderRequest {
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

    @SerializedName("paymentMethod")
    public String paymentMethod;

    public String note;

    public CreateOrderRequest(String customerName, String phone, String address,
                              long totalPrice, long shippingFee, String itemsSummary,
                              String paymentMethod, String note) {
        this.customerName = customerName;
        this.phone = phone;
        this.address = address;
        this.totalPrice = totalPrice;
        this.shippingFee = shippingFee;
        this.itemsSummary = itemsSummary;
        this.paymentMethod = paymentMethod;
        this.note = note;
    }
}
