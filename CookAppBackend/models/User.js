const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

// Bảng users lưu tài khoản đăng nhập và vai trò phân quyền.
// Admin Panel dựa vào trường role để cho phép hoặc chặn quyền quản trị.
const User = sequelize.define('User', {
    email: {
        type: DataTypes.STRING,
        allowNull: false,
        unique: true
    },
    password: {
        type: DataTypes.STRING,
        allowNull: false
    },
    fullName: {
        type: DataTypes.STRING,
        allowNull: false
    },
    phoneNumber: DataTypes.STRING,
    address: DataTypes.STRING,
    avatarUrl: DataTypes.STRING,
    // role = 'admin' được phép truy cập các API /api/admin/*; role = 'user' là người dùng thường.
    role: {
        type: DataTypes.STRING,
        defaultValue: 'user'   // 'user' | 'admin'
    }
}, {
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: false
});

module.exports = User;
