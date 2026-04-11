const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const PostLike = sequelize.define('PostLike', {
    user_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    post_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    }
}, {
    tableName: 'post_likes',
    timestamps: true,
    createdAt: 'created_at',
    updatedAt: false
});

module.exports = PostLike;
