package com.example.cookapp.api.dto;

import java.util.List;

public class AdminUsersResponse {
    public int total;
    public int page;
    public int totalPages;
    public List<UserDto> users;
}
