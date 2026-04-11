package com.example.cookapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    private final List<Store> stores;
    private int selectedPosition = -1;

    public StoreAdapter(List<Store> stores) {
        this.stores = stores;
    }

    public static class StoreViewHolder extends RecyclerView.ViewHolder {
        CardView rootCard;
        TextView tvName, tvTypeIcon, tvStatus, tvType, tvDistance, tvAddress, tvStars, tvRating;
        FrameLayout flTypeIconBg;
        LinearLayout llActions;

        public StoreViewHolder(View view) {
            super(view);
            rootCard = (CardView) view;
            tvName     = view.findViewById(R.id.tv_store_name);
            tvTypeIcon = view.findViewById(R.id.tv_type_icon);
            tvStatus   = view.findViewById(R.id.tv_status);
            tvType     = view.findViewById(R.id.tv_store_type);
            tvDistance = view.findViewById(R.id.tv_distance);
            tvAddress  = view.findViewById(R.id.tv_address);
            tvStars    = view.findViewById(R.id.tv_stars);
            tvRating   = view.findViewById(R.id.tv_rating);
            flTypeIconBg = view.findViewById(R.id.fl_type_icon_bg);
            llActions  = view.findViewById(R.id.ll_actions);
        }
    }

    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        Store store = stores.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(store.getName());
        holder.tvDistance.setText(store.getDistance());
        holder.tvAddress.setText(store.getAddress());
        holder.tvType.setText(store.getType());
        holder.tvRating.setText("(" + store.getRating() + ")");

        if (store.isOpen()) {
            holder.tvStatus.setText("Mở cửa");
            holder.tvStatus.setTextColor(Color.parseColor("#14AE5C"));
        } else {
            holder.tvStatus.setText("Đóng cửa");
            holder.tvStatus.setTextColor(Color.parseColor("#FF383C"));
        }

        // Stars render
        int full = (int) store.getRating();
        boolean half = (store.getRating() % 1) >= 0.5;
        StringBuilder starsStr = new StringBuilder();
        for (int i = 0; i < full; i++) starsStr.append("★");
        if (half) starsStr.append("½");
        int empty = 5 - full - (half ? 1 : 0);
        for (int i = 0; i < empty; i++) starsStr.append("☆");
        holder.tvStars.setText(starsStr.toString());

        boolean isSelected = (position == selectedPosition);
        holder.llActions.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.rootCard.setCardBackgroundColor(
            isSelected ? Color.parseColor("#FFF0F0") : Color.parseColor("#F9F9F9"));

        // ── Map Intent button ─────────────────────────────────────────
        if (isSelected && holder.llActions != null) {
            // Gắn listener cho button "Xem bản đồ" nếu có trong layout
            View btnMap = holder.llActions.findViewWithTag("btn_map");
            if (btnMap == null) {
                // Tạo button nếu chưa có trong layout
                Button mapBtn = new Button(context);
                mapBtn.setTag("btn_map");
                mapBtn.setText("📍 Xem bản đồ");
                mapBtn.setTextColor(Color.WHITE);
                mapBtn.setBackgroundColor(Color.parseColor("#FF6B35"));
                int p = (int)(8 * context.getResources().getDisplayMetrics().density);
                mapBtn.setPadding(p * 2, p, p * 2, p);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, p, 0, 0);
                mapBtn.setLayoutParams(lp);
                holder.llActions.addView(mapBtn);
                btnMap = mapBtn;
            }

            final View finalBtnMap = btnMap;
            finalBtnMap.setOnClickListener(v -> {
                try {
                    Uri gmmIntentUri = Uri.parse(store.getMapsUri());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(mapIntent);
                    } else {
                        // Fallback: mở trong browser
                        Uri webUri = Uri.parse("https://maps.google.com/?q=" +
                            Uri.encode(store.getName() + ", " + store.getAddress()));
                        context.startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "Không mở được bản đồ", Toast.LENGTH_SHORT).show();
                }
            });
        }

        holder.rootCard.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = (selectedPosition == position) ? -1 : position;
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }
}
