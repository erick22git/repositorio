const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('bd_hotel_b', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Habitacion = sequelize.define('Habitacion', {
  codHabitacion: { type: Sequelize.STRING(10), allowNull: false, unique: true },
  tipo:          { type: Sequelize.STRING(30), allowNull: false },
  capacidad:     { type: Sequelize.INTEGER, allowNull: false },
  tarifa:        { type: Sequelize.FLOAT, allowNull: false },
  disponible:    { type: Sequelize.BOOLEAN, allowNull: false, defaultValue: true }
}, {
  tableName: 'habitaciones',
  timestamps: false
});

module.exports = { sequelize, Habitacion };
