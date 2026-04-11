package com.example.cookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DecimalFormat;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> items;
    private final Runnable onTotalChanged;
    private final DecimalFormat fmt = new DecimalFormat("#,###");

    public CartAdapter(List<CartItem> items, Runnable onTotalChanged) {
        this.items = items;
        this.onTotalChanged = onTotalChanged;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUnitPrice, tvUnit, tvSubtotal, tvQty;
        FrameLayout btnMinus, btnPlus;
        ImageView btnTrash;

        public CartViewHolder(View view) {
            super(view);
            tvName      = view.findViewById(R.id.tv_cart_item_name);
            tvUnit      = view.findViewById(R.id.tv_cart_item_unit);
            tvUnitPrice = view.findViewById(R.id.tv_cart_item_unit_price);
            tvSubtotal  = view.findViewById(R.id.tv_cart_item_price);
            tvQty       = view.findViewById(R.id.tv_qty);
            btnMinus    = view.findViewById(R.id.btn_minus);
            btnPlus     = view.findViewById(R.id.btn_plus);
            btnTrash    = view.findViewById(R.id.btn_trash);
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);

        // Tên nguyên liệu
        if (holder.tvName != null) holder.tvName.setText(item.getName());

        // Đơn vị — ẩn dòng nếu trống
        String unit = item.getUnit();
        boolean hasUnit = unit != null && !unit.trim().isEmpty();
        if (holder.tvUnit != null) {
            if (hasUnit) {
                holder.tvUnit.setText("Đơn vị: " + unit);
                holder.tvUnit.setVisibility(View.VISIBLE);
            } else {
                holder.tvUnit.setVisibility(View.GONE);
            }
        }

        // Giá đơn vị — ẩn nếu = 0
        if (holder.tvUnitPrice != null) {
            if (item.getPrice() > 0) {
                holder.tvUnitPrice.setText(formatVnd(item.getPrice()) + " đ / đơn vị");
                holder.tvUnitPrice.setVisibility(View.VISIBLE);
            } else {
                holder.tvUnitPrice.setVisibility(View.GONE);
            }
        }

        // Số lượng
        if (holder.tvQty != null) holder.tvQty.setText(String.valueOf(item.getQty()));

        // Thành tiền
        if (holder.tvSubtotal != null) {
            if (item.getPrice() > 0) {
                holder.tvSubtotal.setText(formatVnd(item.getSubtotal()) + " đ");
            } else {
                holder.tvSubtotal.setText("—");
            }
        }

        // Nút − : giảm qty, nếu = 0 thì xóa item
        if (holder.btnMinus != null) {
            holder.btnMinus.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos < 0) return;
                if (item.getQty() > 1) {
                    item.setQty(item.getQty() - 1);
                    notifyItemChanged(pos);
                } else {
                    items.remove(pos);
                    notifyItemRemoved(pos);
                }
                if (onTotalChanged != null) onTotalChanged.run();
            });
        }

        // Nút +
        if (holder.btnPlus != null) {
            holder.btnPlus.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos < 0) return;
                item.setQty(item.getQty() + 1);
                notifyItemChanged(pos);
                if (onTotalChanged != null) onTotalChanged.run();
            });
        }

        // Nút xóa
        if (holder.btnTrash != null) {
            holder.btnTrash.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos < 0) return;
                items.remove(pos);
                notifyItemRemoved(pos);
                if (onTotalChanged != null) onTotalChanged.run();
            });
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    public List<CartItem> getItems() { return items; }

    private String formatVnd(int dong) {
        if (dong <= 0) return "0";
        return fmt.format(dong).replace(",", ".");
    }
}
