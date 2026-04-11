const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

/**
 * StoreProduct — sản phẩm từ các siêu thị / sàn TMĐT
 * Mỗi nguyên liệu có thể có nhiều sản phẩm từ nhiều cửa hàng khác nhau.
 */
const StoreProduct = sequelize.define('StoreProduct', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },

    // Tên nguyên liệu gốc (để match với ingredient trong công thức)
    ingredient_name: { type: DataTypes.STRING, allowNull: false },

    // Tên sản phẩm thực tế trên sàn (có thể khác ingredient_name)
    product_name: { type: DataTypes.STRING, allowNull: false },

    // Đơn vị bán: "500g", "1 kg", "1 quả", ...
    unit: { type: DataTypes.STRING },

    // Giá bán (VND)
    price_dong: { type: DataTypes.INTEGER, defaultValue: 0 },

    // Tên cửa hàng / sàn
    store_name: { type: DataTypes.STRING },

    // Logo / icon cửa hàng (URL)
    store_logo_url: { type: DataTypes.STRING },

    // Ảnh sản phẩm
    image_url: { type: DataTypes.STRING },

    // Còn hàng hay không
    in_stock: { type: DataTypes.BOOLEAN, defaultValue: true },

    // Đánh giá trung bình (1-5)
    rating: { type: DataTypes.FLOAT, defaultValue: 4.5 },
}, {
    timestamps: false,
    tableName: 'store_products'
});

module.exports = StoreProduct;
