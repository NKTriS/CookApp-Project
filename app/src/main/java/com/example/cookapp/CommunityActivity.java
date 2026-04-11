package com.example.cookapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.app.ActivityOptions;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.PostDto;
import com.example.cookapp.repository.CommunityRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityActivity extends AppCompatActivity {

    private CommunityRepository communityRepo;
    private RecyclerView recyclerView;
    private ProgressBar pbLoading;
    private TextView tvEmpty;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rv_posts), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        communityRepo = new CommunityRepository(this);
        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        ImageView btnNewPost = findViewById(R.id.btn_new_post);
        if (btnNewPost != null)
            btnNewPost.setOnClickListener(v -> startActivity(new Intent(CommunityActivity.this, NewPostActivity.class)));

        recyclerView = findViewById(R.id.rv_posts);
        pbLoading    = findViewById(R.id.pb_loading);
        tvEmpty      = findViewById(R.id.tv_empty_posts);

        if (recyclerView != null)
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

        boolean isMyPostsOnly = getIntent().getBooleanExtra("MY_POSTS_ONLY", false);
        if (isMyPostsOnly) {
            TextView tvTitle = findViewById(R.id.tv_header_title);
            if (tvTitle != null) tvTitle.setText("Bài viết của tôi");
            if (btnNewPost != null) btnNewPost.setVisibility(View.GONE);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_recipe);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_recipe) { startActivityNoAnim(new Intent(this, MainActivity.class)); return true; }
                else if (id == R.id.nav_shop)     { startActivityNoAnim(new Intent(this, ShopActivity.class)); return true; }
                else if (id == R.id.nav_fav)      { if (SessionManager.requireLogin(this)) return true; startActivityNoAnim(new Intent(this, FavoritesActivity.class)); return true; }
                else if (id == R.id.nav_settings) { startActivityNoAnim(new Intent(this, SettingsActivity.class)); return true; }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        if (recyclerView == null) return;
        SessionManager session = new SessionManager(this);

        communityRepo.getCommunityPosts(result -> {
            if (result.status == Resource.Status.LOADING) {
                if (pbLoading != null) runOnUiThread(() -> pbLoading.setVisibility(View.VISIBLE));
            } else if (result.status == Resource.Status.SUCCESS && result.data != null) {
                List<PostDto> dtos = result.data;
                List<Post> posts = new ArrayList<>();
                for (PostDto dto : dtos) {
                    posts.add(new Post(
                        String.valueOf(dto.id),
                        dto.author != null ? dto.author : "Tác giả",
                        dto.created_at != null ? dto.created_at : "",
                        (dto.title != null ? dto.title + "\n" : "") + (dto.content != null ? dto.content : ""),
                        R.drawable.img_category4,
                        dto.likes,
                        dto.comments != null ? dto.comments.size() : 0,
                        dto.is_liked_by_me,
                        dto.image_url,
                        dto.is_saved_by_me
                    ));
                }
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    if (tvEmpty != null) tvEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);

                    PostAdapter adapter = new PostAdapter(posts, new PostAdapter.OnPostClickListener() {
                        @Override
                        public void onItemClick(Post post) {
                            Intent intent = new Intent(CommunityActivity.this, PostDetailActivity.class);
                            try { intent.putExtra("post_id", Integer.parseInt(post.getId())); }
                            catch (NumberFormatException ex) { intent.putExtra("post_id", -1); }
                            startActivity(intent);
                        }

                        @Override
                        public void onLikeClick(Post post) {
                            if (!session.isLoggedIn()) {
                                Toast.makeText(CommunityActivity.this, "Vui lòng đăng nhập để thả tim!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                int pid = Integer.parseInt(post.getId());
                                communityRepo.toggleLike(pid, res -> {});
                            } catch (NumberFormatException ignored) {}
                        }

                        @Override
                        public void onSaveClick(Post post) {
                            if (!session.isLoggedIn()) {
                                Toast.makeText(CommunityActivity.this, "Vui lòng đăng nhập để lưu bài viết!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                int pid = Integer.parseInt(post.getId());
                                apiService.toggleSavePost(pid).enqueue(new Callback<GenericResponse>() {
                                    @Override
                                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                                        if (response.isSuccessful()) {
                                            runOnUiThread(() -> Toast.makeText(CommunityActivity.this,
                                                post.isSaved() ? "Đã lưu bài viết 📌" : "Đã bỏ lưu", Toast.LENGTH_SHORT).show());
                                        } else {
                                            runOnUiThread(() -> {
                                                post.setSaved(!post.isSaved());
                                                if (recyclerView != null && recyclerView.getAdapter() != null) {
                                                    recyclerView.getAdapter().notifyDataSetChanged();
                                                }
                                                try {
                                                    Toast.makeText(CommunityActivity.this, "Lỗi: " + response.errorBody().string(), Toast.LENGTH_SHORT).show();
                                                } catch (Exception e) {}
                                            });
                                        }
                                    }
                                    @Override
                                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                                        runOnUiThread(() -> {
                                            post.setSaved(!post.isSaved());
                                            if (recyclerView != null && recyclerView.getAdapter() != null) {
                                                recyclerView.getAdapter().notifyDataSetChanged();
                                            }
                                            Toast.makeText(CommunityActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            } catch (NumberFormatException ignored) {}
                        }
                    });
                    recyclerView.setAdapter(adapter);
                });
            } else if (result.status == Resource.Status.ERROR) {
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải bài viết: " + result.message, Toast.LENGTH_SHORT).show();
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void startActivityNoAnim(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle());
        } else {
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }
}
