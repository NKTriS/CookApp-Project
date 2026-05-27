const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

// Bảng orders lưu đơn mua nguyên liệu/sản phẩm trong app.
// Admin Panel dùng bảng này để theo dõi đơn và cập nhật trạng thái giao hàng.
const Order = sequelize.define('Order', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    user_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    customerName: {
        type: DataTypes.STRING,
        allowNull: false
    },
    phone: {
        type: DataTypes.STRING,
        allowNull: false
    },
    address: {
        type: DataTypes.STRING,
        allowNull: false
    },
    totalPrice: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    },
    shippingFee: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    },
    itemsSummary: {
        type: DataTypes.TEXT
    },
    // Trạng thái nghiệp vụ của đơn hàng, được cập nhật từ màn hình Admin Orders.
    status: {
        type: DataTypes.STRING,
        defaultValue: 'Chờ xác nhận'   // Chờ xác nhận → Đang giao → Hoàn thành / Đã hủy
    },
    paymentMethod: {
        type: DataTypes.STRING,
        defaultValue: 'COD'
    },
    note: {
        type: DataTypes.TEXT
    },
    cancelReason: {
        type: DataTypes.TEXT
    },
    cancelledAt: {
        type: DataTypes.DATE
    },
    paymentStatus: {
        type: DataTypes.STRING,
        defaultValue: null  // null for COD, 'pending'/'paid'/'expired' for bank transfer
    },
    paymentCode: {
        type: DataTypes.STRING,
        unique: true,
        defaultValue: null  // e.g. "COOKAPP42"
    },
    paidAt: {
        type: DataTypes.DATE,
        defaultValue: null
    },
    sepayTransactionId: {
        type: DataTypes.STRING,
        defaultValue: null  // SePay transaction ID to prevent duplicates
    }
}, {
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: 'updated_at',
    tableName: 'orders'
});

module.exports = Order;
