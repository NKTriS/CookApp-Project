package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminStatsDto {
    public int users;
    public int recipes;
    public int orders;
    public int posts;
    public int reviews;
    public int ingredients;
    public long revenue;
    public int newUsersToday;

    @SerializedName("ordersByStatus")
    public List<OrderStatusCount> ordersByStatus;

    @SerializedName("recentOrders")
    public List<OrderDto> recentOrders;

    public static class OrderStatusCount {
        public String status;
        public int count;
    }
}
