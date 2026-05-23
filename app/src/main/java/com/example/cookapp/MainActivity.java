package com.example.cookapp;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.data.local.AppDatabase;
import com.example.cookapp.data.local.DatabaseInitializer;
import com.example.cookapp.data.local.entity.CategoryEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import android.view.View;
import android.widget.Toast;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialise DB & seed data
        AppDatabase db = AppDatabase.getDatabase(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // ── Bottom navigation ──────────────────────────────────────────────
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_recipe);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_shop) {
                    startActivityWithNoTransition(new Intent(this, ShopActivity.class)); return true;
                } else if (id == R.id.nav_fav) {
                    if (SessionManager.requireLogin(this)) return true;
                    startActivityWithNoTransition(new Intent(this, FavoritesActivity.class)); return true;
                } else if (id == R.id.nav_settings) {
                    startActivityWithNoTransition(new Intent(this, SettingsActivity.class)); return true;
                } else if (id == R.id.nav_recipe) { return true; }
                return false;
            });
        }

        // ── Category RecyclerView (horizontal) ────────────────────────────
        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        if (rvCategories != null) {
            LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            rvCategories.setLayoutManager(llm);
        }

        DatabaseInitializer.populateAsync(db, () -> {
            if (rvCategories != null) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<CategoryEntity> cats = db.recipeDao().getAllCategories();
                    runOnUiThread(() -> {
                        CategoryAdapter adapter = new CategoryAdapter(cats, cat -> {
                            Intent intent = new Intent(this, RecipeListActivity.class);
                            intent.putExtra("category_id",   cat.id);
                            intent.putExtra("category_name", cat.name);
                            startActivity(intent);
                        });
                        rvCategories.setAdapter(adapter);
                    });
                });
            }
        });

        // ── "Xem tất cả" link ─────────────────────────────────────────────
        TextView tvViewAll = findViewById(R.id.tv_view_all);
        if (tvViewAll != null) tvViewAll.setOnClickListener(v ->
                startActivity(new Intent(this, RecipeListActivity.class)));

        // ── AI-1: Smart Recommendations (Hidden if not logged in) ─────────────────────────
        LinearLayout layoutRecommendations = findViewById(R.id.layout_recommendations);
        RecyclerView rvRecommendations = findViewById(R.id.rv_recommendations);
        
        if (layoutRecommendations != null && rvRecommendations != null) {
            rvRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            
            SessionManager sessionManager = new SessionManager(this);
            if (sessionManager.isLoggedIn()) {
                ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
                api.getRecommendations().enqueue(new Callback<List<Recipe>>() {
                    @Override
                    public void onResponse(Call<List<Recipe>> call, Response<List<Recipe>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            layoutRecommendations.setVisibility(View.VISIBLE);
                            RecipeAdapter adapter = new RecipeAdapter(response.body(), r -> {
                                Intent intent = new Intent(MainActivity.this, RecipeDetailActivity.class);
                                intent.putExtra("recipe_id", r.getId());
                                startActivity(intent);
                            }, true); // use compact small layout mode
                            rvRecommendations.setAdapter(adapter);
                        } else {
                            layoutRecommendations.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Recipe>> call, Throwable t) {
                        layoutRecommendations.setVisibility(View.GONE);
                    }
                });
            } else {
                layoutRecommendations.setVisibility(View.GONE);
            }
        }

        // ── AI Chatbot FAB ───────────────────────────────────────────
        View fabSmartFridge = findViewById(R.id.fab_smart_fridge);
        if (fabSmartFridge != null) {
            fabSmartFridge.setOnClickListener(v -> 
                startActivity(new Intent(this, ChatActivity.class)));
        }

        // ── Search bar ────────────────────────────────────────────────────
        TextView etSearch = findViewById(R.id.et_search);
        if (etSearch != null) etSearch.setOnClickListener(v ->
                startActivity(new Intent(this, RecipeListActivity.class)));

        // ── Avatar / notification icons ───────────────────────────────────
        ImageView ivAvatar = findViewById(R.id.iv_avatar);
        if (ivAvatar != null) ivAvatar.setOnClickListener(v ->
                startActivity(new Intent(this, CommunityActivity.class)));

        ImageView ivNotif = findViewById(R.id.iv_notification);
        if (ivNotif != null) ivNotif.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));
    }

    private void startActivityWithNoTransition(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle());
        } else {
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }
}
