package com.example.cookapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cookapp.data.local.AppDatabase;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderDetailActivity extends AppCompatActivity {

    private int orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sys.top, v.getPaddingRight(), sys.bottom);
            return insets;
        });

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        orderId = getIntent().getIntExtra("order_id", 0);
        loadOrderDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrderDetail();
    }

    private void loadOrderDetail() {
        boolean isRemote = getIntent().getBooleanExtra("is_remote", true); // assume remote from now on by default

        com.example.cookapp.api.ApiService api = com.example.cookapp.api.RetrofitClient.getClient(this).create(com.example.cookapp.api.ApiService.class);
        api.getOrderDetail(orderId).enqueue(new retrofit2.Callback<com.example.cookapp.api.dto.OrderDto>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.cookapp.api.dto.OrderDto> call, retrofit2.Response<com.example.cookapp.api.dto.OrderDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindRemoteUI(response.body());
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.example.cookapp.api.dto.OrderDto> call, Throwable t) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindRemoteUI(com.example.cookapp.api.dto.OrderDto o) {
        TextView tvId = findViewById(R.id.tv_detail_order_id);
        if (tvId != null) tvId.setText("Đơn #" + o.id);

        TextView tvStatus = findViewById(R.id.tv_detail_status);
        if (tvStatus != null) {
            tvStatus.setText(o.status != null ? o.status : "Chờ xác nhận");
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getStatusColor(o.status)));
        }

        updateStepper(o.status);

        TextView tvDate = findViewById(R.id.tv_detail_date);
        if (tvDate != null) {
            String dateStr;
            try {
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date d = in.parse(o.createdAt.replace(".000Z", ""));
                dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d);
            } catch (Exception e) {
                dateStr = o.createdAt;
            }
            tvDate.setText(dateStr);
        }

        TextView tvAddr = findViewById(R.id.tv_detail_address);
        if (tvAddr != null) tvAddr.setText(o.address != null ? o.address : "–");

        TextView tvPay = findViewById(R.id.tv_detail_payment);
        if (tvPay != null) tvPay.setText(o.paymentMethod != null ? o.paymentMethod : "COD");

        TextView tvItems = findViewById(R.id.tv_detail_items);
        if (tvItems != null) tvItems.setText(o.itemsSummary != null ? o.itemsSummary : "–");

        LinearLayout llNote = findViewById(R.id.ll_note_row);
        TextView tvNote = findViewById(R.id.tv_detail_note);
        if (o.note != null && !o.note.isEmpty()) {
            if (llNote != null) llNote.setVisibility(View.VISIBLE);
            if (tvNote != null) tvNote.setText(o.note);
        }

        TextView tvSub = findViewById(R.id.tv_detail_subtotal);
        TextView tvShip = findViewById(R.id.tv_detail_shipping);
        TextView tvTotal = findViewById(R.id.tv_detail_total);
        if (tvSub != null) tvSub.setText(formatPrice(o.totalPrice));
        if (tvShip != null) tvShip.setText("+" + formatPrice(o.shippingFee));
        if (tvTotal != null) tvTotal.setText(formatPrice(o.totalPrice + o.shippingFee));

        LinearLayout llCancelInfo = findViewById(R.id.ll_cancel_info);
        LinearLayout llRefundPolicy = findViewById(R.id.ll_refund_policy);
        if ("Đã hủy".equals(o.status)) {
            if (llCancelInfo != null) {
                llCancelInfo.setVisibility(View.VISIBLE);
                TextView tvReason = findViewById(R.id.tv_cancel_reason);
                TextView tvTime = findViewById(R.id.tv_cancel_time);
                if (tvReason != null)
                    tvReason.setText(o.cancelReason != null ? o.cancelReason : "Không rõ");
                if (tvTime != null && o.cancelledAt != null) {
                    try {
                        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        Date d = in.parse(o.cancelledAt.replace(".000Z", ""));
                        tvTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d));
                    } catch (Exception e) {
                        tvTime.setText(o.cancelledAt);
                    }
                }
            }
            if (llRefundPolicy != null) {
                llRefundPolicy.setVisibility(View.VISIBLE);
                TextView tvRefund = findViewById(R.id.tv_refund_detail);
                if (tvRefund != null) {
                    if ("Chuyển khoản".equals(o.paymentMethod)) {
                        tvRefund.setText("Số tiền " + formatPrice(o.totalPrice + o.shippingFee)
                                + " sẽ được hoàn lại vào tài khoản gốc trong 3–5 ngày làm việc.");
                    } else {
                        tvRefund.setText("Đơn hàng thanh toán COD — không phát sinh hoàn trả.");
                    }
                }
            }
        }

        LinearLayout btnCancel = findViewById(R.id.btn_cancel_order);
        if (btnCancel != null) {
            boolean canCancel = "Chờ xác nhận".equals(o.status) || "Đang xử lý".equals(o.status);
            btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            btnCancel.setOnClickListener(v -> showCancelDialog());
        }
    }



    // ── Status stepper ──────────────────────────────────────────────────
    private void updateStepper(String status) {
        TextView step1 = findViewById(R.id.step_1);
        TextView step2 = findViewById(R.id.step_2);
        TextView step3 = findViewById(R.id.step_3);
        TextView step4 = findViewById(R.id.step_4);
        View line12 = findViewById(R.id.line_1_2);
        View line23 = findViewById(R.id.line_2_3);
        View line34 = findViewById(R.id.line_3_4);

        int activeColor = Color.parseColor("#00B14F");
        int currentColor = Color.parseColor("#FFA726");
        int inactiveColor = Color.parseColor("#BDBDBD");
        int cancelledColor = Color.parseColor("#9E9E9E");

        if ("Đã hủy".equals(status)) {
            // All grey for cancelled
            setStepColor(step1, cancelledColor);
            setStepColor(step2, cancelledColor);
            setStepColor(step3, cancelledColor);
            setStepColor(step4, cancelledColor);
            if (line12 != null) line12.setBackgroundColor(cancelledColor);
            if (line23 != null) line23.setBackgroundColor(cancelledColor);
            if (line34 != null) line34.setBackgroundColor(cancelledColor);
            // Hide stepper entirely for cancelled, show cancel info instead
            LinearLayout llStepper = findViewById(R.id.ll_stepper);
            if (llStepper != null) llStepper.setVisibility(View.GONE);
            return;
        }

        int level = 0;
        if ("Chờ xác nhận".equals(status)) level = 1;
        else if ("Đang xử lý".equals(status)) level = 2;
        else if ("Đang giao hàng".equals(status)) level = 3;
        else if ("Đã giao".equals(status)) level = 4;

        setStepColor(step1, level >= 1 ? (level == 1 ? currentColor : activeColor) : inactiveColor);
        setStepColor(step2, level >= 2 ? (level == 2 ? currentColor : activeColor) : inactiveColor);
        setStepColor(step3, level >= 3 ? (level == 3 ? currentColor : activeColor) : inactiveColor);
        setStepColor(step4, level >= 4 ? activeColor : inactiveColor);

        if (line12 != null) line12.setBackgroundColor(level >= 2 ? activeColor : inactiveColor);
        if (line23 != null) line23.setBackgroundColor(level >= 3 ? activeColor : inactiveColor);
        if (line34 != null) line34.setBackgroundColor(level >= 4 ? activeColor : inactiveColor);
    }

    private void setStepColor(TextView tv, int color) {
        if (tv != null) {
            tv.setTextColor(color);
            if (color != Color.parseColor("#BDBDBD") && color != Color.parseColor("#9E9E9E")) {
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        }
    }

    // ── Cancel dialog ─────────────────────────────────────────────────────
    private void showCancelDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_order, null);
        dialog.setContentView(view);

        RadioGroup rgReasons = view.findViewById(R.id.rg_cancel_reasons);
        EditText etOther = view.findViewById(R.id.et_other_reason);
        RadioButton rbOther = view.findViewById(R.id.rb_reason_other);

        // Show/hide custom reason input
        if (rgReasons != null) {
            rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
                if (etOther != null) {
                    etOther.setVisibility(checkedId == R.id.rb_reason_other ? View.VISIBLE : View.GONE);
                }
            });
        }

        // Confirm cancel
        View btnConfirm = view.findViewById(R.id.btn_confirm_cancel);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String reason = getSelectedReason(rgReasons, etOther);
                if (reason == null || reason.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn lý do hủy đơn", Toast.LENGTH_SHORT).show();
                    return;
                }
                executeCancelOrder(reason);
                dialog.dismiss();
            });
        }

        // Keep order
        View btnKeep = view.findViewById(R.id.btn_keep_order);
        if (btnKeep != null) {
            btnKeep.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private String getSelectedReason(RadioGroup rg, EditText etOther) {
        if (rg == null) return null;
        int checkedId = rg.getCheckedRadioButtonId();
        if (checkedId == -1) return null;

        if (checkedId == R.id.rb_reason_other) {
            return etOther != null ? etOther.getText().toString().trim() : "";
        }
        RadioButton rb = rg.findViewById(checkedId);
        return rb != null ? rb.getText().toString() : null;
    }

    private void executeCancelOrder(String reason) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("reason", reason);
        com.example.cookapp.api.RetrofitClient.getClient(this)
            .create(com.example.cookapp.api.ApiService.class)
            .cancelOrder(orderId, body)
            .enqueue(new retrofit2.Callback<com.example.cookapp.api.dto.OrderDto>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.cookapp.api.dto.OrderDto> call, retrofit2.Response<com.example.cookapp.api.dto.OrderDto> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        loadOrderDetail();
                    } else {
                        Toast.makeText(OrderDetailActivity.this, "Không thể hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<com.example.cookapp.api.dto.OrderDto> call, Throwable t) {
                    Toast.makeText(OrderDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private int getStatusColor(String status) {
        if (status == null) return Color.parseColor("#FFA726");
        switch (status) {
            case "Chờ xác nhận": return Color.parseColor("#FFA726"); // amber
            case "Đang xử lý":  return Color.parseColor("#42A5F5"); // blue
            case "Đang giao hàng": return Color.parseColor("#FF7043"); // orange
            case "Đã giao":     return Color.parseColor("#66BB6A"); // green
            case "Đã hủy":      return Color.parseColor("#9E9E9E"); // grey
            default:            return Color.parseColor("#FFA726");
        }
    }

    private String formatPrice(long amount) {
        return String.format("%,d đ", amount).replace(',', '.');
    }
}
