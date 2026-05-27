const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

// Bảng recipes lưu thông tin chính của công thức, bao gồm ảnh, video hướng dẫn
// và các thuộc tính dùng để lọc/tìm kiếm trong app Android.
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
    // URL video nấu ăn; Android dùng để mở VideoPlayerActivity và CookingModeActivity.
    video_url: {
        type: DataTypes.STRING
    },
    // Ảnh thumbnail của video nếu backend/admin có tạo riêng.
    video_thumbnail_url: {
        type: DataTypes.STRING
    }
}, {
    timestamps: false,
    tableName: 'recipes'
});

module.exports = Recipe;
