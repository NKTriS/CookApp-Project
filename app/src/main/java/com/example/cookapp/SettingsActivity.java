package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.UserDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, 0);
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_settings);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_recipe) {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_shop) {
                    startActivity(new Intent(this, ShopActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_fav) {
                    if (SessionManager.requireLogin(this)) return true;
                    startActivity(new Intent(this, FavoritesActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_settings) return true;
                return false;
            });
        }

        // ── Guest mode: ẩn các mục cần đăng nhập ──
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            // Ẩn user profile section, đổi thành nút Đăng nhập
            TextView tvName = findViewById(R.id.tv_settings_name);
            if (tvName != null) tvName.setText("Khách");
            TextView tvEmail = findViewById(R.id.tv_settings_email);
            if (tvEmail != null) tvEmail.setText("Đăng nhập để sử dụng đầy đủ tính năng");

            // Profile click → Login
            LinearLayout btnProfile = findViewById(R.id.btn_profile);
            if (btnProfile != null)
                btnProfile.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
            LinearLayout btnProfileMenu = findViewById(R.id.btn_profile_menu);
            if (btnProfileMenu != null)
                btnProfileMenu.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));

            // Ẩn các mục cần login
            hideViewById(R.id.btn_my_orders);
            hideViewById(R.id.btn_saved_posts);
            hideViewById(R.id.btn_notifications_menu);

            // Đổi nút Logout thành nút Đăng nhập
            LinearLayout btnLogout = findViewById(R.id.btn_logout);
            if (btnLogout != null) {
                // Đổi background sang màu xanh
                btnLogout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#332AD2C1")));
                // Đổi text và icon
                for (int i = 0; i < btnLogout.getChildCount(); i++) {
                    View child = btnLogout.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setText("ĐĂNG NHẬP");
                        ((TextView) child).setTextColor(android.graphics.Color.parseColor("#2AD2C1"));
                    } else if (child instanceof ImageView) {
                        ((ImageView) child).setImageResource(android.R.drawable.ic_menu_agenda);
                        ((ImageView) child).setColorFilter(android.graphics.Color.parseColor("#2AD2C1"));
                    }
                }
                btnLogout.setOnClickListener(v ->
                    startActivity(new Intent(this, LoginActivity.class)));
            }
            return; // Không load user info
        }

        // ── Logged-in mode (giữ nguyên logic cũ) ──
        LinearLayout btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null)
            btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        LinearLayout btnProfileMenu = findViewById(R.id.btn_profile_menu);
        if (btnProfileMenu != null)
            btnProfileMenu.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        LinearLayout btnMyOrders = findViewById(R.id.btn_my_orders);
        if (btnMyOrders != null)
            btnMyOrders.setOnClickListener(v -> startActivity(new Intent(this, MyOrdersActivity.class)));

        LinearLayout btnCommunity = findViewById(R.id.btn_community);
        if (btnCommunity != null)
            btnCommunity.setOnClickListener(v -> startActivity(new Intent(this, CommunityActivity.class)));

        LinearLayout btnSavedPosts = findViewById(R.id.btn_saved_posts);
        if (btnSavedPosts != null)
            btnSavedPosts.setOnClickListener(v -> startActivity(new Intent(this, SavedPostsActivity.class)));

        LinearLayout btnNotificationsMenu = findViewById(R.id.btn_notifications_menu);
        if (btnNotificationsMenu != null)
            btnNotificationsMenu.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));

        LinearLayout btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new SessionManager(this).logout();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Admin Panel — mặc định ẩn, chỉ hiện khi role = 'admin'
        LinearLayout btnAdmin = findViewById(R.id.btn_admin_panel);
        if (btnAdmin != null) {
            btnAdmin.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminActivity.class)));
        }

        loadUserInfo();
    }

    private void hideViewById(int id) {
        View v = findViewById(id);
        if (v != null) v.setVisibility(View.GONE);
    }

    /**
     * Hiển thị ngay từ cache, sau đó refresh từ API /auth/me
     * Không còn phụ thuộc Room DB.
     */
    private void loadUserInfo() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) return;

        // Hiển thị ngay từ cache (không flicker)
        String cachedName  = session.getCachedUserName();
        String cachedEmail = session.getCachedUserEmail();
        if (!cachedName.isEmpty()) {
            TextView tvName = findViewById(R.id.tv_settings_name);
            if (tvName != null) tvName.setText(cachedName);
        }
        if (!cachedEmail.isEmpty()) {
            TextView tvEmail = findViewById(R.id.tv_settings_email);
            if (tvEmail != null) tvEmail.setText(cachedEmail);
        }

        // Refresh từ API
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getMe().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto user = response.body();
                    session.saveUserProfile(
                        user.id != 0 ? user.id : session.getUserId(),
                        user.fullName, user.email, null
                    );
                    runOnUiThread(() -> {
                        if (user.fullName != null) {
                            TextView tvName = findViewById(R.id.tv_settings_name);
                            if (tvName != null) tvName.setText(user.fullName);
                        }
                        if (user.email != null) {
                            TextView tvEmail = findViewById(R.id.tv_settings_email);
                            if (tvEmail != null) tvEmail.setText(user.email);
                        }

                        // Hiện nút Admin Panel nếu có quyền admin
                        if ("admin".equals(user.role)) {
                            LinearLayout btnAdmin = findViewById(R.id.btn_admin_panel);
                            View dividerAdmin = findViewById(R.id.divider_admin);
                            if (btnAdmin != null) btnAdmin.setVisibility(View.VISIBLE);
                            if (dividerAdmin != null) dividerAdmin.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                // Offline — dùng cache ổn
            }
        });

        apiService.getProfileStats().enqueue(new Callback<com.example.cookapp.api.dto.ProfileStatsDto>() {
            @Override
            public void onResponse(Call<com.example.cookapp.api.dto.ProfileStatsDto> call, Response<com.example.cookapp.api.dto.ProfileStatsDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.cookapp.api.dto.ProfileStatsDto stats = response.body();
                    runOnUiThread(() -> {
                        TextView tvSaved = findViewById(R.id.tv_count_saved);
                        if (tvSaved != null) tvSaved.setText(String.valueOf(stats.getFavorites()));
                        TextView tvReviews = findViewById(R.id.tv_count_reviews);
                        if (tvReviews != null) tvReviews.setText(String.valueOf(stats.getReviews()));
                    });
                }
            }

            @Override
            public void onFailure(Call<com.example.cookapp.api.dto.ProfileStatsDto> call, Throwable t) {}
        });
    }
}
