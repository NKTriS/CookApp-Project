package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OrderSuccessActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_success);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sys.top, v.getPaddingRight(), sys.bottom);
            return insets;
        });

        // Dữ liệu đơn hàng từ CheckoutActivity
        int    orderId       = getIntent().getIntExtra("order_id", 0);
        long   orderTotal    = getIntent().getLongExtra("order_total", 0);
        String paymentMethod = getIntent().getStringExtra("payment_method");
        String address       = getIntent().getStringExtra("address");
        String itemsSummary  = getIntent().getStringExtra("items_summary");

        if (paymentMethod == null) paymentMethod = "COD";

        // ── Bind UI ─────────────────────────────────────────────────────
        TextView tvOrderCode  = findViewById(R.id.tv_order_code);
        TextView tvOrderTotal = findViewById(R.id.tv_order_total_val);
        TextView tvPayment    = findViewById(R.id.tv_payment_val);
        TextView tvAddress    = findViewById(R.id.tv_address_val);
        TextView tvItems      = findViewById(R.id.tv_items_val);
        TextView tvEstTime    = findViewById(R.id.tv_est_time);

        if (tvOrderCode != null)
            tvOrderCode.setText("Mã đơn hàng: #" + orderId);

        if (tvOrderTotal != null)
            tvOrderTotal.setText(formatPrice(orderTotal));

        if (tvPayment != null)
            tvPayment.setText(paymentMethod);

        if (tvAddress != null && address != null)
            tvAddress.setText(address);

        if (tvItems != null && itemsSummary != null && !itemsSummary.isEmpty())
            tvItems.setText(itemsSummary);

        if (tvEstTime != null)
            tvEstTime.setText("Dự kiến giao trong 30–45 phút");

        // ── Buttons ──────────────────────────────────────────────────────
        LinearLayout btnHome = findViewById(R.id.btn_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(OrderSuccessActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        boolean isRemote = getIntent().getBooleanExtra("is_remote", false);

        LinearLayout btnTrackOrder = findViewById(R.id.btn_track_order);
        if (btnTrackOrder != null) {
            btnTrackOrder.setOnClickListener(v -> {
                Intent intent = new Intent(OrderSuccessActivity.this, OrderDetailActivity.class);
                intent.putExtra("order_id", orderId);
                intent.putExtra("is_remote", isRemote);
                startActivity(intent);
            });
        }
    }

    private String formatPrice(long amount) {
        return String.format("%,d đ", amount).replace(',', '.');
    }
}
