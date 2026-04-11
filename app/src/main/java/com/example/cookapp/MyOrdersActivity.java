package com.example.cookapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.data.local.AppDatabase;

import com.example.cookapp.SessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyOrdersActivity extends AppCompatActivity {

    private List<com.example.cookapp.api.dto.OrderDto> allOrders;
    private TextView[] tabs;
    private String[] tabStatuses = {"Tất cả", "Chờ xác nhận", "Đang giao", "Hoàn thành", "Đã hủy"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        
        setupTabs();
    }

    private void setupTabs() {
        tabs = new TextView[]{
            findViewById(R.id.tab_all),
            findViewById(R.id.tab_pending),
            findViewById(R.id.tab_delivering),
            findViewById(R.id.tab_completed),
            findViewById(R.id.tab_cancelled)
        };

        for (int i = 0; i < tabs.length; i++) {
            int finalI = i;
            if (tabs[i] != null) {
                tabs[i].setOnClickListener(v -> selectTab(finalI));
            }
        }
    }

    private void selectTab(int index) {
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) continue;
            if (i == index) {
                tabs[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF383C")));
                tabs[i].setTextColor(Color.WHITE);
                tabs[i].setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tabs[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F0F0F0")));
                tabs[i].setTextColor(Color.parseColor("#1A1A1A"));
                tabs[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
        
        if (allOrders == null) return;
        
        String filter = tabStatuses[index];
        List<com.example.cookapp.api.dto.OrderDto> filtered = new java.util.ArrayList<>();
        
        for (com.example.cookapp.api.dto.OrderDto o : allOrders) {
            String s = o.status != null ? o.status : "Chờ xác nhận";
            if (filter.equals("Tất cả")) {
                filtered.add(o);
            } else if (filter.equals("Đang giao") && (s.equals("Đang giao") || s.equals("Đang giao hàng") || s.equals("Đang xử lý"))) {
                filtered.add(o);
            } else if (filter.equals("Hoàn thành") && (s.equals("Đã giao") || s.equals("Hoàn thành"))) {
                filtered.add(o);
            } else if (filter.equals(s)) {
                filtered.add(o);
            }
        }

        RecyclerView rv = findViewById(R.id.rv_orders);
        TextView tvEmpty = findViewById(R.id.tv_empty_orders);
        
        if (filtered.isEmpty()) {
            if (rv != null) rv.setVisibility(View.GONE);
            if (tvEmpty != null) {
                tvEmpty.setText("Chưa có đơn hàng nào.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        } else {
            if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
            if (rv != null) {
                rv.setVisibility(View.VISIBLE);
                rv.setAdapter(new OrderAdapter(filtered));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        RecyclerView rv = findViewById(R.id.rv_orders);
        TextView tvEmpty = findViewById(R.id.tv_empty_orders);
        if (rv == null) return;

        com.example.cookapp.api.ApiService api = com.example.cookapp.api.RetrofitClient.getClient(this).create(com.example.cookapp.api.ApiService.class);
        api.getOrders().enqueue(new retrofit2.Callback<List<com.example.cookapp.api.dto.OrderDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<com.example.cookapp.api.dto.OrderDto>> call, retrofit2.Response<List<com.example.cookapp.api.dto.OrderDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allOrders = response.body();
                    java.util.Collections.sort(allOrders, (o1, o2) -> {
                        int r1 = getStatusRank(o1.status);
                        int r2 = getStatusRank(o2.status);
                        if (r1 != r2) return Integer.compare(r1, r2);
                        return Integer.compare(o2.id, o1.id);
                    });
                    
                    if (rv.getLayoutManager() == null) {
                        rv.setLayoutManager(new LinearLayoutManager(MyOrdersActivity.this));
                    }
                    
                    // Kích hoạt lại tab đang chọn (mặc định tab 0 - Tất cả)
                    int selectedTab = 0;
                    for (int i = 0; i < tabs.length; i++) {
                        if (tabs[i] != null && tabs[i].getCurrentTextColor() == Color.WHITE) {
                            selectedTab = i;
                            break;
                        }
                    }
                    selectTab(selectedTab);
                } else {
                    showError(rv, tvEmpty);
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<com.example.cookapp.api.dto.OrderDto>> call, Throwable t) {
                showError(rv, tvEmpty);
            }
        });
    }

    private void showError(RecyclerView rv, TextView tvEmpty) {
        if (rv != null) rv.setVisibility(View.GONE);
        if (tvEmpty != null) {
            tvEmpty.setText("Không thể tải đơn hàng. Vui lòng kiểm tra kết nối mạng.");
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private int getStatusRank(String status) {
        if (status == null) return 1;
        switch (status) {
            case "Chờ xác nhận": return 0;
            case "Đang xử lý": return 1;
            case "Đang giao": return 2;
            case "Đang giao hàng": return 2;
            case "Đã giao": return 3;
            case "Hoàn thành": return 3;
            case "Đã hủy": return 4;
            default: return 5;
        }
    }

    // ── Remote Adapter ───────────────────────────────────────────────────────
    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {
        private final List<com.example.cookapp.api.dto.OrderDto> list;

        OrderAdapter(List<com.example.cookapp.api.dto.OrderDto> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            com.example.cookapp.api.dto.OrderDto o = list.get(pos);
            h.tvOrderId.setText("Đơn #" + o.id);

            String status = o.status != null ? o.status : "Chờ xác nhận";
            h.tvStatus.setText(status);
            h.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getStatusColor(status)));

            h.tvItems.setText(o.itemsSummary != null ? o.itemsSummary : "–");
            h.tvAddress.setText(o.address != null ? o.address : "–");
            long finalTotal = o.totalPrice + o.shippingFee;
            h.tvTotal.setText(formatPrice(finalTotal));
            
            String dateStr;
            try {
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date d = in.parse(o.createdAt.replace(".000Z", ""));
                dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d);
            } catch (Exception e) {
                dateStr = o.createdAt;
            }
            h.tvDate.setText(dateStr);

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MyOrdersActivity.this, OrderDetailActivity.class);
                intent.putExtra("order_id", o.id);
                intent.putExtra("is_remote", true);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvStatus, tvItems, tvAddress, tvTotal, tvDate;

            VH(View v) {
                super(v);
                tvOrderId  = v.findViewById(R.id.tv_order_id);
                tvStatus   = v.findViewById(R.id.tv_order_status);
                tvItems    = v.findViewById(R.id.tv_order_items);
                tvAddress  = v.findViewById(R.id.tv_order_address);
                tvTotal    = v.findViewById(R.id.tv_order_total);
                tvDate     = v.findViewById(R.id.tv_order_date);
            }
        }

        private String formatPrice(long amount) {
            return String.format("%,d đ", amount).replace(',', '.');
        }

        private int getStatusColor(String status) {
            if (status == null) return Color.parseColor("#FFA726");
            switch (status) {
                case "Chờ xác nhận":   return Color.parseColor("#FFA726"); // amber
                case "Đang xử lý":    return Color.parseColor("#42A5F5"); // blue
                case "Đang giao":      return Color.parseColor("#42A5F5"); // blue
                case "Đang giao hàng": return Color.parseColor("#FF7043"); // orange
                case "Đã giao":        return Color.parseColor("#66BB6A"); // green
                case "Hoàn thành":     return Color.parseColor("#66BB6A"); // green
                case "Đã hủy":         return Color.parseColor("#EF5350"); // grey
                default:               return Color.parseColor("#FFA726");
            }
        }
    }
}
