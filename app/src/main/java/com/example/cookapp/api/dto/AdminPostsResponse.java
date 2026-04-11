package com.example.cookapp.api.dto;

import java.util.List;

public class AdminPostsResponse {
    public int total;
    public int page;
    public int totalPages;
    public List<PostDto> posts;
}
