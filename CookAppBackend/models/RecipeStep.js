const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

// Bảng recipe_steps lưu từng bước nấu của một công thức.
// Đây là bảng quan trọng cho Cooking Mode vì chứa hướng dẫn, timer và mốc video.
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
    // Số giây hẹn giờ cho bước nấu; Android hiển thị CountDownTimer từ trường này.
    timer_seconds: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    },
    // Mốc giây bắt đầu trong video tổng; Android dùng để tua/cắt đúng đoạn của bước.
    video_start_time: {
        type: DataTypes.INTEGER,
        defaultValue: 0
    }
}, {
    timestamps: false,
    tableName: 'recipe_steps'
});

module.exports = RecipeStep;
