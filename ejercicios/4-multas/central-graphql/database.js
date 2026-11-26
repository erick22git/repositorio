const { Sequelize } = require('sequelize');

// MISMA base de datos que Laravel: asi REST, GraphQL y TCP ven lo mismo
const sequelize = new Sequelize('bd_multas', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Multa = sequelize.define('Multa', {
  codigo: { type: Sequelize.STRING },
  ci:     { type: Sequelize.STRING },
  nombre: { type: Sequelize.STRING },
  fecha:  { type: Sequelize.DATEONLY },
  tipo:   { type: Sequelize.STRING },
  lugar:  { type: Sequelize.STRING },
  monto:  { type: Sequelize.FLOAT },
  estado: { type: Sequelize.STRING }
}, {
  tableName: 'multas',     // la tabla que creo la migracion de Laravel
  timestamps: false
});

module.exports = { sequelize, Multa };
