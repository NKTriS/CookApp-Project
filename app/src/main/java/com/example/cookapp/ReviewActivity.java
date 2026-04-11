package com.example.cookapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.ReviewsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewActivity extends AppCompatActivity {
    private int currentRecipeId = -1;
    private ReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();
    private List<Review> fullReviewList = new ArrayList<>();
    private RatingBar ratingBar;
    private TextView tvAvgRating;
    private ProgressBar pbLoading;
    private ApiService apiService;

    // Star filter
    private int selectedStarFilter = 0; // 0 = all
    private TextView[] chipViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        currentRecipeId = getIntent().getIntExtra("recipe_id", -1);

        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        ratingBar   = findViewById(R.id.rating_bar);
        tvAvgRating = findViewById(R.id.tv_avg_rating);
        pbLoading   = findViewById(R.id.pb_loading);

        RecyclerView recyclerReview = findViewById(R.id.rv_reviews);
        if (recyclerReview != null) {
            recyclerReview.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ReviewAdapter(reviewList, this::deleteReview);
            recyclerReview.setAdapter(adapter);
        }

        // Setup star filter chips
        setupStarFilterChips();

        Button btnSubmit  = findViewById(R.id.btn_submit);
        EditText etReview = findViewById(R.id.et_review);

        if (btnSubmit != null && etReview != null) {
            btnSubmit.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                if (!session.isLoggedIn()) {
                    Toast.makeText(this, "❗ Vui lòng đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentRecipeId == -1) {
                    Toast.makeText(this, "Lỗi: Không tìm thấy món ăn!", Toast.LENGTH_SHORT).show();
                    return;
                }
                String comment = etReview.getText().toString().trim();
                if (comment.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập nhận xét", Toast.LENGTH_SHORT).show();
                    return;
                }
                int chosenRating = (ratingBar != null) ? (int) ratingBar.getRating() : 5;
                submitReviewToApi(currentRecipeId, chosenRating, comment,
                    session.getCachedUserName());
                etReview.setText("");
            });
        }

        if (currentRecipeId != -1) {
            loadReviewsFromApi();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // STAR FILTER
    // ─────────────────────────────────────────────────────────────

    private void setupStarFilterChips() {
        chipViews = new TextView[6];
        chipViews[0] = findViewById(R.id.chip_all);
        chipViews[5] = findViewById(R.id.chip_5);
        chipViews[4] = findViewById(R.id.chip_4);
        chipViews[3] = findViewById(R.id.chip_3);
        chipViews[2] = findViewById(R.id.chip_2);
        chipViews[1] = findViewById(R.id.chip_1);

        for (int i = 0; i < chipViews.length; i++) {
            if (chipViews[i] == null) continue;
            final int star = i;
            chipViews[i].setOnClickListener(v -> {
                selectedStarFilter = star;
                updateChipUI();
                applyStarFilter();
            });
        }
    }

    private void updateChipUI() {
        for (int i = 0; i < chipViews.length; i++) {
            if (chipViews[i] == null) continue;
            if (i == selectedStarFilter) {
                chipViews[i].setBackgroundResource(R.drawable.bg_action_chip_selected);
                chipViews[i].setTextColor(0xFFFFFFFF);
            } else {
                chipViews[i].setBackgroundResource(R.drawable.bg_action_chip);
                chipViews[i].setTextColor(0xFF555555);
            }
        }
    }

    private void applyStarFilter() {
        reviewList.clear();
        if (selectedStarFilter == 0) {
            // Show all
            reviewList.addAll(fullReviewList);
        } else {
            for (Review r : fullReviewList) {
                if (r.getRating() == selectedStarFilter) {
                    reviewList.add(r);
                }
            }
        }
        currentPage = 1;
        updatePaginationUI();
    }

    // ─────────────────────────────────────────────────────────────
    // PAGINATION
    // ─────────────────────────────────────────────────────────────

    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    private List<Review> filteredList = new ArrayList<>();

    private void updatePaginationUI() {
        android.widget.LinearLayout layoutPagination = findViewById(R.id.layout_pagination);
        if (layoutPagination == null) return;
        layoutPagination.removeAllViews();

        // Build filtered list first
        filteredList.clear();
        if (selectedStarFilter == 0) {
            filteredList.addAll(fullReviewList);
        } else {
            for (Review r : fullReviewList) {
                if (r.getRating() == selectedStarFilter) filteredList.add(r);
            }
        }

        int totalPages = (int) Math.ceil(filteredList.size() / (double) PAGE_SIZE);
        if (totalPages <= 1) { loadPageData(); return; }

        for (int i = 1; i <= totalPages; i++) {
            final int page = i;
            TextView pill = new TextView(this);
            pill.setText(String.valueOf(i));
            int dp16 = (int)(16 * getResources().getDisplayMetrics().density);
            int dp8  = (int)(8  * getResources().getDisplayMetrics().density);
            pill.setPadding(dp16, dp8, dp16, dp8);
            pill.setTextSize(14f);

            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            int m4 = (int)(4 * getResources().getDisplayMetrics().density);
            lp.setMargins(m4, 0, m4, 0);
            pill.setLayoutParams(lp);

            if (page == currentPage) {
                pill.setBackgroundResource(R.drawable.bg_pill_black);
                pill.setTextColor(android.graphics.Color.WHITE);
            } else {
                pill.setBackgroundResource(R.drawable.bg_pill_grey);
                pill.setTextColor(android.graphics.Color.parseColor("#7A7A7A"));
            }
            pill.setTypeface(null, android.graphics.Typeface.BOLD);
            pill.setOnClickListener(pv -> { currentPage = page; updatePaginationUI(); });
            layoutPagination.addView(pill);
        }
        loadPageData();
    }

    private void loadPageData() {
        reviewList.clear();
        int start = (currentPage - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredList.size());
        if (start < filteredList.size()) reviewList.addAll(filteredList.subList(start, end));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadReviewsFromApi() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        SessionManager session = new SessionManager(this);
        int currentUserId = session.getUserId();
        apiService.getRecipeReviews(currentRecipeId).enqueue(new Callback<ReviewsResponse>() {
            @Override
            public void onResponse(Call<ReviewsResponse> call, Response<ReviewsResponse> response) {
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        ReviewsResponse body = response.body();
                        fullReviewList.clear();
                        if (body.reviews != null) {
                            for (Review r : body.reviews) {
                                // Đánh dấu review của user hiện tại để hiển thị nút Xóa
                                r.setCurrentUser(currentUserId != -1 && r.getUserId() == currentUserId);
                            }
                            fullReviewList.addAll(body.reviews);
                        }
                        currentPage = 1;
                        applyStarFilter();

                        if (tvAvgRating != null) {
                            if (fullReviewList.isEmpty()) {
                                tvAvgRating.setText("–");
                            } else {
                                tvAvgRating.setText(String.format(Locale.getDefault(),
                                    "%.1f ⭐ (%d đánh giá)", body.average_rating, body.count));
                            }
                        }
                    } else {
                        Toast.makeText(ReviewActivity.this, "Lỗi tải đánh giá", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<ReviewsResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    Toast.makeText(ReviewActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void submitReviewToApi(int recipeId, int rating, String comment, String author) {
        Review newReview = new Review(null, author != null ? author : "Ẩn danh", rating, "", comment);
        apiService.createReview(recipeId, newReview).enqueue(new Callback<Review>() {
            @Override
            public void onResponse(Call<Review> call, Response<Review> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ReviewActivity.this,
                            "Cảm ơn bạn đã đánh giá! (" + rating + "⭐)", Toast.LENGTH_SHORT).show();
                        loadReviewsFromApi();
                    } else {
                        Toast.makeText(ReviewActivity.this, "Lỗi gửi đánh giá", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<Review> call, Throwable t) {
                runOnUiThread(() ->
                    Toast.makeText(ReviewActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void deleteReview(Review review) {
        if (review.getId() == null) return;
        int reviewId;
        try {
            reviewId = Integer.parseInt(review.getId());
        } catch (NumberFormatException e) {
            return;
        }

        apiService.deleteReview(currentRecipeId, reviewId).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ReviewActivity.this, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show();
                        loadReviewsFromApi(); // reload list
                    } else {
                        Toast.makeText(ReviewActivity.this, "Không thể xóa đánh giá", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(ReviewActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
