package com.example.cookapp.admin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.dto.OrderDto;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.VH> {

    private List<OrderDto> items;
    private final OnStatusChangeListener listener;

    public interface OnStatusChangeListener {
        void onStatusChanged(int orderId, String newStatus);
    }

    public AdminOrderAdapter(List<OrderDto> items, OnStatusChangeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<OrderDto> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        OrderDto o = items.get(pos);
        h.tvOrderId.setText("#" + o.id);
        h.tvCustomer.setText(o.customerName != null ? o.customerName : "—");
        h.tvPhone.setText(o.phone != null ? o.phone : "");

        long total = o.totalPrice + o.shippingFee;
        h.tvTotal.setText(NumberFormat.getInstance(new Locale("vi")).format(total) + " đ");

        // Date
        h.tvDate.setText(formatDate(o.createdAt));

        // Status badge
        applyStatusBadge(h.tvStatus, o.status);

        // Spinner
        String[] statuses = {"Chờ xác nhận", "Đang giao", "Hoàn thành", "Đã hủy"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(h.itemView.getContext(),
                android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        h.spinnerStatus.setAdapter(adapter);

        int idx = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(o.status)) { idx = i; break; }
        }
        h.spinnerStatus.setSelection(idx, false);
        boolean locked = "Đã hủy".equals(o.status) || "Hoàn thành".equals(o.status);
        h.spinnerStatus.setEnabled(!locked);
        if (locked) {
            h.btnSaveStatus.setVisibility(View.GONE);
        } else {
            h.btnSaveStatus.setVisibility(View.VISIBLE);
        }

        h.btnSaveStatus.setOnClickListener(v -> {
            int selectedPos = h.spinnerStatus.getSelectedItemPosition();
            if (listener != null) listener.onStatusChanged(o.id, statuses[selectedPos]);
        });
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvCustomer, tvPhone, tvTotal, tvDate;
        Spinner spinnerStatus;
        android.widget.ImageView btnSaveStatus;
        VH(View v) {
            super(v);
            tvOrderId = v.findViewById(R.id.tv_order_id);
            tvStatus = v.findViewById(R.id.tv_status);
            tvCustomer = v.findViewById(R.id.tv_customer);
            tvPhone = v.findViewById(R.id.tv_phone);
            tvTotal = v.findViewById(R.id.tv_total);
            tvDate = v.findViewById(R.id.tv_date);
            spinnerStatus = v.findViewById(R.id.spinner_status);
            btnSaveStatus = v.findViewById(R.id.btn_save_status);
        }
    }

    private void applyStatusBadge(TextView tv, String status) {
        tv.setText(status);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40);
        int bgColor, textColor;
        switch (status != null ? status : "") {
            case "Chờ xác nhận": bgColor = 0x26FFC107; textColor = 0xFFFFC107; break;
            case "Đang giao":    bgColor = 0x2642A5F5; textColor = 0xFF42A5F5; break;
            case "Hoàn thành":   bgColor = 0x264CAF50; textColor = 0xFF4CAF50; break;
            case "Đã hủy":      bgColor = 0x26EF5350; textColor = 0xFFEF5350; break;
            default:             bgColor = 0x26FFC107; textColor = 0xFFFFC107;
        }
        bg.setColor(bgColor);
        tv.setBackground(bg);
        tv.setTextColor(textColor);
    }

    private String formatDate(String iso) {
        if (iso == null) return "—";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date d = in.parse(iso.replace(".000Z",""));
            SimpleDateFormat out = new SimpleDateFormat("dd/MM HH:mm", new Locale("vi"));
            return out.format(d);
        } catch (Exception e) { return iso.substring(0, Math.min(10, iso.length())); }
    }
}
