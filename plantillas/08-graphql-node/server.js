const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const schema = require('./schema');
const { sequelize, Recurso } = require('./database');

const app = express();
const PUERTO = 4000;

// CORS: necesario si una pagina web (HTML/JS) va a consumir este servidor
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

app.use('/graphql', graphqlHTTP({ schema, graphiql: true }));

async function iniciar() {
  await sequelize.authenticate();
  await sequelize.sync();                 // crea la tabla si no existe (no borra datos)
  if (await Recurso.count() === 0) {      // datos de prueba la primera vez
    await Recurso.bulkCreate([
      { codigo: 'A1', nombre: 'Ejemplo uno', monto: 100 },
      { codigo: 'A2', nombre: 'Ejemplo dos', monto: 200 }
    ]);
  }
  app.listen(PUERTO, () => console.log(`GraphQL en http://localhost:${PUERTO}/graphql`));
}

iniciar().catch(err => console.error('No pude conectar a la base de datos:', err.message));
