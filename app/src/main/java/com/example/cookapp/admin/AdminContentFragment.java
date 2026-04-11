package com.example.cookapp.admin;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.Recipe;
import com.example.cookapp.Review;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.AdminPostsResponse;
import com.example.cookapp.api.dto.AdminRecipesResponse;
import com.example.cookapp.api.dto.AdminReviewsResponse;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.PostDto;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminContentFragment extends Fragment {

    private RecyclerView rvContent;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private LinearLayout chipContainer;
    private View fabAdd;

    private String currentTab = "recipes";
    private final String[][] tabs = {
            {"recipes", "🍲 Công thức"},
            {"posts", "📝 Bài viết"},
            {"reviews", "⭐ Đánh giá"}
    };

    private AdminRecipeAdapter recipeAdapter;
    private AdminPostAdapter postAdapter;
    private AdminReviewAdapter reviewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvContent = view.findViewById(R.id.rv_content);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        chipContainer = view.findViewById(R.id.chip_container);
        fabAdd = view.findViewById(R.id.fab_add);
        
        fabAdd.setOnClickListener(v -> {
            if ("recipes".equals(currentTab)) {
                startActivity(new Intent(requireContext(), com.example.cookapp.AdminAddRecipeActivity.class));
            }
        });

        rvContent.setLayoutManager(new LinearLayoutManager(getContext()));

        recipeAdapter = new AdminRecipeAdapter(new ArrayList<>(), new AdminRecipeAdapter.AdminRecipeListener() {
            @Override
            public void onDelete(Recipe recipe) {
                confirmDeleteRecipe(recipe);
            }

            @Override
            public void onEditTime(Recipe recipe) {
                Intent intent = new Intent(requireContext(), AdminEditTimeActivity.class);
                intent.putExtra("recipe_id", recipe.getId());
                intent.putExtra("recipe_title", recipe.getTitle());
                startActivity(intent);
            }
        });
        postAdapter = new AdminPostAdapter(new ArrayList<>(), this::confirmDeletePost);
        reviewAdapter = new AdminReviewAdapter(new ArrayList<>(), this::confirmDeleteReview);

        buildChips();
        loadContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getUserVisibleHint() || isResumed()) {
            loadContent();
        }
    }

    private void buildChips() {
        chipContainer.removeAllViews();
        int dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                getResources().getDisplayMetrics());

        for (String[] tab : tabs) {
            TextView chip = new TextView(requireContext());
            chip.setText(tab[1]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(14 * dp, 8 * dp, 14 * dp, 8 * dp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8 * dp);
            chip.setLayoutParams(lp);

            boolean selected = tab[0].equals(currentTab);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(20 * dp);
            if (selected) {
                bg.setColor(0xFFFF7043);
                chip.setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(0xFF1C1F2E);
                bg.setStroke(1 * dp, 0xFF2A2E3F);
                chip.setTextColor(0xFF8B8FA3);
            }
            chip.setBackground(bg);

            chip.setOnClickListener(v -> {
                currentTab = tab[0];
                buildChips();
                loadContent();
            });

            chipContainer.addView(chip);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadContent();
        }
    }

    private void loadContent() {
        if ("recipes".equals(currentTab)) {
            fabAdd.setVisibility(View.VISIBLE);
        } else {
            fabAdd.setVisibility(View.GONE);
        }

        switch (currentTab) {
            case "recipes": loadRecipes(); break;
            case "posts":   loadPosts(); break;
            case "reviews": loadReviews(); break;
        }
    }

    // ── RECIPES ────────────────────────────────
    private void loadRecipes() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvContent.setAdapter(recipeAdapter);

        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.getAdminRecipes(1, 50, "").enqueue(new Callback<AdminRecipesResponse>() {
            @Override
            public void onResponse(Call<AdminRecipesResponse> call, Response<AdminRecipesResponse> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    recipeAdapter.setItems(response.body().recipes);
                    tvEmpty.setVisibility(response.body().recipes.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onFailure(Call<AdminRecipesResponse> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteRecipe(Recipe r) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa công thức")
                .setMessage("Xóa \"" + r.getTitle() + "\"? Tất cả dữ liệu liên quan sẽ bị xóa.")
                .setPositiveButton("Xóa", (d, w) -> deleteRecipe(r.getId()))
                .setNegativeButton("Hủy", null).show();
    }

    private void deleteRecipe(int id) {
        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.deleteAdminRecipe(id).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã xóa ✓", Toast.LENGTH_SHORT).show();
                    loadRecipes();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── POSTS ──────────────────────────────────
    private void loadPosts() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvContent.setAdapter(postAdapter);

        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.getAdminPosts(1, 50).enqueue(new Callback<AdminPostsResponse>() {
            @Override
            public void onResponse(Call<AdminPostsResponse> call, Response<AdminPostsResponse> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    postAdapter.setItems(response.body().posts);
                    tvEmpty.setVisibility(response.body().posts.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onFailure(Call<AdminPostsResponse> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeletePost(PostDto p) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa bài viết")
                .setMessage("Xóa \"" + p.title + "\"? Comments liên quan cũng sẽ bị xóa.")
                .setPositiveButton("Xóa", (d, w) -> deletePost(p.id))
                .setNegativeButton("Hủy", null).show();
    }

    private void deletePost(int id) {
        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.deleteAdminPost(id).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã xóa ✓", Toast.LENGTH_SHORT).show();
                    loadPosts();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── REVIEWS ────────────────────────────────
    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvContent.setAdapter(reviewAdapter);

        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.getAdminReviews(1, 50).enqueue(new Callback<AdminReviewsResponse>() {
            @Override
            public void onResponse(Call<AdminReviewsResponse> call, Response<AdminReviewsResponse> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    reviewAdapter.setItems(response.body().reviews);
                    tvEmpty.setVisibility(response.body().reviews.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onFailure(Call<AdminReviewsResponse> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteReview(Review r) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa đánh giá")
                .setMessage("Xóa đánh giá này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    try { deleteReview(Integer.parseInt(r.getId())); }
                    catch (Exception e) { Toast.makeText(getContext(), "Lỗi ID", Toast.LENGTH_SHORT).show(); }
                })
                .setNegativeButton("Hủy", null).show();
    }

    private void deleteReview(int id) {
        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.deleteAdminReview(id).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã xóa ✓", Toast.LENGTH_SHORT).show();
                    loadReviews();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
