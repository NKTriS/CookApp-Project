package com.example.cookapp;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * NearbyStoresActivity — Danh sách cửa hàng gần đây.
 *
 * ⚠ DEMO DATA: Dữ liệu hiện tại là mock/hardcode.
 * TODO: Tích hợp Google Maps API / backend location endpoint để lấy dữ liệu thật.
 *
 * Cách tích hợp Maps sau này:
 *   1. Thêm com.google.android.gms:play-services-maps vào build.gradle
 *   2. Gọi LocationManager/FusedLocationProvider để lấy tọa độ user
 *   3. Gọi GET /api/stores?lat=x&lng=y từ backend (cần thêm endpoint)
 *   4. Hiển thị trên SupportMapFragment thay vì RecyclerView
 */
public class NearbyStoresActivity extends AppCompatActivity {

    private RecyclerView rvStores;
    private StoreAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nearby_stores);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        // TODO: Replace with visual banner in layout — current is demo data only
        Toast.makeText(this, "⚠ Đây là dữ liệu demo (chưa tích hợp bản đồ thật)", Toast.LENGTH_LONG).show();

        rvStores = findViewById(R.id.rv_stores);
        if (rvStores != null) {
            rvStores.setLayoutManager(new LinearLayoutManager(this));

            // TODO: Replace StoreRepository with Maps/backend API calls
            StoreRepository repository = new StoreRepository();
            List<Store> stores = repository.getLocalStores();

            adapter = new StoreAdapter(stores);
            rvStores.setAdapter(adapter);
        }
    }
}
