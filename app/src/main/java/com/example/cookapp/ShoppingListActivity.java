package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.ShoppingListItemDto;
import com.example.cookapp.api.dto.ShoppingListResponse;
import com.example.cookapp.api.dto.SyncShoppingListRequest;
import com.example.cookapp.data.local.AppDatabase;
import com.example.cookapp.data.local.entity.ShoppingListItemEntity;
import com.example.cookapp.repository.ShoppingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ShoppingListActivity extends AppCompatActivity {

    private RecyclerView rvShoppingList;
    private TextView tvEmpty;
    private ProgressBar pbLoading;
    private ShoppingListAdapter adapter;

    // Dùng ShoppingListItemEntity làm model hiển thị (tương thích adapter hiện tại)
    private final List<ShoppingListItemEntity> items = new ArrayList<>();
    private ShoppingRepository shoppingRepo;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_list);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        // Login-gate
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem danh sách đi chợ", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        shoppingRepo = new ShoppingRepository(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header_bar), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sb.top + 8, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        rvShoppingList = findViewById(R.id.rv_shopping_list);
        tvEmpty   = findViewById(R.id.tv_empty);
        pbLoading = findViewById(R.id.pb_loading);
        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        rvShoppingList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShoppingListAdapter(items, new ShoppingListAdapter.OnItemInteractionListener() {
            @Override
            public void onCheckChanged(ShoppingListItemEntity item, boolean isChecked) {
                item.checked = isChecked;
                adapter.notifyDataSetChanged();
                syncToApi();
            }

            @Override
            public void onQuantityChanged(ShoppingListItemEntity item, int delta) {
                float newQty = item.quantity + delta;
                if (newQty <= 0) {
                    onDelete(item);
                } else {
                    item.quantity = newQty;
                    adapter.notifyDataSetChanged();
                    syncToApi();
                }
            }

            @Override
            public void onDelete(ShoppingListItemEntity item) {
                items.remove(item);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                syncToApi();
            }
        });
        rvShoppingList.setAdapter(adapter);

        // Clear All button
        ImageView btnClearAll = findViewById(R.id.btn_clear_all);
        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> {
                if (items.isEmpty()) return;
                new AlertDialog.Builder(this)
                    .setTitle("Xóa danh sách")
                    .setMessage("Bạn có chắc muốn xóa toàn bộ danh sách đi chợ?")
                    .setPositiveButton("Xóa tất cả", (d, w) -> {
                        items.clear();
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        syncToApi();
                        Toast.makeText(this, "Đã xóa toàn bộ!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            });
        }

        // loadShoppingList will be called by onResume()
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadShoppingList();
    }

    private void loadShoppingList() {
        shoppingRepo.getShoppingList(result -> {
            if (result.status == Resource.Status.LOADING) {
                runOnUiThread(() -> { if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE); });
            } else if (result.status == Resource.Status.SUCCESS && result.data != null) {
                ShoppingListResponse resp = result.data;
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    items.clear();
                    // Convert DTO → Entity for adaptor compatibility, with deduplication
                    if (resp.items != null) {
                        java.util.Set<String> seen = new java.util.HashSet<>();
                        for (ShoppingListItemDto dto : resp.items) {
                            String key = dto.ingredient_name != null ? dto.ingredient_name.toLowerCase().trim() : "";
                            if (!key.isEmpty() && seen.add(key)) {
                                items.add(dtoToEntity(dto));
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            } else if (result.status == Resource.Status.ERROR) {
                runOnUiThread(() -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    loadFromLocalDb(); // Offline fallback
                });
            }
        });
    }

    /** Convert API DTO sang Room entity cho adapter */
    private ShoppingListItemEntity dtoToEntity(ShoppingListItemDto dto) {
        ShoppingListItemEntity e = new ShoppingListItemEntity();
        e.id = dto.id;
        e.shopping_list_id = dto.shopping_list_id;
        e.ingredient_name = dto.ingredient_name;
        e.quantity = dto.quantity;
        e.unit = dto.unit != null ? dto.unit : "";
        e.checked = dto.checked;
        return e;
    }

    /** Fallback: Load từ Room DB khi mất mạng */
    private void loadFromLocalDb() {
        int userId = new SessionManager(this).getUserId();
        if (userId == -1) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            var sDao = AppDatabase.getDatabase(this).shoppingListDao();
            var list = sDao.getLatestListByUser(userId);
            if (list != null) {
                List<ShoppingListItemEntity> localItems = sDao.getItemsByListId(list.id);
                runOnUiThread(() -> {
                    items.clear();
                    if (localItems != null) items.addAll(localItems);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(this, "⚠️ Đang dùng dữ liệu ngoại tuyến", Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(this::updateEmptyState);
            }
        });
    }

    /** Đồng bộ danh sách hiện tại lên API (replace toàn bộ) */
    private void syncToApi() {
        List<SyncShoppingListRequest.ShoppingItem> dtoItems = new ArrayList<>();
        for (ShoppingListItemEntity e : items) {
            dtoItems.add(new SyncShoppingListRequest.ShoppingItem(
                e.ingredient_name, e.quantity, e.unit, e.checked, 0));
        }
        shoppingRepo.syncShoppingList(new SyncShoppingListRequest(dtoItems), result -> {
            if (result.status == Resource.Status.ERROR) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Lỗi đồng bộ danh sách", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateEmptyState() {
        boolean empty = items.isEmpty();
        if (rvShoppingList != null) rvShoppingList.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
