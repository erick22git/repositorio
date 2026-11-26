const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('bd_clinica', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Cita = sequelize.define('Cita', {
  ci_paciente:  { type: Sequelize.STRING(20), allowNull: false },
  paciente:     { type: Sequelize.STRING(170), allowNull: false },
  especialidad: { type: Sequelize.STRING(50), allowNull: false },
  fecha:        { type: Sequelize.DATEONLY, allowNull: false },
  hora:         { type: Sequelize.STRING(5), allowNull: false },
  costo:        { type: Sequelize.FLOAT, allowNull: false, defaultValue: 0 },
  estado:       { type: Sequelize.STRING(20), allowNull: false, defaultValue: 'reservada' }
}, {
  tableName: 'citas',
  timestamps: false
});

module.exports = { sequelize, Cita };
