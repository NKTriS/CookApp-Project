package com.example.cookapp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.PostDto;
import com.example.cookapp.repository.CommunityRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {

    private RecyclerView rvComments;
    private EditText etComment;
    private CommentAdapter adapter;
    private final List<Comment> commentsList = new ArrayList<>();
    private Comment editingComment = null;

    private int likesCount   = 0;
    private int currentPostId = -1;
    private ImageView ivHeart;
    private ImageView ivBookmark;
    private TextView tvLikes;
    private boolean isSaved = false;

    private CommunityRepository communityRepo;
    private ApiService apiService;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), sb.bottom);
            return insets;
        });

        session       = new SessionManager(this);
        communityRepo = new CommunityRepository(this);
        apiService    = RetrofitClient.getClient(this).create(ApiService.class);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        ivBookmark = findViewById(R.id.iv_bookmark);
        if (ivBookmark != null) {
            ivBookmark.setOnClickListener(v -> toggleSave());
        }

        rvComments = findViewById(R.id.rv_comments);
        etComment  = findViewById(R.id.et_comment);

        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
        }

        adapter = new CommentAdapter(commentsList, new CommentAdapter.OnCommentAction() {
            @Override
            public void onEdit(Comment comment) {
                editingComment = comment;
                if (etComment != null) {
                    etComment.setText(comment.getText());
                    etComment.setSelection(comment.getText().length());
                    etComment.requestFocus();
                }
            }
            @Override
            public void onDelete(String commentId) {
                deleteComment(commentId);
            }
        });

        if (rvComments != null) rvComments.setAdapter(adapter);

        currentPostId = getIntent().getIntExtra("post_id", -1);

        // Send comment
        LinearLayout btnSendComment = findViewById(R.id.btn_send_comment);
        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> {
                if (!session.isLoggedIn()) {
                    Toast.makeText(this, "Vui lòng đăng nhập để bình luận!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentPostId == -1) return;
                String text = etComment != null ? etComment.getText().toString().trim() : "";
                if (!text.isEmpty()) {
                    if (editingComment != null) {
                        updateComment(editingComment, text);
                    } else {
                        postComment(text);
                    }
                }
            });
        }

        if (currentPostId != -1) {
            loadPostFromApi();
        }

        ivHeart = findViewById(R.id.iv_heart);
        tvLikes = findViewById(R.id.tv_likes);

        LinearLayout btnLike = findViewById(R.id.btn_like);
        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                if (!session.isLoggedIn()) {
                    Toast.makeText(this, "Vui lòng đăng nhập để thích!", Toast.LENGTH_SHORT).show();
                    return;
                }
                communityRepo.toggleLike(currentPostId, result -> {
                    runOnUiThread(() -> {
                        if (result.status == Resource.Status.SUCCESS && result.data != null) {
                            com.example.cookapp.api.dto.ToggleLikeResponse resp = result.data;
                            likesCount = resp.getLikesCount();
                            if (tvLikes != null) tvLikes.setText(String.valueOf(likesCount));
                            if (ivHeart != null) {
                                if (resp.isLiked()) {
                                    ivHeart.setColorFilter(Color.parseColor("#FF383C"));
                                } else {
                                    ivHeart.clearColorFilter();
                                }
                            }
                        } else {
                            Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOAD POST FROM API
    // ─────────────────────────────────────────────────────────────────────

    private void loadPostFromApi() {
        apiService.getPostDetail(currentPostId).enqueue(new Callback<PostDto>() {
            @Override
            public void onResponse(Call<PostDto> call, Response<PostDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostDto post = response.body();
                    runOnUiThread(() -> bindPost(post));
                } else {
                    runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                        "Không tải được bài viết", Toast.LENGTH_SHORT).show());
                }
            }
            @Override
            public void onFailure(Call<PostDto> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                    "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void bindPost(PostDto post) {
        TextView tvTitle   = findViewById(R.id.tv_post_title);
        TextView tvContent = findViewById(R.id.tv_post_content);
        TextView tvAuthor  = findViewById(R.id.tv_post_author);
        TextView tvTime    = findViewById(R.id.tv_post_time);
        ImageView ivPostImage = findViewById(R.id.iv_post_image);

        if (tvTitle   != null) tvTitle.setText(post.title);
        if (tvContent != null) tvContent.setText(post.content);
        if (tvAuthor  != null) tvAuthor.setText(post.author != null ? post.author : "Tác giả");

        // Format relative time
        if (tvTime != null) {
            String relTime = ReviewAdapter.formatRelativeTime(post.created_at);
            tvTime.setText(relTime != null ? relTime : (post.created_at != null ? post.created_at : ""));
        }

        if (ivPostImage != null && post.image_url != null && !post.image_url.isEmpty()) {
            Glide.with(this).load(post.image_url)
                .placeholder(R.drawable.img_category4)
                .error(R.drawable.img_category4)
                .into(ivPostImage);
        }

        likesCount = post.likes;
        if (tvLikes != null) tvLikes.setText(String.valueOf(likesCount));

        // Tô màu tim nếu user đã like
        if (ivHeart != null) {
            if (post.is_liked_by_me) {
                ivHeart.setColorFilter(android.graphics.Color.parseColor("#FF383C"));
            } else {
                ivHeart.clearColorFilter();
            }
        }

        // Bookmark state
        isSaved = post.is_saved_by_me;
        updateBookmarkIcon();

        // Load comments từ post response — xác định quyền sở hữu
        int currentUserId = session.getUserId();
        if (post.comments != null && !post.comments.isEmpty()) {
            commentsList.clear();
            for (PostDto.CommentDto c : post.comments) {
                boolean isOwn = (currentUserId != -1 && c.user_id == currentUserId);
                commentsList.add(new Comment(
                    String.valueOf(c.id),
                    c.author != null ? c.author : "Người dùng",
                    c.created_at != null ? c.created_at : "",
                    c.content,
                    isOwn
                ));
            }
            adapter.notifyDataSetChanged();
            updateCommentCount();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST COMMENT VIA API
    // ─────────────────────────────────────────────────────────────────────

    private void postComment(String text) {
        PostDto.CommentDto dto = new PostDto.CommentDto();
        dto.content = text;
        dto.author  = session.getCachedUserName();

        apiService.addComment(currentPostId, dto).enqueue(new Callback<PostDto.CommentDto>() {
            @Override
            public void onResponse(Call<PostDto.CommentDto> call, Response<PostDto.CommentDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostDto.CommentDto saved = response.body();
                    Comment newComment = new Comment(
                        String.valueOf(saved.id),
                        saved.author != null ? saved.author : session.getCachedUserName(),
                        saved.created_at != null ? saved.created_at : "Vừa xong",
                        saved.content,
                        true
                    );
                    runOnUiThread(() -> {
                        if (etComment != null) etComment.getText().clear();
                        commentsList.add(newComment);
                        adapter.notifyItemInserted(commentsList.size() - 1);
                        if (rvComments != null) rvComments.scrollToPosition(commentsList.size() - 1);
                        updateCommentCount();
                        Toast.makeText(PostDetailActivity.this, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                        "Lỗi gửi bình luận", Toast.LENGTH_SHORT).show());
                }
            }
            @Override
            public void onFailure(Call<PostDto.CommentDto> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                    "Mất kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateComment(Comment comment, String text) {
        PostDto.CommentDto dto = new PostDto.CommentDto();
        dto.content = text;

        int commentId = Integer.parseInt(comment.getId());
        apiService.updateComment(commentId, dto).enqueue(new Callback<PostDto.CommentDto>() {
            @Override
            public void onResponse(Call<PostDto.CommentDto> call, Response<PostDto.CommentDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostDto.CommentDto updatedDto = response.body();
                    runOnUiThread(() -> {
                        if (etComment != null) etComment.getText().clear();
                        editingComment = null; // Clear edit mode
                        
                        // Find and update comment in list
                        for (int i = 0; i < commentsList.size(); i++) {
                            if (commentsList.get(i).getId().equals(comment.getId())) {
                                commentsList.get(i).setText(updatedDto.content);
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                        Toast.makeText(PostDetailActivity.this, "Đã cập nhật bình luận!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                        "Lỗi cập nhật bình luận", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<PostDto.CommentDto> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                    "Mất kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void deleteComment(String commentId) {
        int idVal = Integer.parseInt(commentId);
        apiService.deleteComment(idVal).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        commentsList.removeIf(c -> c.getId().equals(commentId));
                        adapter.notifyDataSetChanged();
                        updateCommentCount();
                        Toast.makeText(PostDetailActivity.this, "Đã xóa bình luận!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                        "Không thể xóa bình luận", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                    "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateCommentCount() {
        TextView tv = findViewById(R.id.tv_comment_count);
        if (tv != null) tv.setText(String.valueOf(commentsList.size()));
    }

    private void updateBookmarkIcon() {
        if (ivBookmark == null) return;
        if (isSaved) {
            ivBookmark.setImageResource(R.drawable.ic_bookmark_filled);
            ivBookmark.setColorFilter(Color.parseColor("#FF6B35"));
        } else {
            ivBookmark.setImageResource(R.drawable.ic_bookmark_outline);
            ivBookmark.setColorFilter(Color.parseColor("#1A1A1A"));
        }
    }

    private void toggleSave() {
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu bài viết!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentPostId == -1) return;

        // Optimistic update
        isSaved = !isSaved;
        updateBookmarkIcon();

        apiService.toggleSavePost(currentPostId).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(PostDetailActivity.this,
                        isSaved ? "Đã lưu bài viết" : "Đã bỏ lưu", Toast.LENGTH_SHORT).show());
                } else {
                    // Revert
                    isSaved = !isSaved;
                    runOnUiThread(() -> {
                        updateBookmarkIcon();
                        try {
                            Toast.makeText(PostDetailActivity.this, "Lỗi: " + response.errorBody().string(), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {}
                    });
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                isSaved = !isSaved;
                runOnUiThread(() -> {
                    updateBookmarkIcon();
                    Toast.makeText(PostDetailActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
