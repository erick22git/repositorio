const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('bd_kardex', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Estudiante = sequelize.define('Estudiante', {
  ru:         { type: Sequelize.STRING(10), allowNull: false, unique: true },
  nombre:     { type: Sequelize.STRING(120), allowNull: false },
  carrera:    { type: Sequelize.STRING(80), allowNull: false },
  habilitado: { type: Sequelize.BOOLEAN, allowNull: false, defaultValue: true },
  deuda:      { type: Sequelize.FLOAT, allowNull: false, defaultValue: 0 }
}, {
  tableName: 'estudiantes',
  timestamps: false
});

module.exports = { sequelize, Estudiante };
