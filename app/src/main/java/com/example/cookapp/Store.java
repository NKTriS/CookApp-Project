package com.example.cookapp;

/**
 * Model cửa hàng gần đây.
 *
 * ⚠ DEMO DATA: Dữ liệu hiện tại là mock cứng trong StoreRepository.
 * TODO: Sau này thay bằng API endpoint GET /api/stores?lat=x&lng=y
 *       kết hợp với FusedLocationProviderClient để lấy vị trí thật của user.
 */
public class Store {
    private String id;
    private String name;
    private String type;
    private String distance;
    private String address;
    private boolean isOpen;
    private double rating;
    /** Tọa độ để mở Google Maps (optional, null = dùng address) */
    private Double lat;
    private Double lng;

    public Store(String id, String name, String type, String distance, String address, boolean isOpen, double rating) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.distance = distance;
        this.address = address;
        this.isOpen = isOpen;
        this.rating = rating;
    }

    public Store(String id, String name, String type, String distance, String address,
                 boolean isOpen, double rating, double lat, double lng) {
        this(id, name, type, distance, address, isOpen, rating);
        this.lat = lat;
        this.lng = lng;
    }

    /** Tạo Google Maps URI để mở bản đồ */
    public String getMapsUri() {
        if (lat != null && lng != null) {
            return "geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + android.net.Uri.encode(name) + ")";
        }
        // Fallback: tìm kiếm theo tên + địa chỉ
        return "geo:0,0?q=" + android.net.Uri.encode(name + ", " + address);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
}
