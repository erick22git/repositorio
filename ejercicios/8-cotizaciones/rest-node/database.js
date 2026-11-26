const { Sequelize } = require('sequelize');

// MISMA base que GraphQL (Laravel); la tabla la crea la migracion de Laravel
const sequelize = new Sequelize('bcb_cotizaciones', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Cotizacion = sequelize.define('Cotizacion', {
  fecha:              { type: Sequelize.DATEONLY, allowNull: false, unique: true },
  cotizacion:         { type: Sequelize.DECIMAL(8, 2), allowNull: false },
  cotizacion_oficial: { type: Sequelize.DECIMAL(8, 2), allowNull: false }
}, {
  tableName: 'cotizaciones',
  timestamps: true,
  createdAt: 'created_at',
  updatedAt: 'updated_at'
});

module.exports = { sequelize, Cotizacion };
