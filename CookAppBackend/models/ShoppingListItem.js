const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const ShoppingListItem = sequelize.define('ShoppingListItem', {
    shopping_list_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    ingredient_name: {
        type: DataTypes.STRING,
        allowNull: false
    },
    quantity: {
        type: DataTypes.FLOAT,
        allowNull: false
    },
    unit: {
        type: DataTypes.STRING,
        allowNull: false
    },
    checked: {
        type: DataTypes.BOOLEAN,
        defaultValue: false
    },
    price: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    }
}, {
    tableName: 'shopping_list_items',
    timestamps: false
});

module.exports = ShoppingListItem;
