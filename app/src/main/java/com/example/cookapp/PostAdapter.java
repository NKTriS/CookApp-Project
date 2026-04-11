package com.example.cookapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onItemClick(Post post);
        void onLikeClick(Post post);
        void onSaveClick(Post post);
    }

    private List<Post> posts;
    private OnPostClickListener listener;

    public PostAdapter(List<Post> posts, OnPostClickListener listener) {
        this.posts = posts;
        this.listener = listener;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvTimeAgo, tvContent, tvLikeCount, tvCommentCount;
        ImageView ivPostImage, ivLikeIcon, ivBookmark;
        LinearLayout btnLike;

        public PostViewHolder(View view) {
            super(view);
            tvAuthor = view.findViewById(R.id.tv_author_name);
            tvTimeAgo = view.findViewById(R.id.tv_time_ago);
            tvContent = view.findViewById(R.id.tv_post_content);
            ivPostImage = view.findViewById(R.id.iv_post_image);
            tvLikeCount = view.findViewById(R.id.tv_like_count);
            tvCommentCount = view.findViewById(R.id.tv_comment_count);
            ivLikeIcon = view.findViewById(R.id.iv_like_icon);
            btnLike = view.findViewById(R.id.btn_like);
            ivBookmark = view.findViewById(R.id.iv_bookmark);
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.tvAuthor.setText(post.getAuthor());

        // Format time as relative
        String timeText = ReviewAdapter.formatRelativeTime(post.getTimeAgo());
        holder.tvTimeAgo.setText(timeText != null ? timeText : (post.getTimeAgo() != null ? post.getTimeAgo() : ""));

        holder.tvContent.setText(post.getContent());

        if (post.getImageUri() != null && !post.getImageUri().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                 .load(post.getImageUri())
                 .placeholder(R.drawable.img_category4)
                 .error(R.drawable.img_category4)
                 .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setImageResource(post.getImgResId());
        }
        holder.tvLikeCount.setText(String.valueOf(post.getLikes()));
        holder.tvCommentCount.setText(String.valueOf(post.getComments()));

        if (post.isLiked()) {
            holder.ivLikeIcon.setColorFilter(Color.parseColor("#FF383C"));
        } else {
            holder.ivLikeIcon.setColorFilter(Color.parseColor("#1C1B1F"));
        }

        // Bookmark state
        if (holder.ivBookmark != null) {
            if (post.isSaved()) {
                holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled);
                holder.ivBookmark.setColorFilter(Color.parseColor("#FF6B35"));
            } else {
                holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline);
                holder.ivBookmark.setColorFilter(Color.parseColor("#1C1B1F"));
            }
            holder.ivBookmark.setOnClickListener(v -> {
                post.setSaved(!post.isSaved());
                notifyItemChanged(position);
                if (listener != null) listener.onSaveClick(post);
            });
        }

        holder.btnLike.setOnClickListener(v -> {
            post.setLiked(!post.isLiked());
            post.setLikes(post.isLiked() ? post.getLikes() + 1 : post.getLikes() - 1);
            notifyItemChanged(position);
            if (listener != null) listener.onLikeClick(post);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(post);
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
