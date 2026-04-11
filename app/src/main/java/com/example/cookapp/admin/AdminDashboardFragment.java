package com.example.cookapp.admin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.AdminStatsDto;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardFragment extends Fragment implements AdminOrderAdapter.OnStatusChangeListener {

    private GridLayout statsGrid;
    private RecyclerView rvRecentOrders;
    private ProgressBar progressBar;
    private TextView tvError;
    private AdminOrderAdapter recentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        statsGrid = view.findViewById(R.id.stats_grid);
        rvRecentOrders = view.findViewById(R.id.rv_recent_orders);
        progressBar = view.findViewById(R.id.progress_bar);
        tvError = view.findViewById(R.id.tv_error);

        recentAdapter = new AdminOrderAdapter(new ArrayList<>(), this);
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentOrders.setAdapter(recentAdapter);

        loadStats();
    }

    @Override
    public void onStatusChanged(int orderId, String newStatus) {
        updateOrderStatus(orderId, newStatus);
    }

    private void updateOrderStatus(int id, String status) {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("status", status);

        api.updateAdminOrderStatus(id, body).enqueue(new Callback<com.example.cookapp.api.dto.OrderDto>() {
            @Override
            public void onResponse(Call<com.example.cookapp.api.dto.OrderDto> call, Response<com.example.cookapp.api.dto.OrderDto> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    android.widget.Toast.makeText(getContext(), "Cập nhật thành công ✓", android.widget.Toast.LENGTH_SHORT).show();
                    // ✅ TỰ ĐỘNG RELOAD SAU KHI CẬP NHẬT
                    loadStats();
                } else {
                    progressBar.setVisibility(View.GONE);
                    android.widget.Toast.makeText(getContext(), "Lỗi cập nhật", android.widget.Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.cookapp.api.dto.OrderDto> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                android.widget.Toast.makeText(getContext(), "Lỗi kết nối", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadStats();
        }
    }

    private void loadStats() {
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.getAdminStats().enqueue(new Callback<AdminStatsDto>() {
            @Override
            public void onResponse(Call<AdminStatsDto> call, Response<AdminStatsDto> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    renderStats(response.body());
                } else {
                    tvError.setText("Không thể tải dữ liệu");
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<AdminStatsDto> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                tvError.setText("Lỗi kết nối: " + t.getMessage());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderStats(AdminStatsDto data) {
        statsGrid.removeAllViews();

        NumberFormat nf = NumberFormat.getInstance(new Locale("vi"));

        // Stat cards
        Object[][] stats = {
            {"💰", nf.format(data.revenue) + " đ", "Doanh thu", 0xFFFF7043},
            {"📦", String.valueOf(data.orders), "Đơn hàng", 0xFF42A5F5},
            {"👥", String.valueOf(data.users), "Người dùng", 0xFF4CAF50},
            {"🍲", String.valueOf(data.recipes), "Công thức", 0xFFFFC107},
            {"📝", String.valueOf(data.posts), "Bài viết", 0xFF42A5F5},
            {"⭐", String.valueOf(data.reviews), "Đánh giá", 0xFF4CAF50},
        };

        for (Object[] s : stats) {
            addStatCard((String) s[0], (String) s[1], (String) s[2], (int) s[3]);
        }

        // Recent orders
        if (data.recentOrders != null && !data.recentOrders.isEmpty()) {
            recentAdapter.setItems(data.recentOrders);
            rvRecentOrders.setVisibility(View.VISIBLE);
        }
    }

    private void addStatCard(String icon, String value, String label, int accentColor) {
        int dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                getResources().getDisplayMetrics());

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14 * dp, 12 * dp, 14 * dp, 12 * dp);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * dp);
        bg.setColor(0xFF1C1F2E);
        bg.setStroke(1 * dp, 0xFF2A2E3F);
        card.setBackground(bg);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(4 * dp, 4 * dp, 4 * dp, 4 * dp);
        card.setLayoutParams(params);

        // Icon
        TextView tvIcon = new TextView(requireContext());
        tvIcon.setText(icon);
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        card.addView(tvIcon);

        // Value
        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvValue.setTextColor(0xFFE8EAF0);
        tvValue.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        valParams.topMargin = 4 * dp;
        tvValue.setLayoutParams(valParams);
        card.addView(tvValue);

        // Label
        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvLabel.setTextColor(0xFF8B8FA3);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = 2 * dp;
        tvLabel.setLayoutParams(labelParams);
        card.addView(tvLabel);

        // Left accent border (draw via a View)
        // We'll use setForeground or just tint the left border
        GradientDrawable leftBorder = new GradientDrawable();
        leftBorder.setCornerRadius(12 * dp);
        leftBorder.setColor(0xFF1C1F2E);
        leftBorder.setStroke(1 * dp, 0xFF2A2E3F);
        // Override left stroke with accent
        GradientDrawable accent = new GradientDrawable();
        accent.setShape(GradientDrawable.RECTANGLE);
        accent.setCornerRadii(new float[]{12*dp, 12*dp, 0, 0, 0, 0, 12*dp, 12*dp});
        accent.setColor(accentColor);

        // Instead of complex border, just add a colored View on left
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.HORIZONTAL);

        View stripe = new View(requireContext());
        stripe.setLayoutParams(new LinearLayout.LayoutParams(3 * dp, LinearLayout.LayoutParams.MATCH_PARENT));
        GradientDrawable strBg = new GradientDrawable();
        strBg.setCornerRadius(4 * dp);
        strBg.setColor(accentColor);
        stripe.setBackground(strBg);

        // Rebuild: wrap stripe + card content in horizontal layout
        // But simpler: just put stripe inside the card at top
        // Actually easiest: remove card from grid, wrap in horizontal

        // Simplify: just use the card directly with top tint line
        View topLine = new View(requireContext());
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 3 * dp);
        lineParams.bottomMargin = 8 * dp;
        topLine.setLayoutParams(lineParams);
        GradientDrawable lineBg = new GradientDrawable();
        lineBg.setCornerRadius(2 * dp);
        lineBg.setColor(accentColor);
        topLine.setBackground(lineBg);

        // Insert line at top
        card.addView(topLine, 0);

        statsGrid.addView(card);
    }
}
