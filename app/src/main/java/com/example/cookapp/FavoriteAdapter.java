package com.example.cookapp;

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

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    public interface OnFavoriteClickListener {
        void onDeleteClick(FavoriteRecipe recipe);
        void onItemClick(FavoriteRecipe recipe);
    }

    private final List<FavoriteRecipe> recipes;
    private final OnFavoriteClickListener listener;

    public FavoriteAdapter(List<FavoriteRecipe> recipes, OnFavoriteClickListener listener) {
        this.recipes  = recipes;
        this.listener = listener;
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeImage;
        TextView tvRecipeName;
        LinearLayout btnDelete;

        public FavoriteViewHolder(View view) {
            super(view);
            ivRecipeImage = view.findViewById(R.id.iv_recipe_img);
            tvRecipeName  = view.findViewById(R.id.tv_recipe_name);
            btnDelete     = view.findViewById(R.id.btn_delete);
        }
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FavoriteRecipe recipe = recipes.get(position);
        holder.tvRecipeName.setText(recipe.getTitle());

        // Load ảnh từ URL nếu có, fallback sang res drawable
        String url = recipe.getImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(url)
                .placeholder(recipe.getImgResId())
                .error(recipe.getImgResId())
                .into(holder.ivRecipeImage);
        } else {
            holder.ivRecipeImage.setImageResource(recipe.getImgResId());
        }

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(recipe);
            });
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(recipe);
        });
    }

    @Override
    public int getItemCount() { return recipes.size(); }
}
