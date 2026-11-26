const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('eleccion_api', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const opciones = { timestamps: false };

const Candidato = sequelize.define('Candidato', {
  nombres:   Sequelize.STRING,
  apellidos: Sequelize.STRING,
  cargo:     Sequelize.STRING,
  ambito:    Sequelize.STRING
}, { ...opciones, tableName: 'candidatos' });

const Mesa = sequelize.define('Mesa', {
  numero:      Sequelize.INTEGER,
  recinto:     Sequelize.STRING,
  habilitados: Sequelize.INTEGER
}, { ...opciones, tableName: 'mesas' });

const Acta = sequelize.define('Acta', {
  mesa_id: Sequelize.INTEGER,
  cargo:   Sequelize.STRING,
  blancos: Sequelize.INTEGER,
  nulos:   Sequelize.INTEGER
}, { ...opciones, tableName: 'actas' });

const Voto = sequelize.define('Voto', {
  acta_id:      Sequelize.INTEGER,
  candidato_id: Sequelize.INTEGER,
  cantidad:     Sequelize.INTEGER
}, { ...opciones, tableName: 'votos' });

Acta.belongsTo(Mesa, { foreignKey: 'mesa_id' });
Mesa.hasOne(Acta, { foreignKey: 'mesa_id' });
Acta.hasMany(Voto, { foreignKey: 'acta_id' });
Voto.belongsTo(Candidato, { foreignKey: 'candidato_id' });

module.exports = { sequelize, Candidato, Mesa, Acta, Voto };
