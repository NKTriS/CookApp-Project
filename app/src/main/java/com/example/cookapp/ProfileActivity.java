package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.UserDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        ImageView ivSettings = findViewById(R.id.iv_settings);
        if (ivSettings != null)
            ivSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // Navigation cards
        FrameLayout btnMyPosts = findViewById(R.id.btn_my_posts);
        if (btnMyPosts != null)
            btnMyPosts.setOnClickListener(v -> startActivity(new Intent(this, MyPostsActivity.class)));

        FrameLayout btnMyOrders = findViewById(R.id.btn_my_orders);
        if (btnMyOrders != null)
            btnMyOrders.setOnClickListener(v -> startActivity(new Intent(this, MyOrdersActivity.class)));

        FrameLayout btnCommunity = findViewById(R.id.btn_community);
        if (btnCommunity != null)
            btnCommunity.setOnClickListener(v -> startActivity(new Intent(this, CommunityActivity.class)));

        LinearLayout btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                session.logout();
                com.example.cookapp.api.RetrofitClient.reset(); // Xóa token khỏi Retrofit
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Hiển thị cached name ngay lập tức (no flicker)
        String cachedName = session.getCachedUserName();
        String cachedEmail = session.getCachedUserEmail();
        if (!cachedName.isEmpty()) {
            TextView tvName = findViewById(R.id.tv_user_name);
            if (tvName != null) tvName.setText(cachedName);
        }
        // Note: tv_user_email may not exist in all profile layouts — skip safely

        // Load profile thật từ API
        loadProfileFromApi(session);
    }

    /**
     * Load fullName, email từ GET /api/auth/me (server source of truth)
     * Update cache nếu thành công.
     */
    private void loadProfileFromApi(SessionManager session) {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getMe().enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserDto user = response.body();
                    // Cập nhật cache
                    session.saveUserProfile(
                        user.id != 0 ? user.id : session.getUserId(),
                        user.fullName,
                        user.email,
                        null  // token không thay đổi
                    );
                    runOnUiThread(() -> {
                        TextView tvName = findViewById(R.id.tv_user_name);
                        if (tvName != null && user.fullName != null) tvName.setText(user.fullName);
                        // Note: email field may not exist in layout; update cache only
                    });
                }
                // Nếu lỗi — cache đã hiển thị rồi, không crash
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                // Offline — đã dùng cache OK
            }
        });

        apiService.getProfileStats().enqueue(new Callback<com.example.cookapp.api.dto.ProfileStatsDto>() {
            @Override
            public void onResponse(Call<com.example.cookapp.api.dto.ProfileStatsDto> call, Response<com.example.cookapp.api.dto.ProfileStatsDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.example.cookapp.api.dto.ProfileStatsDto stats = response.body();
                    runOnUiThread(() -> {
                        TextView tvPosts = findViewById(R.id.tv_count_posts);
                        if (tvPosts != null) tvPosts.setText(String.valueOf(stats.getPosts()));

                        TextView tvReviews = findViewById(R.id.tv_count_reviews);
                        if (tvReviews != null) tvReviews.setText(String.valueOf(stats.getReviews()));
                    });
                }
            }

            @Override
            public void onFailure(Call<com.example.cookapp.api.dto.ProfileStatsDto> call, Throwable t) {}
        });
    }

    private TextView findTextViewById(int id) {
        try { return findViewById(id); }
        catch (Exception e) { return null; }
    }
}
