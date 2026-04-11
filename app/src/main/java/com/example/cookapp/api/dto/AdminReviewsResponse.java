package com.example.cookapp.api.dto;

import com.example.cookapp.Review;
import java.util.List;

public class AdminReviewsResponse {
    public int total;
    public int page;
    public int totalPages;
    public List<Review> reviews;
}
