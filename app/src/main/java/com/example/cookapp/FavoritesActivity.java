package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.FavoriteDto;
import com.example.cookapp.repository.UserRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private TextView tvEmpty;
    private ProgressBar pbLoading;
    private FavoriteAdapter adapter;
    private final List<FavoriteRecipe> favoriteRecipes = new ArrayList<>();
    private final List<FavoriteRecipe> allFavorites = new ArrayList<>();
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorites);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, 0);
            return insets;
        });

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userRepo = new UserRepository(this);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        rvFavorites = findViewById(R.id.rv_favorites);
        tvEmpty     = findViewById(R.id.tv_empty);
        pbLoading   = findViewById(R.id.pb_loading);

        adapter = new FavoriteAdapter(favoriteRecipes, new FavoriteAdapter.OnFavoriteClickListener() {
            @Override
            public void onDeleteClick(FavoriteRecipe recipe) {
                showDeleteConfirmDialog(recipe);
            }

            @Override
            public void onItemClick(FavoriteRecipe recipe) {
                Intent intent = new Intent(FavoritesActivity.this, RecipeDetailActivity.class);
                try {
                    intent.putExtra("recipe_id", Integer.parseInt(recipe.getId()));
                } catch (NumberFormatException e) {
                    intent.putExtra("recipe_id", 1);
                }
                startActivity(intent);
            }
        });

        if (rvFavorites != null) {
            rvFavorites.setLayoutManager(new LinearLayoutManager(this));
            rvFavorites.setAdapter(adapter);
        }

        // Search box — filter as you type + Enter to search
        android.widget.EditText etSearch = findViewById(R.id.et_search);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    filterFavorites(s.toString());
                }
            });
            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    filterFavorites(etSearch.getText().toString());
                    // Ẩn bàn phím
                    android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                    return true;
                }
                return false;
            });
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_fav);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_recipe) {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_shop) {
                    startActivity(new Intent(this, ShopActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_fav) return true;
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        userRepo.getFavorites(result -> {
            if (result.status == Resource.Status.LOADING) {
                runOnUiThread(() -> { if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE); });
            } else if (result.status == Resource.Status.SUCCESS && result   .data != null) {
                Log.d("FAV", "Loaded " + result.data.size() + " favorites from API");
                List<FavoriteRecipe> loaded = new ArrayList<>();
                for (FavoriteDto fav : result.data) {
                    Log.d("FAV", "  fav id=" + fav.id + " recipe_id=" + fav.recipe_id + " recipe=" + (fav.recipe != null ? fav.recipe.title : "NULL"));
                    // Dùng recipe title thật từ API (kèm Recipe object)
                    String title = "Công thức #" + fav.recipe_id;
                    String imageUrl = null;
                    if (fav.recipe != null) {
                        if (fav.recipe.title != null) title = fav.recipe.title;
                        imageUrl = fav.recipe.image_url;
                    }
                    FavoriteRecipe fr = new FavoriteRecipe(
                        String.valueOf(fav.recipe_id),
                        title,
                        R.drawable.img_category3
                    );
                    fr.setImageUrl(imageUrl);
                    loaded.add(fr);
                }
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    favoriteRecipes.clear();
                    favoriteRecipes.addAll(loaded);
                    allFavorites.clear();
                    allFavorites.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            } else if (result.status == Resource.Status.ERROR) {
                Log.e("FAV", "Error loading favorites: " + result.message);
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải yêu thích: " + result.message, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    private void filterFavorites(String query) {
        favoriteRecipes.clear();
        if (query == null || query.trim().isEmpty()) {
            favoriteRecipes.addAll(allFavorites);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (FavoriteRecipe recipe : allFavorites) {
                if (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(lowerQuery)) {
                    favoriteRecipes.add(recipe);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateEmptyState();
    }

    private void showDeleteConfirmDialog(FavoriteRecipe recipe) {
        new AlertDialog.Builder(this)
            .setTitle("Bỏ yêu thích?")
            .setMessage("Bạn có muốn xóa \"" + recipe.getTitle() + "\" khỏi danh sách yêu thích?")
            .setPositiveButton("Đồng ý", (dialog, which) -> {
                int idx = favoriteRecipes.indexOf(recipe);
                if (idx != -1) {
                    favoriteRecipes.remove(idx);
                    adapter.notifyItemRemoved(idx);
                    updateEmptyState();
                }
                try {
                    int recipeId = Integer.parseInt(recipe.getId());
                    userRepo.toggleFavorite(recipeId, res -> {
                        if (res.status == Resource.Status.ERROR) {
                            runOnUiThread(() -> Toast.makeText(this, "Lỗi xóa yêu thích", Toast.LENGTH_SHORT).show());
                        }
                    });
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void updateEmptyState() {
        boolean empty = favoriteRecipes.isEmpty();
        if (rvFavorites != null) rvFavorites.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (tvEmpty    != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
