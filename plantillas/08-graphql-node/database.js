const { Sequelize } = require('sequelize');

// (base de datos, usuario, clave, opciones)  -> XAMPP: usuario root, sin clave
const sequelize = new Sequelize('bd_examen', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

// CAMBIAR campos segun el enunciado
const Recurso = sequelize.define('Recurso', {
  codigo: { type: Sequelize.STRING(30), allowNull: false, unique: true },
  nombre: { type: Sequelize.STRING(100), allowNull: false },
  monto:  { type: Sequelize.FLOAT, allowNull: false, defaultValue: 0 },
  estado: { type: Sequelize.STRING(20), allowNull: false, defaultValue: 'activo' }
}, {
  tableName: 'recursos_graphql',   // nombre de la tabla en MySQL
  timestamps: false
});

module.exports = { sequelize, Recurso };
