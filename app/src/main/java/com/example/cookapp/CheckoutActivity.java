package com.example.cookapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cookapp.data.local.AppDatabase;
    // Removed OrderEntity
import com.example.cookapp.utils.ShippingHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CheckoutActivity extends AppCompatActivity {

    // Phí ship mặc định (cập nhật sau khi tính)
    private long shippingFee = 25_000L;
    private long subtotal    = 0L;

    private TextView tvSubtotal, tvShipping, tvTotal, tvBtnTotal, tvLocationStatus;
    private EditText etName, etPhone, etAddress, etNote;

    private String selectedPayment = "COD";

    private FusedLocationProviderClient fusedLocation;

    // Permission launcher
    private final ActivityResultLauncher<String> locationPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) getGpsLocation();
            else Toast.makeText(this, "Cần quyền vị trí để tự điền địa chỉ", Toast.LENGTH_SHORT).show();
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkout);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sys.top, v.getPaddingRight(), sys.bottom);
            return insets;
        });

        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        // ── Views ────────────────────────────────────────────────────────
        tvSubtotal      = findViewById(R.id.tv_subtotal);
        tvShipping      = findViewById(R.id.tv_shipping);
        tvTotal         = findViewById(R.id.tv_total);
        tvBtnTotal      = findViewById(R.id.tv_btn_total);
        tvLocationStatus = findViewById(R.id.tv_location_status);
        etName          = findViewById(R.id.et_name);
        etPhone         = findViewById(R.id.et_phone);
        etAddress       = findViewById(R.id.et_address);
        etNote          = findViewById(R.id.et_note);

        // ── 1. Giỏ hàng → render items ───────────────────────────────────
        List<CartItem> cartItems = CartManager.getInstance(this).getCartItems();
        StringBuilder itemsSummaryBuilder = new StringBuilder();
        for (CartItem item : cartItems) {
            subtotal += (long) item.getPrice() * item.getQty();
            if (itemsSummaryBuilder.length() > 0) itemsSummaryBuilder.append(", ");
            itemsSummaryBuilder.append(item.getName()).append(" x").append(item.getQty());
        }
        final String itemsSummary = itemsSummaryBuilder.toString();

        LinearLayout llItems = findViewById(R.id.ll_order_items);
        if (llItems != null) {
            llItems.removeAllViews();
            if (cartItems.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("Giỏ hàng trống");
                tvEmpty.setTextColor(Color.parseColor("#8E8E93"));
                tvEmpty.setTextSize(14f);
                llItems.addView(tvEmpty);
            } else {
                for (CartItem item : cartItems) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = dpToPx(8);
                    row.setLayoutParams(lp);

                    // Bullet
                    TextView tvBullet = new TextView(this);
                    tvBullet.setText("•  ");
                    tvBullet.setTextColor(Color.parseColor("#FF383C"));
                    tvBullet.setTextSize(14f);

                    TextView tvName2 = new TextView(this);
                    tvName2.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    tvName2.setText(item.getName()
                        + (item.getUnit() != null && !item.getUnit().isEmpty()
                           ? "  (" + item.getQty() + " " + item.getUnit() + ")"
                           : "  ×" + item.getQty()));
                    tvName2.setTextColor(Color.parseColor("#1A1A1A"));
                    tvName2.setTextSize(14f);

                    TextView tvPrice2 = new TextView(this);
                    tvPrice2.setText(formatPrice((long) item.getPrice() * item.getQty()));
                    tvPrice2.setTextColor(Color.parseColor("#FF383C"));
                    tvPrice2.setTextSize(14f);
                    tvPrice2.setTypeface(null, Typeface.BOLD);

                    row.addView(tvBullet);
                    row.addView(tvName2);
                    row.addView(tvPrice2);
                    llItems.addView(row);
                }
            }
        }

        updatePriceUI(); // hiển thị giá ban đầu

        // ── 2. Khi địa chỉ thay đổi → re-tính phí ship ──────────────────
        if (etAddress != null) {
            etAddress.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    String addr = s.toString().trim();
                    if (addr.length() >= 5) {  // tính sau khi nhập đủ
                        recalculateShipping(addr, cartItems.size() * 300);
                    }
                }
            });
        }

        // ── 3. GPS Button ────────────────────────────────────────────────
        LinearLayout btnGps = findViewById(R.id.btn_use_location);
        if (btnGps != null) {
            btnGps.setOnClickListener(v -> requestLocationPermission());
        }

        // ── 4. Phương thức thanh toán ─────────────────────────────────────
        RadioButton rbCod   = findViewById(R.id.rb_cod);
        RadioButton rbBank  = findViewById(R.id.rb_bank);
        View optCod   = findViewById(R.id.option_cod);
        View optBank  = findViewById(R.id.option_bank);

        if (rbCod != null) rbCod.setClickable(false);
        if (rbBank != null) rbBank.setClickable(false);

        updatePaymentSelection(optCod, optBank, rbCod, rbBank, "COD");

        if (optCod   != null) optCod.setOnClickListener(v -> {
            selectedPayment = "COD";
            updatePaymentSelection(optCod, optBank, rbCod, rbBank, "COD");
        });
        if (optBank  != null) optBank.setOnClickListener(v -> {
            selectedPayment = "Chuyển khoản";
            updatePaymentSelection(optCod, optBank, rbCod, rbBank, "Chuyển khoản");
        });

        // ── 5. Xác nhận đặt hàng ─────────────────────────────────────────
        LinearLayout btnConfirm = findViewById(R.id.btn_confirm);
        final long finalSubtotal = subtotal;

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String name    = etName    != null ? etName.getText().toString().trim()    : "";
                String phone   = etPhone   != null ? etPhone.getText().toString().trim()   : "";
                String address = etAddress != null ? etAddress.getText().toString().trim() : "";
                String note    = etNote    != null ? etNote.getText().toString().trim()    : "";

                if (TextUtils.isEmpty(name)) {
                    etName.setError("Vui lòng nhập họ tên!"); etName.requestFocus(); return;
                }
                if (TextUtils.isEmpty(phone) || phone.length() < 9) {
                    etPhone.setError("Số điện thoại không hợp lệ!"); etPhone.requestFocus(); return;
                }
                if (TextUtils.isEmpty(address)) {
                    etAddress.setError("Vui lòng nhập địa chỉ!"); etAddress.requestFocus(); return;
                }
                if (cartItems.isEmpty()) {
                    Toast.makeText(this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show(); return;
                }

                final long finalTotal = finalSubtotal + shippingFee;

                // Disable button
                btnConfirm.setClickable(false);
                btnConfirm.setAlpha(0.5f);

                // ── Tạo đơn trực tiếp trên Thread ─────────────────────────────────
                new Thread(() -> {
                    long finalOrderId = 0;
                    boolean isRemote = false;

                    try {
                        com.example.cookapp.api.dto.CreateOrderRequest apiOrder =
                            new com.example.cookapp.api.dto.CreateOrderRequest(
                                name, phone, address,
                                finalSubtotal, shippingFee,
                                itemsSummary.isEmpty() ? "Không có" : itemsSummary,
                                selectedPayment,
                                note.isEmpty() ? null : note
                            );
                        retrofit2.Response<com.example.cookapp.api.dto.OrderDto> response = com.example.cookapp.api.RetrofitClient.getClient(this)
                            .create(com.example.cookapp.api.ApiService.class)
                            .createOrder(apiOrder)
                            .execute(); 
                            
                        if (response.isSuccessful() && response.body() != null) {
                            finalOrderId = response.body().id;
                            isRemote = true;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("Checkout", "Failed to create order", e);
                    }
                    
                    final long passedOrderId = finalOrderId;
                    final boolean passedIsRemote = isRemote;

                    runOnUiThread(() -> {
                        btnConfirm.setClickable(true);
                        btnConfirm.setAlpha(1.0f);

                        if (!passedIsRemote) {
                            Toast.makeText(CheckoutActivity.this, "Không thể kết nối máy chủ. Vui lòng thử lại sau.", Toast.LENGTH_LONG).show();
                            return; 
                        }
                        
                        CartManager.getInstance(this).clearCart();

                        if ("Chuyển khoản".equals(selectedPayment)) {
                            Intent vnpayIntent = new Intent(CheckoutActivity.this, VnpayPaymentActivity.class);
                            vnpayIntent.putExtra("order_id", (int) passedOrderId);
                            vnpayIntent.putExtra("order_total", finalTotal);
                            vnpayIntent.putExtra("customer_name", name);
                            vnpayIntent.putExtra("address", address);
                            vnpayIntent.putExtra("items_summary", itemsSummary);
                            startActivity(vnpayIntent);
                        } else {
                            Intent intent = new Intent(CheckoutActivity.this, OrderSuccessActivity.class);
                            intent.putExtra("order_id",       (int) passedOrderId);
                            intent.putExtra("is_remote",      true);
                            intent.putExtra("order_total",    finalTotal);
                            intent.putExtra("payment_method", selectedPayment);
                            intent.putExtra("customer_name",  name);
                            intent.putExtra("address",        address);
                            intent.putExtra("items_summary",  itemsSummary);
                            startActivity(intent);
                        }
                        finish();
                    });
                }).start();
            });
        }

    }

    // ── Re-tính phí ship theo địa chỉ ────────────────────────────────────
    private void recalculateShipping(String address, int weightGram) {
        if (tvShipping != null) tvShipping.setText("Đang tính...");
        ShippingHelper.calculateFee(this, address, weightGram, new ShippingHelper.ShippingFeeCallback() {
            @Override
            public void onResult(long feeVnd, String label) {
                runOnUiThread(() -> {
                    shippingFee = feeVnd;
                    if (tvShipping != null)
                        tvShipping.setText("+" + formatPrice(feeVnd) + "  (" + label + ")");
                    updatePriceUI();
                });
            }
            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    if (tvShipping != null) tvShipping.setText("+25.000 đ  (Ước tính)");
                });
            }
        });
    }

    // ── GPS ──────────────────────────────────────────────────────────────
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getGpsLocation();
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getGpsLocation() {
        if (tvLocationStatus != null) {
            tvLocationStatus.setVisibility(View.VISIBLE);
            tvLocationStatus.setText("📡 Đang xác định vị trí GPS...");
        }

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
            .addOnSuccessListener(location -> {
                if (location != null) {
                    onLocationReceived(location);
                } else {
                    // Thử last known location
                    fusedLocation.getLastLocation().addOnSuccessListener(last -> {
                        if (last != null) onLocationReceived(last);
                        else showLocationError();
                    });
                }
            })
            .addOnFailureListener(e -> showLocationError());
    }

    private void onLocationReceived(Location location) {
        if (tvLocationStatus != null)
            tvLocationStatus.setText("📡 Đang chuyển đổi tọa độ → địa chỉ...");

        ShippingHelper.reverseGeocode(this, location.getLatitude(), location.getLongitude(),
            new ShippingHelper.GeocoderCallback() {
                @Override
                public void onAddress(String fullAddress, String city) {
                    runOnUiThread(() -> {
                        if (etAddress != null) etAddress.setText(fullAddress);
                        if (tvLocationStatus != null) {
                            tvLocationStatus.setText("✅ Đã xác định: " + city);
                        }
                        recalculateShipping(fullAddress, 500);
                    });
                }
                @Override
                public void onFailed() {
                    runOnUiThread(() -> {
                        if (tvLocationStatus != null)
                            tvLocationStatus.setText("⚠ Không thể chuyển đổi địa chỉ — nhập tay");
                    });
                }
            });
    }

    private void showLocationError() {
        runOnUiThread(() -> {
            if (tvLocationStatus != null)
                tvLocationStatus.setText("⚠ Không lấy được vị trí — hãy bật GPS");
        });
    }

    private void updatePriceUI() {
        long total = subtotal + shippingFee;
        if (tvSubtotal != null) tvSubtotal.setText(formatPrice(subtotal));
        if (tvTotal    != null) tvTotal.setText(formatPrice(total));
        if (tvBtnTotal != null) tvBtnTotal.setText(" · " + formatPrice(total));
    }


    private void updatePaymentSelection(View optCod, View optBank,
                                        RadioButton rbCod, RadioButton rbBank,
                                        String selected) {
        int activeColor  = Color.parseColor("#FFF0F0");
        int defaultColor = Color.parseColor("#F0F0F0");
        boolean isCod = "COD".equals(selected);
        if (optCod   != null) optCod.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(isCod ? activeColor : defaultColor));
        if (optBank  != null) optBank.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(!isCod ? activeColor : defaultColor));
        if (rbCod   != null) rbCod.setChecked(isCod);
        if (rbBank  != null) rbBank.setChecked(!isCod);
    }

    private String formatPrice(long amount) {
        return String.format("%,d đ", amount).replace(',', '.');
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
