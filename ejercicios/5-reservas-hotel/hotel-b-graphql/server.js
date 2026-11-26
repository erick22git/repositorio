const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const schema = require('./schema');
const { sequelize, Habitacion } = require('./database');

const app = express();

app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

app.use('/graphql', graphqlHTTP({ schema, graphiql: true }));

async function iniciar() {
  await sequelize.authenticate();
  await sequelize.sync();
  if (await Habitacion.count() === 0) {
    await Habitacion.bulkCreate([
      { codHabitacion: 'B101', tipo: 'Simple', capacidad: 1, tarifa: 180 },
      { codHabitacion: 'B201', tipo: 'Suite', capacidad: 3, tarifa: 400 }
    ]);
  }
  app.listen(4000, () => console.log('Hotel B (GraphQL) en http://localhost:4000/graphql'));
}

iniciar().catch(err => console.error('No pude conectar a la base de datos:', err.message));
