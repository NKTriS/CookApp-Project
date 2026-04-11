package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.PostDto;
import com.example.cookapp.repository.CommunityRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * MyPostsActivity — Hiển thị bài viết của user đang đăng nhập.
 * Dữ liệu lấy từ GET /api/community/posts/mine (API-based, không dùng Room).
 */
public class MyPostsActivity extends AppCompatActivity {

    private MyPostDtoAdapter adapter;
    private final List<PostDto> posts = new ArrayList<>();
    private CommunityRepository communityRepo;
    private ProgressBar pbLoading;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        communityRepo = new CommunityRepository(this);
        pbLoading = findViewById(R.id.pb_loading);
        tvEmpty   = findViewById(R.id.tv_empty_posts);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_my_posts);
        if (rv != null) {
            adapter = new MyPostDtoAdapter(posts, post -> {
                Intent intent = new Intent(this, PostDetailActivity.class);
                intent.putExtra("post_id", post.id);
                startActivity(intent);
            });
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyPosts();
    }

    private void loadMyPosts() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        communityRepo.getMyPosts(result -> {
            runOnUiThread(() -> {
                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                if (result.status == Resource.Status.SUCCESS && result.data != null) {
                    posts.clear();
                    posts.addAll(result.data);
                    if (adapter != null) adapter.notifyDataSetChanged();
                    boolean empty = posts.isEmpty();
                    if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                } else if (result.status == Resource.Status.ERROR) {
                    Toast.makeText(this, "Lỗi tải bài viết: " + result.message, Toast.LENGTH_SHORT).show();
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    // ── Inner Adapter (dùng PostDto thay vì PostEntity Room) ─────────────────
    interface OnPostClickListener { void onClick(PostDto post); }

    static class MyPostDtoAdapter extends RecyclerView.Adapter<MyPostDtoAdapter.VH> {
        private final List<PostDto> list;
        private final OnPostClickListener onClick;

        MyPostDtoAdapter(List<PostDto> list, OnPostClickListener onClick) {
            this.list    = list;
            this.onClick = onClick;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_post, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            PostDto p = list.get(pos);
            if (h.tvTitle != null)   h.tvTitle.setText(p.title != null ? p.title : "");
            if (h.tvContent != null) h.tvContent.setText(p.content != null ? p.content : "");
            if (h.tvDate != null)    h.tvDate.setText(p.created_at != null ? p.created_at : "");
            if (h.tvLikes != null)   h.tvLikes.setText("❤️ " + p.likes);
            // Hide delete button (users can't delete via this screen for now)
            if (h.btnDelete != null) h.btnDelete.setVisibility(View.GONE);
            h.itemView.setOnClickListener(v -> onClick.onClick(p));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvDate, tvLikes, btnDelete;
            VH(View v) {
                super(v);
                tvTitle   = v.findViewById(R.id.tv_post_title);
                tvContent = v.findViewById(R.id.tv_post_content);
                tvDate    = v.findViewById(R.id.tv_post_date);
                tvLikes   = v.findViewById(R.id.tv_post_likes);
                btnDelete = v.findViewById(R.id.btn_delete_post);
            }
        }
    }
}
