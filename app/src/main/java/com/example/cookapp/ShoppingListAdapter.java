package com.example.cookapp;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.data.local.entity.ShoppingListItemEntity;

import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {

    private final List<ShoppingListItemEntity> items;
    private final OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onCheckChanged(ShoppingListItemEntity item, boolean isChecked);
        void onQuantityChanged(ShoppingListItemEntity item, int delta); // +1 or -1
        void onDelete(ShoppingListItemEntity item);
    }

    public ShoppingListAdapter(List<ShoppingListItemEntity> items, OnItemInteractionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingListItemEntity item = items.get(position);
        
        holder.tvItemName.setText(item.ingredient_name);
        
        // Format quantity
        String qtyFormated = (item.quantity == (int)item.quantity) 
                ? (int)item.quantity + " " + item.unit 
                : String.format(java.util.Locale.US, "%.1f %s", item.quantity, item.unit);
        holder.tvItemQty.setText(qtyFormated);
        
        holder.cbPurchased.setOnCheckedChangeListener(null); // Prevent recycling trigger
        holder.cbPurchased.setChecked(item.checked);
        
        if (item.checked) {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvItemName.setTextColor(Color.GRAY);
        } else {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvItemName.setTextColor(Color.BLACK);
        }

        holder.cbPurchased.setOnCheckedChangeListener((btnView, isChecked) -> {
            listener.onCheckChanged(item, isChecked);
        });

        holder.btnQtyMinus.setOnClickListener(v -> listener.onQuantityChanged(item, -1));
        holder.btnQtyPlus.setOnClickListener(v -> listener.onQuantityChanged(item, 1));
        holder.btnDeleteItem.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbPurchased;
        TextView tvItemName, tvItemQty;
        TextView btnQtyMinus, btnQtyPlus;
        ImageView btnDeleteItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbPurchased = itemView.findViewById(R.id.cb_purchased);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvItemQty = itemView.findViewById(R.id.tv_item_qty);
            btnQtyMinus = itemView.findViewById(R.id.btn_qty_minus);
            btnQtyPlus = itemView.findViewById(R.id.btn_qty_plus);
            btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}
