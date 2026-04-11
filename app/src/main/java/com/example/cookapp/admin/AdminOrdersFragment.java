package com.example.cookapp.admin;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.AdminOrdersResponse;
import com.example.cookapp.api.dto.OrderDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminOrdersFragment extends Fragment {

    private RecyclerView rvOrders;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private LinearLayout chipContainer;
    private AdminOrderAdapter adapter;
    private String currentFilter = "";
    private final String[] filters = {"Tất cả", "Chờ xác nhận", "Đang giao", "Hoàn thành", "Đã hủy"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvOrders = view.findViewById(R.id.rv_orders);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        chipContainer = view.findViewById(R.id.chip_container);

        adapter = new AdminOrderAdapter(new ArrayList<>(), this::onStatusChanged);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(adapter);

        buildChips();
        loadOrders();
    }

    private void buildChips() {
        chipContainer.removeAllViews();
        int dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                getResources().getDisplayMetrics());

        for (String filter : filters) {
            TextView chip = new TextView(requireContext());
            chip.setText(filter);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(14 * dp, 8 * dp, 14 * dp, 8 * dp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8 * dp);
            chip.setLayoutParams(lp);

            boolean selected = (filter.equals("Tất cả") && currentFilter.isEmpty())
                    || filter.equals(currentFilter);
            applyChipStyle(chip, selected, dp);

            chip.setOnClickListener(v -> {
                currentFilter = filter.equals("Tất cả") ? "" : filter;
                buildChips();
                loadOrders();
            });

            chipContainer.addView(chip);
        }
    }

    private void applyChipStyle(TextView chip, boolean selected, int dp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(20 * dp);
        if (selected) {
            bg.setColor(0xFFFF7043);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            bg.setColor(0xFF1C1F2E);
            bg.setStroke(1 * dp, 0xFF2A2E3F);
            chip.setTextColor(0xFF8B8FA3);
        }
        chip.setBackground(bg);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadOrders();
        }
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.getAdminOrders(1, 50, currentFilter).enqueue(new Callback<AdminOrdersResponse>() {
            @Override
            public void onResponse(Call<AdminOrdersResponse> call, Response<AdminOrdersResponse> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    AdminOrdersResponse data = response.body();
                    adapter.setItems(data.orders);
                    tvEmpty.setVisibility(data.orders.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<AdminOrdersResponse> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onStatusChanged(int orderId, String newStatus) {
        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("status", newStatus);
        api.updateAdminOrderStatus(orderId, body).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Cập nhật thành công ✓", Toast.LENGTH_SHORT).show();
                    loadOrders();
                } else {
                    Toast.makeText(getContext(), "Lỗi cập nhật", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
