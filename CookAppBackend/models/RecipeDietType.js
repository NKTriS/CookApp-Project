const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const RecipeDietType = sequelize.define('RecipeDietType', {
    recipe_id: {
        type: DataTypes.INTEGER,
        references: {
            model: 'recipes',
            key: 'id'
        }
    },
    diet_type_id: {
        type: DataTypes.INTEGER,
        references: {
            model: 'diet_types',
            key: 'id'
        }
    }
}, {
    timestamps: false,
    tableName: 'recipe_diet_types'
});

module.exports = RecipeDietType;
