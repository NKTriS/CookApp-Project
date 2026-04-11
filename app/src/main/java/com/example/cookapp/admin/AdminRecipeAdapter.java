package com.example.cookapp.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cookapp.R;
import com.example.cookapp.Recipe;

import java.util.List;

public class AdminRecipeAdapter extends RecyclerView.Adapter<AdminRecipeAdapter.VH> {

    private List<Recipe> items;
    private final AdminRecipeListener listener;

    public interface AdminRecipeListener { 
        void onDelete(Recipe recipe); 
        void onEditTime(Recipe recipe);
    }

    public AdminRecipeAdapter(List<Recipe> items, AdminRecipeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<Recipe> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_recipe, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Recipe r = items.get(pos);
        h.tvTitle.setText(r.getTitle() != null ? r.getTitle() : "—");
        h.tvCategory.setText(r.getCategoryName() != null ? r.getCategoryName() : "—");
        h.tvTime.setText(r.getTime() != null ? r.getTime() + " phút" : "—");

        String imgUrl = r.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            Glide.with(h.itemView.getContext()).load(imgUrl)
                    .centerCrop().into(h.ivThumb);
        }

        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(r); });
        h.btnEditTime.setOnClickListener(v -> { if (listener != null) listener.onEditTime(r); });
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTitle, tvCategory, tvTime, btnDelete, btnEditTime;
        VH(View v) {
            super(v);
            ivThumb = v.findViewById(R.id.iv_thumb);
            tvTitle = v.findViewById(R.id.tv_title);
            tvCategory = v.findViewById(R.id.tv_category);
            tvTime = v.findViewById(R.id.tv_time);
            btnDelete = v.findViewById(R.id.btn_delete);
            btnEditTime = v.findViewById(R.id.btn_edit_time);
        }
    }
}
