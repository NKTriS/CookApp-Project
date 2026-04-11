package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView   rvCart;
    private CartAdapter    adapter;
    private TextView       tvTotalPrice;
    private TextView       tvCheckoutPrice;
    private LinearLayout   totalContainer;
    private LinearLayout   checkoutBar;

    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        // Giỏ hàng cần đăng nhập
        if (SessionManager.requireLogin(this)) { finish(); return; }

        // Back button
        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        // Bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_shop);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_recipe) {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_fav) {
                    if (SessionManager.requireLogin(this)) return true;
                    startActivity(new Intent(this, FavoritesActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                } else if (id == R.id.nav_shop) {
                    // Current is Cart, but nav_shop usually points to ShopActivity
                    // Do we navigate back to ShopActivity? Or do nothing?
                    // Let's go to ShopActivity
                    startActivity(new Intent(this, ShopActivity.class));
                    overridePendingTransition(0, 0); finish(); return true;
                }
                return false;
            });
        }

        // Views
        rvCart         = findViewById(R.id.rv_cart);
        tvTotalPrice   = findViewById(R.id.tv_total_price);
        tvCheckoutPrice = findViewById(R.id.tv_checkout_price);
        totalContainer = findViewById(R.id.total_container);
        checkoutBar    = findViewById(R.id.checkout_bar);

        rvCart.setLayoutManager(new LinearLayoutManager(this));

        // Sao chép danh sách để tránh ConcurrentModificationException khi xóa
        List<CartItem> cartCopy = new ArrayList<>(CartManager.getInstance(this).getCartItems());

        adapter = new CartAdapter(cartCopy, this::updateTotals);
        rvCart.setAdapter(adapter);

        // Checkout bar → CheckoutActivity
        if (checkoutBar != null) {
            checkoutBar.setOnClickListener(v ->
                    startActivity(new Intent(CartActivity.this, CheckoutActivity.class)));
        }

        updateTotals();
    }

    /** Cập nhật tổng tiền và ẩn/hiện checkout bar */
    private void updateTotals() {
        if (adapter == null) return;
        List<CartItem> items = adapter.getItems();
        
        // Đồng bộ ngược lại vào Singleton Manager để lưu mảng mới nhất vào Local & Server DB
        CartManager.getInstance(this).replaceEntireCart(items);

        long total = 0;
        for (CartItem item : items) {
            total += (long) item.getPrice() * item.getQty();
        }

        boolean hasItems = !items.isEmpty();

        if (totalContainer != null)
            totalContainer.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (checkoutBar != null)
            checkoutBar.setVisibility(hasItems ? View.VISIBLE : View.GONE);

        if (hasItems) {
            String formatted = formatVnd((int) total);
            if (tvTotalPrice   != null) tvTotalPrice.setText(formatted + " đ");
            if (tvCheckoutPrice != null) tvCheckoutPrice.setText(formatted + " đ");
        }
    }

    private String formatVnd(int dong) {
        if (dong <= 0) return "0";
        return formatter.format(dong).replace(",", ".");
    }
}
