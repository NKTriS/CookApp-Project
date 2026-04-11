package com.example.cookapp;

public class Product {
    private int    id;
    private String name;
    private String unit;
    private String priceText;
    private String imageUrl;   // Glide-loaded URL (replaces static resId)

    public Product(int id, String name, String unit, String priceText, String imageUrl) {
        this.id        = id;
        this.name      = name;
        this.unit      = unit;
        this.priceText = priceText;
        this.imageUrl  = imageUrl;
    }

    // Legacy constructor for backward compat (unused, will be removed gradually)
    public Product(int id, String name, String unit, String priceText, int imageResId) {
        this(id, name, unit, priceText, (String) null);
    }

    public int    getId()        { return id; }
    public String getName()      { return name; }
    public String getUnit()      { return unit; }
    public String getPriceText() { return priceText; }
    public String getImageUrl()  { return imageUrl; }

    // Store info (từ API store-products)
    private String storeName;
    private float  rating;
    public String getStoreName()            { return storeName != null ? storeName : ""; }
    public void   setStoreName(String s)    { this.storeName = s; }
    public float  getRating()               { return rating; }
    public void   setRating(float r)        { this.rating = r; }

    // kept for compilation compat
    public int getImageResId() { return 0; }
}
