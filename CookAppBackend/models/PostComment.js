const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const PostComment = sequelize.define('PostComment', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    post_id: {
        type: DataTypes.INTEGER,
        allowNull: false
    },
    author: {
        type: DataTypes.STRING
    },
    content: {
        type: DataTypes.TEXT,
        allowNull: false
    },
    created_at: {
        type: DataTypes.INTEGER // Timestamp
    }
}, {
    timestamps: false,
    tableName: 'post_comments'
});

module.exports = PostComment;
