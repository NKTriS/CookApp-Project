const { Sequelize } = require('sequelize');
const path = require('path');

// Configure Sequelize to use SQLite locally
const sequelize = new Sequelize({
  dialect: 'sqlite',
  storage: path.join(__dirname, '..', 'database.sqlite'),
  logging: false // Disable logging for cleaner console output
});

module.exports = sequelize;
