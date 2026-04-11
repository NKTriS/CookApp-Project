package com.example.cookapp.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.dto.PostDto;

import java.util.List;

public class AdminPostAdapter extends RecyclerView.Adapter<AdminPostAdapter.VH> {

    private List<PostDto> items;
    private final OnDeleteListener listener;

    public interface OnDeleteListener { void onDelete(PostDto post); }

    public AdminPostAdapter(List<PostDto> items, OnDeleteListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<PostDto> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_post, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        PostDto p = items.get(pos);
        h.tvTitle.setText(p.title != null ? p.title : "—");
        h.tvAuthor.setText(p.author != null ? p.author : "—");
        h.tvLikes.setText(String.valueOf(p.likes));
        int commentCount = p.comments != null ? p.comments.size() : 0;
        h.tvComments.setText(String.valueOf(commentCount));

        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(p); });
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvLikes, tvComments, btnDelete;
        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_title);
            tvAuthor = v.findViewById(R.id.tv_author);
            tvLikes = v.findViewById(R.id.tv_likes);
            tvComments = v.findViewById(R.id.tv_comments);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
