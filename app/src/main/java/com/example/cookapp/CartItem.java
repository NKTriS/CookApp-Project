package com.example.cookapp;

public class CartItem {
    private String id;
    private String name;
    private String unit;     // đơn vị: "500g", "1 quả", v.v.
    private int price;       // đơn giá (VND)
    private int qty;

    public CartItem(String id, String name, String unit, int price, int qty) {
        this.id    = id;
        this.name  = name;
        this.unit  = unit != null ? unit : "";
        this.price = price;
        this.qty   = qty;
    }

    // Backward-compat constructor (không có unit)
    public CartItem(String id, String name, int price, int qty) {
        this(id, name, "", price, qty);
    }

    public String getId()    { return id; }
    public void setId(String id) { this.id = id; }

    public String getName()  { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit()  { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getPrice()    { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getQty()      { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    /** Thành tiền = đơn giá × số lượng */
    public int getSubtotal() { return price * qty; }
}
