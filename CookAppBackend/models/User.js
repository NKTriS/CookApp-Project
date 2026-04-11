const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

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
