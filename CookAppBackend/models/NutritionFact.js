const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const NutritionFact = sequelize.define('NutritionFact', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    recipe_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    calories: { type: DataTypes.INTEGER },
    protein: { type: DataTypes.FLOAT },
    fat: { type: DataTypes.FLOAT },
    carbs: { type: DataTypes.FLOAT },
    fiber: { type: DataTypes.FLOAT },
    sugar: { type: DataTypes.FLOAT },
    sodium: { type: DataTypes.FLOAT }
}, {
    timestamps: false,
    tableName: 'nutrition_facts'
});

module.exports = NutritionFact;
