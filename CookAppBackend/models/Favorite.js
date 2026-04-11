const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Favorite = sequelize.define('Favorite', {
    user_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    recipe_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    }
}, {
    tableName: 'favorites',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: false
});

module.exports = Favorite;
