package com.example.cookapp.api.dto;

import java.util.List;

public class AdminOrdersResponse {
    public int total;
    public int page;
    public int totalPages;
    public List<OrderDto> orders;
}
