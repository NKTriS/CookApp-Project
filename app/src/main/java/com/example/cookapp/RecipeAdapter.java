package com.example.cookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.utils.image.GlideHelper;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onItemClick(Recipe recipe);
    }

    private List<Recipe> recipes;
    private OnRecipeClickListener listener;
    private boolean isCompact;

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
        this.isCompact = false;
    }

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener, boolean isCompact) {
        this.recipes = recipes;
        this.listener = listener;
        this.isCompact = isCompact;
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipe;
        TextView tvTitle, tvCalories, tvTime, tvCategory, tvDifficulty, tvMatchPercentage;

        public RecipeViewHolder(View view) {
            super(view);
            ivRecipe     = view.findViewById(R.id.iv_recipe);
            tvTitle      = view.findViewById(R.id.tv_recipe_title);
            tvCalories   = view.findViewById(R.id.tv_calories);
            tvTime       = view.findViewById(R.id.tv_recipe_time);
            tvCategory   = view.findViewById(R.id.tv_category);
            tvDifficulty = view.findViewById(R.id.tv_difficulty);
            tvMatchPercentage = view.findViewById(R.id.tv_match_percentage);
        }
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        
        if (isCompact) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null) {
                params.width = (int) (parent.getContext().getResources().getDisplayMetrics().widthPixels * 0.75);
                view.setLayoutParams(params);
            }
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).setMarginEnd(32);
            }
        }
        
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.tvTitle.setText(recipe.getTitle());

        // Hiển thị % phù hợp (AI-2)
        if (holder.tvMatchPercentage != null) {
            Integer matchPct = recipe.getMatchPercentage();
            if (matchPct != null) {
                holder.tvMatchPercentage.setVisibility(View.VISIBLE);
                holder.tvMatchPercentage.setText("✨ Có sẵn " + matchPct + "% nguyên liệu");
                
                // Đổi màu tuỳ theo %
                if (matchPct >= 80) holder.tvMatchPercentage.setTextColor(android.graphics.Color.parseColor("#388E3C")); // Xanh lá đậm
                else if (matchPct >= 40) holder.tvMatchPercentage.setTextColor(android.graphics.Color.parseColor("#F57C00")); // Cam
                else holder.tvMatchPercentage.setTextColor(android.graphics.Color.parseColor("#D32F2F")); // Đỏ (ít trùng khớp)
            } else {
                holder.tvMatchPercentage.setVisibility(View.GONE);
            }
        }

        // Calories — hiển thị số + kcal
        String cal = recipe.getCalories();
        if (cal != null && !cal.isEmpty() && !cal.equals("0")) {
            try {
                int c = (int) Double.parseDouble(cal);
                holder.tvCalories.setText(c + " kcal");
            } catch (NumberFormatException e) {
                holder.tvCalories.setText(cal + " kcal");
            }
        } else {
            holder.tvCalories.setText("— kcal");
        }

        // Cook time — hiển thị số + phút
        String t = recipe.getTime();
        if (t != null && !t.isEmpty() && !t.equals("0")) {
            try {
                int min = (int) Double.parseDouble(t);
                holder.tvTime.setText("⏱ " + min + " phút");
            } catch (NumberFormatException e) {
                holder.tvTime.setText("⏱ " + t + " phút");
            }
        } else {
            holder.tvTime.setText("— phút");
        }

        // Danh mục — hiển thị tất cả, cách nhau bằng " · "
        if (holder.tvCategory != null) {
            String cat = recipe.getCategoriesString();
            holder.tvCategory.setText(cat != null && !cat.isEmpty() ? cat : "");
        }

        // Độ khó
        if (holder.tvDifficulty != null) {
            String diff = recipe.getDifficulty();
            holder.tvDifficulty.setText(diff != null && !diff.isEmpty() ? diff : "");
        }

        GlideHelper.loadRecipe(holder.itemView.getContext(), recipe.getImageUrl(), holder.ivRecipe);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(recipe);
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }
}
