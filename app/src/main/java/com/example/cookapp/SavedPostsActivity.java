package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.PostDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavedPostsActivity extends AppCompatActivity {

    private RecyclerView rvSavedPosts;
    private ProgressBar pbLoading;
    private TextView tvEmpty;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_saved_posts);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        View header = findViewById(R.id.header);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                androidx.core.graphics.Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        apiService = RetrofitClient.getClient(this).create(ApiService.class);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        rvSavedPosts = findViewById(R.id.rv_saved_posts);
        pbLoading = findViewById(R.id.pb_loading);
        tvEmpty = findViewById(R.id.tv_empty);

        if (rvSavedPosts != null) {
            rvSavedPosts.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedPosts();
    }

    private void loadSavedPosts() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);

        apiService.getSavedPosts().enqueue(new Callback<List<PostDto>>() {
            @Override
            public void onResponse(Call<List<PostDto>> call, Response<List<PostDto>> response) {
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        List<PostDto> dtos = response.body();
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
                                true // saved
                            ));
                        }

                        if (tvEmpty != null) tvEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);

                        SessionManager session = new SessionManager(SavedPostsActivity.this);
                        PostAdapter adapter = new PostAdapter(posts, new PostAdapter.OnPostClickListener() {
                            @Override
                            public void onItemClick(Post post) {
                                Intent intent = new Intent(SavedPostsActivity.this, PostDetailActivity.class);
                                try { intent.putExtra("post_id", Integer.parseInt(post.getId())); }
                                catch (NumberFormatException ex) { intent.putExtra("post_id", -1); }
                                startActivity(intent);
                            }

                            @Override
                            public void onLikeClick(Post post) {
                                // No-op or handle like
                            }

                            @Override
                            public void onSaveClick(Post post) {
                                if (!session.isLoggedIn()) return;
                                try {
                                    int pid = Integer.parseInt(post.getId());
                                    apiService.toggleSavePost(pid).enqueue(new Callback<GenericResponse>() {
                                        @Override
                                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> resp) {
                                            if (resp.isSuccessful()) {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(SavedPostsActivity.this,
                                                        post.isSaved() ? "Đã lưu lại" : "Đã bỏ lưu", Toast.LENGTH_SHORT).show();
                                                    loadSavedPosts(); // reload to reflect changes
                                                });
                                            }
                                        }
                                        @Override
                                        public void onFailure(Call<GenericResponse> call, Throwable t) {}
                                    });
                                } catch (NumberFormatException ignored) {}
                            }
                        });
                        if (rvSavedPosts != null) rvSavedPosts.setAdapter(adapter);
                    } else {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<PostDto>> call, Throwable t) {
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    Toast.makeText(SavedPostsActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                });
            }
        });
    }
}
