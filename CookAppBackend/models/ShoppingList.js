const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const ShoppingList = sequelize.define('ShoppingList', {
    user_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    }
}, {
    tableName: 'shopping_lists',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: false
});

module.exports = ShoppingList;
