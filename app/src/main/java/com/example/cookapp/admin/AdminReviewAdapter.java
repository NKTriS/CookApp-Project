package com.example.cookapp.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.Review;

import java.util.List;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.VH> {

    private List<Review> items;
    private final OnDeleteListener listener;

    public interface OnDeleteListener { void onDelete(Review review); }

    public AdminReviewAdapter(List<Review> items, OnDeleteListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<Review> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Review r = items.get(pos);
        h.tvRecipe.setText(r.getRecipeName() != null ? r.getRecipeName() : "Recipe #" + r.getRecipeId());
        h.tvComment.setText(r.getContent() != null && !r.getContent().isEmpty() ? r.getContent() : "—");
        h.tvAuthor.setText(r.getAuthorName() != null ? "— " + r.getAuthorName() : "");

        // Star rating
        int rating = r.getRating();
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) stars.append(i < rating ? "⭐" : "☆");
        h.tvRating.setText(stars.toString());

        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(r); });
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRecipe, tvRating, tvComment, tvAuthor, btnDelete;
        VH(View v) {
            super(v);
            tvRecipe = v.findViewById(R.id.tv_recipe);
            tvRating = v.findViewById(R.id.tv_rating);
            tvComment = v.findViewById(R.id.tv_comment);
            tvAuthor = v.findViewById(R.id.tv_author);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
