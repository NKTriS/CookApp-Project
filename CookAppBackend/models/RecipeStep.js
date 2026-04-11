const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const RecipeStep = sequelize.define('RecipeStep', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    recipe_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    step_number: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    title: {
        type: DataTypes.STRING,
        allowNull: true
    },
    instruction: {
        type: DataTypes.TEXT,
        allowNull: false
    },
    timer_seconds: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    },
    video_start_time: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    }
}, {
    timestamps: false,
    tableName: 'recipe_steps'
});

module.exports = RecipeStep;
