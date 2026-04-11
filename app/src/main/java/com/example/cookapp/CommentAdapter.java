package com.example.cookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentAction {
        void onEdit(Comment comment);
        void onDelete(String commentId);
    }

    private List<Comment> comments;
    private OnCommentAction actionListener;

    public CommentAdapter(List<Comment> comments, OnCommentAction actionListener) {
        this.comments = comments;
        this.actionListener = actionListener;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvDate, tvText;
        LinearLayout llActions, btnEdit, btnDelete;

        public CommentViewHolder(View view) {
            super(view);
            tvAuthor = view.findViewById(R.id.tv_author);
            tvDate = view.findViewById(R.id.tv_date);
            tvText = view.findViewById(R.id.tv_text);
            llActions = view.findViewById(R.id.ll_actions);
            btnEdit = view.findViewById(R.id.btn_edit);
            btnDelete = view.findViewById(R.id.btn_delete);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.tvAuthor.setText(comment.getAuthor());

        // Format relative time
        String timeText = ReviewAdapter.formatRelativeTime(comment.getDate());
        holder.tvDate.setText(timeText != null ? timeText : (comment.getDate() != null ? comment.getDate() : ""));

        holder.tvText.setText(comment.getText());

        if (comment.isOwn()) {
            holder.llActions.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onEdit(comment);
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDelete(comment.getId());
            });
        } else {
            holder.llActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void updateComments(List<Comment> newComments) {
        this.comments = newComments;
        notifyDataSetChanged();
    }
}
