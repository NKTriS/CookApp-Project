package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.StoreProductDto;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopActivity extends AppCompatActivity {

    private final List<Product> allProducts   = new ArrayList<>();
    private final List<Product> displayList   = new ArrayList<>();
    private ShopAdapter   adapter;

    // Filter state
    private String activeStore = null;   // null = tất cả

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);

        // Back button
        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        // Bottom navigation
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
                } else if (id == R.id.nav_shop) { return true; }
                return false;
            });
        }

        // Cart button
        FrameLayout btnCart = findViewById(R.id.btn_cart);
        if (btnCart != null) btnCart.setOnClickListener(v -> {
                if (SessionManager.requireLogin(this)) return;
                startActivity(new Intent(this, CartActivity.class));
        });

        // Nearby stores


        // RecyclerView
        RecyclerView rv = findViewById(R.id.rv_products);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ShopAdapter(displayList, product -> {
                int priceInt = 0;
                try {
                    priceInt = Integer.parseInt(product.getPriceText().replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
                CartManager.getInstance(ShopActivity.this).addOrUpdateItem(
                        "store_" + product.getId(),
                        product.getName(),
                        product.getUnit(),
                        priceInt,
                        1);
                android.widget.Toast.makeText(this,
                        "✓ Đã thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show();
            });
            rv.setAdapter(adapter);
        }

        // Load từ siêu thị duy nhất: Bách Hóa Xanh

        // Live search
        EditText etSearch = findViewById(R.id.et_search);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProducts(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Load từ API store-products (chỉ WinMart)
        loadStoreProducts("WinMart");
    }



    /** Load sản phẩm từ API /store-products?store=xxx */
    private void loadStoreProducts(String storeName) {
        // Show loading state
        TextView tvLoading = findViewById(R.id.tv_shop_loading);
        if (tvLoading != null) {
            tvLoading.setVisibility(View.VISIBLE);
            tvLoading.setText("🔍 Đang tải sản phẩm...");
        }

        RetrofitClient.getClient(this)
            .create(ApiService.class)
            .searchStoreProducts(null, storeName)
            .enqueue(new Callback<List<StoreProductDto>>() {
                @Override
                public void onResponse(Call<List<StoreProductDto>> call, Response<List<StoreProductDto>> response) {
                    if (tvLoading != null) tvLoading.setVisibility(View.GONE);
                    if (!response.isSuccessful() || response.body() == null) return;

                    List<Product> products = new ArrayList<>();
                    DecimalFormat fmt = new DecimalFormat("#,###");
                    for (StoreProductDto dto : response.body()) {
                        String priceText = dto.priceDong > 0
                            ? fmt.format(dto.priceDong).replace(",", ".") + " đ"
                            : "Liên hệ";
                        String unit = dto.unit != null ? dto.unit : "";
                        // Dùng tên store làm badge trong tên hiển thị
                        Product p = new Product(
                            dto.id,
                            dto.productName,
                            unit,
                            priceText,
                            dto.imageUrl != null ? dto.imageUrl : ""
                        );
                        p.setStoreName(dto.storeName);
                        p.setRating(dto.rating);
                        products.add(p);
                    }

                    // Sắp xếp A-Z theo tên nguyên liệu
                    java.util.Collections.sort(products, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

                    runOnUiThread(() -> {
                        allProducts.clear();
                        allProducts.addAll(products);
                        filterProducts(getSearchQuery());
                    });
                }

                @Override
                public void onFailure(Call<List<StoreProductDto>> call, Throwable t) {
                    if (tvLoading != null) {
                        tvLoading.setVisibility(View.VISIBLE);
                        tvLoading.setText("⚠ Không thể kết nối máy chủ. Kiểm tra mạng.");
                    }
                }
            });
    }

    private String getSearchQuery() {
        EditText et = findViewById(R.id.et_search);
        return et != null ? et.getText().toString().trim() : "";
    }

    private void filterProducts(String query) {
        displayList.clear();
        if (query.isEmpty()) {
            displayList.addAll(allProducts);
        } else {
            String q = removeAccents(query.toLowerCase(new Locale("vi")));
            for (Product p : allProducts) {
                if (removeAccents(p.getName().toLowerCase(new Locale("vi"))).contains(q)) {
                    displayList.add(p);
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    /** Loại bỏ dấu tiếng Việt cho tìm kiếm không dấu */
    private static String removeAccents(String s) {
        return s.replace("à","a").replace("á","a").replace("ả","a").replace("ã","a").replace("ạ","a")
                .replace("ă","a").replace("ắ","a").replace("ặ","a").replace("ằ","a").replace("ẵ","a").replace("ẳ","a")
                .replace("â","a").replace("ấ","a").replace("ầ","a").replace("ẩ","a").replace("ẫ","a").replace("ậ","a")
                .replace("đ","d")
                .replace("è","e").replace("é","e").replace("ẹ","e").replace("ẻ","e").replace("ẽ","e")
                .replace("ê","e").replace("ế","e").replace("ề","e").replace("ệ","e").replace("ể","e").replace("ễ","e")
                .replace("ì","i").replace("í","i").replace("ị","i").replace("ỉ","i").replace("ĩ","i")
                .replace("ò","o").replace("ó","o").replace("ọ","o").replace("ỏ","o").replace("õ","o")
                .replace("ô","o").replace("ố","o").replace("ồ","o").replace("ộ","o").replace("ổ","o").replace("ỗ","o")
                .replace("ơ","o").replace("ớ","o").replace("ờ","o").replace("ợ","o").replace("ở","o").replace("ỡ","o")
                .replace("ù","u").replace("ú","u").replace("ụ","u").replace("ủ","u").replace("ũ","u")
                .replace("ư","u").replace("ứ","u").replace("ừ","u").replace("ự","u").replace("ử","u").replace("ữ","u")
                .replace("ỳ","y").replace("ý","y").replace("ỵ","y").replace("ỷ","y").replace("ỹ","y");
    }
}
