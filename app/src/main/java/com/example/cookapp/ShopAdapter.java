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

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    public interface OnProductClickListener {
        void onItemClick(Product product);
    }

    private final List<Product> products;
    private final OnProductClickListener listener;

    public ShopAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    public static class ShopViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvTitle, tvUnit, tvPrice, tvStoreBadge, tvRating;

        public ShopViewHolder(View view) {
            super(view);
            ivProduct    = view.findViewById(R.id.iv_product);
            tvTitle      = view.findViewById(R.id.tv_product_title);
            tvUnit       = view.findViewById(R.id.tv_product_unit);
            tvPrice      = view.findViewById(R.id.tv_product_price);
            tvStoreBadge = view.findViewById(R.id.tv_store_badge);   // nullable
            tvRating     = view.findViewById(R.id.tv_product_rating); // nullable
        }
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        Product product = products.get(position);

        holder.tvTitle.setText(product.getName());
        holder.tvUnit.setText(product.getUnit().isEmpty() ? "" : product.getUnit());
        holder.tvPrice.setText(product.getPriceText());

        // Store badge
        if (holder.tvStoreBadge != null) {
            String store = product.getStoreName();
            holder.tvStoreBadge.setVisibility(store.isEmpty() ? View.GONE : View.VISIBLE);
            holder.tvStoreBadge.setText(store);
        }

        // Rating stars
        if (holder.tvRating != null) {
            float r = product.getRating();
            if (r > 0) {
                holder.tvRating.setVisibility(View.VISIBLE);
                holder.tvRating.setText(String.format(java.util.Locale.getDefault(), "⭐ %.1f", r));
            } else {
                holder.tvRating.setVisibility(View.GONE);
            }
        }

        // Product image via Glide
        GlideHelper.loadIngredient(holder.ivProduct.getContext(), product.getImageUrl(), holder.ivProduct);

        // Click → add to cart
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(product);
        });
    }

    @Override
    public int getItemCount() { return products.size(); }
}
