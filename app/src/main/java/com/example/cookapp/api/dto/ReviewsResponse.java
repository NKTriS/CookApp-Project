package com.example.cookapp.api.dto;

import java.util.List;
import com.example.cookapp.Review;

public class ReviewsResponse {
    public double average_rating;
    public int count;
    public List<Review> reviews;
}
