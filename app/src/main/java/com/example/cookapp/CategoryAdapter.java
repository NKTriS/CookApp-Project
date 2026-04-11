package com.example.cookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.cookapp.data.local.entity.CategoryEntity;
import com.example.cookapp.utils.image.GlideHelper;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<CategoryEntity> categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryEntity category);
    }

    public CategoryAdapter(List<CategoryEntity> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBg;
        TextView tvName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBg = itemView.findViewById(R.id.iv_category_bg);
            tvName = itemView.findViewById(R.id.tv_category_name);
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryEntity category = categories.get(position);
        holder.tvName.setText(category.name);

        String url = category.imageUrl;
        GlideHelper.loadCategory(holder.ivBg.getContext(), url, holder.ivBg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }
}
