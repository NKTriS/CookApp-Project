const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

// Bảng trung gian M-N giữa Recipe và Category
const RecipeCategory = sequelize.define('RecipeCategory', {
    recipe_id: {
        type: DataTypes.INTEGER,
        primaryKey: true
    },
    category_id: {
        type: DataTypes.INTEGER,
        primaryKey: true
    }
}, {
    timestamps: false,
    tableName: 'recipe_categories'
});

module.exports = RecipeCategory;
