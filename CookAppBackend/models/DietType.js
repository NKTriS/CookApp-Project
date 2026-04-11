const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const DietType = sequelize.define('DietType', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    name: {
        type: DataTypes.STRING,
        allowNull: false
    }
}, {
    timestamps: false,
    tableName: 'diet_types'
});

module.exports = DietType;
