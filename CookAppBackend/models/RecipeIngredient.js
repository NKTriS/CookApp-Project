const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const RecipeIngredient = sequelize.define('RecipeIngredient', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    recipe_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    ingredient_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    quantity: {
        type: DataTypes.FLOAT
    },
    unit: {
        type: DataTypes.STRING
    }
}, {
    timestamps: false,
    tableName: 'recipe_ingredients'
});

module.exports = RecipeIngredient;
