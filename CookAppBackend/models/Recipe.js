const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Recipe = sequelize.define('Recipe', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    title: {
        type: DataTypes.STRING,
        allowNull: false
    },
    description: {
        type: DataTypes.TEXT
    },
    image_url: {
        type: DataTypes.STRING
    },
    cook_time: {
        type: DataTypes.INTEGER // Minutes
    },
    difficulty: {
        type: DataTypes.STRING
    },
    servings: {
        type: DataTypes.INTEGER
    },
    calories: {
        type: DataTypes.INTEGER
    },
    category_id: {
        type: DataTypes.INTEGER
    },
    diet_type_id: {
        type: DataTypes.INTEGER
    },
    video_url: {
        type: DataTypes.STRING
    },
    video_thumbnail_url: {
        type: DataTypes.STRING
    }
}, {
    timestamps: false,
    tableName: 'recipes'
});

module.exports = Recipe;
