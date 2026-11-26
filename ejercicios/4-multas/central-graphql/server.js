const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const schema = require('./schema');
const { sequelize } = require('./database');

const app = express();

app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

app.use('/graphql', graphqlHTTP({ schema, graphiql: true }));

// Sin sync(): la tabla la crea Laravel con "php artisan migrate:fresh --seed"
sequelize.authenticate()
  .then(() => app.listen(4000, () => console.log('GraphQL en http://localhost:4000/graphql')))
  .catch(err => console.error('No pude conectar a la base de datos:', err.message));
