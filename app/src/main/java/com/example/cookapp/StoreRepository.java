package com.example.cookapp;

import java.util.Arrays;
import java.util.List;

/**
 * Cung cấp danh sách cửa hàng gần đây.
 *
 * ⚠ DEMO: Dữ liệu hiện tại là mock (Hà Nội khu vực Hoàn Kiếm/Hai Bà Trưng).
 *
 * Để production-ready:
 *   1. Tạo endpoint GET /api/stores?lat=x&lng=y trên backend
 *   2. Dùng FusedLocationProviderClient để lấy tọa độ thật
 *   3. Gọi API và replace danh sách này
 */
public class StoreRepository {

    /**
     * Danh sách mock stores với tọa độ gần Hà Nội.
     * Tọa độ này cho phép mở đúng vị trí trên Google Maps.
     */
    public List<Store> getLocalStores() {
        return Arrays.asList(
            new Store("1", "Siêu thị Go! Hà Nội",
                "Siêu thị", "0.4 km",
                "Số 5 Lê Duẩn, Hai Bà Trưng, Hà Nội",
                true, 4.5, 21.0033, 105.8471),

            new Store("2", "Co.opmart Lý Thường Kiệt",
                "Siêu thị", "0.8 km",
                "62 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội",
                true, 4.3, 21.0198, 105.8480),

            new Store("3", "Chợ Hôm - Đức Viên",
                "Chợ truyền thống", "1.1 km",
                "Phố Huế, Hai Bà Trưng, Hà Nội",
                true, 4.1, 21.0156, 105.8514),

            new Store("4", "WinMart+ Trần Hưng Đạo",
                "Cửa hàng tiện lợi", "1.4 km",
                "83 Trần Hưng Đạo, Hoàn Kiếm, Hà Nội",
                false, 3.9, 21.0252, 105.8442),

            new Store("5", "Bách Hóa Xanh Bạch Mai",
                "Cửa hàng tiện lợi", "1.7 km",
                "128 Bạch Mai, Hai Bà Trưng, Hà Nội",
                true, 4.2, 21.0080, 105.8534)
        );
    }
}
