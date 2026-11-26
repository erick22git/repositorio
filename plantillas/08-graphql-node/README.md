# 08 · GraphQL en Node

[← Plantillas](../README.md) · [Índice](../../README.md) · **[INGE]**

**Qué es:** **una sola URL** (`/graphql`) donde el cliente pide exactamente los campos que quiere. *Query* = leer, *Mutation* = cambiar.

| Archivo | Qué hace |
|---|---|
| `database.js` | Conexión a MySQL y el modelo |
| `schema.js` | Tipos, queries y mutations |
| `server.js` | Levanta el servidor en el 4000 |
| `package.json` | Dependencias y comando `dev` |

## Pasos (comandos [INGE])
1. En phpMyAdmin crea la base `bd_examen`.
2. Con internet:
```bash
npm init -y
npm install express express-graphql graphql mysql2 sequelize
npm install -D nodemon
```
Sin internet: [copia `base-graphql`](../../00-preparacion/README.md#d3-proyecto-node-graphql-nuevo).

3. En `package.json` → `"scripts": { "dev": "nodemon server.js" }`.
4. Pega los tres `.js`.
5. `npm run dev` → abre `http://localhost:4000/graphql` (pantalla **GraphiQL**).

La tabla `recursos_graphql` y dos datos de prueba se crean solos.

## database.js
```javascript
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
```

## schema.js
```javascript
const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Recurso } = require('./database');

// 1) TIPO: que campos se pueden pedir
const RecursoType = new GraphQLObjectType({
  name: 'Recurso',
  fields: {
    id:     { type: GraphQLString },
    codigo: { type: GraphQLString },
    nombre: { type: GraphQLString },
    monto:  { type: GraphQLFloat },
    estado: { type: GraphQLString }
  }
});

// 2) QUERIES: leer
const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    recursos: {
      type: new GraphQLList(RecursoType),
      resolve() {
        return Recurso.findAll();
      }
    },
    recurso: {
      type: RecursoType,
      args: { codigo: { type: new GraphQLNonNull(GraphQLString) } },
      async resolve(parent, args) {
        const r = await Recurso.findOne({ where: { codigo: args.codigo } });
        if (!r) throw new Error('No existe ' + args.codigo);
        // AQUI LOGICA
        return r;
      }
    }
  }
});

// 3) MUTATIONS: crear / cambiar
const Mutation = new GraphQLObjectType({
  name: 'Mutation',
  fields: {
    crearRecurso: {
      type: RecursoType,
      args: {
        codigo: { type: new GraphQLNonNull(GraphQLString) },
        nombre: { type: new GraphQLNonNull(GraphQLString) },
        monto:  { type: GraphQLFloat }
      },
      resolve(parent, args) {
        // AQUI LOGICA (validaciones antes de guardar)
        return Recurso.create(args);
      }
    },
    cambiarEstado: {
      type: RecursoType,
      args: {
        codigo: { type: new GraphQLNonNull(GraphQLString) },
        estado: { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, args) {
        const r = await Recurso.findOne({ where: { codigo: args.codigo } });
        if (!r) throw new Error('No existe ' + args.codigo);
        // AQUI LOGICA
        return r.update({ estado: args.estado });
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery, mutation: Mutation });
```

## server.js
```javascript
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
```

## package.json
```json
{
  "name": "graphql-examen",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "serve": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.19.2",
    "express-graphql": "^0.12.0",
    "graphql": "^15.8.1",
    "mysql2": "^3.9.7",
    "sequelize": "^6.37.3"
  },
  "devDependencies": {
    "nodemon": "^3.1.10"
  }
}
```

## Probar en GraphiQL
```graphql
{ recursos { codigo nombre monto estado } }
```
```graphql
{ recurso(codigo: "A1") { nombre estado } }
```
```graphql
mutation { crearRecurso(codigo: "B1", nombre: "Nuevo", monto: 5) { codigo estado } }
```
```graphql
mutation { cambiarEstado(codigo: "A1", estado: "pagado") { codigo estado } }
```
Con variables:
```graphql
query($c: String!) { recurso(codigo: $c) { nombre } }
```
y en el panel *Query Variables*: `{ "c": "A1" }`

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Otro campo | Agregarlo en **dos** lugares: modelo (`database.js`) y tipo (`schema.js`) |
| Filtrar o borrar | [logica/graphql](../../logica/README.md#graphql-filtrar-y-borrar) |
| Sin base de datos | Arreglo en memoria → [ejercicio 6](../../ejercicios/6-central-pagos/README.md) |
| Leer la tabla que creó Laravel | Misma base, `tableName` igual y **sin** `sync()` → [ejercicio 4](../../ejercicios/4-multas/README.md) |
| Consumirlo | [logica/consumir](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |

> `sequelize.sync()` crea la tabla si no existe, pero **no** le agrega columnas nuevas. Si cambias el modelo, borra la tabla en phpMyAdmin.

## Cómo defenderlo
- "El **schema** define los tipos; cada campo tiene un **resolve** que trae el dato."
- "A diferencia de REST hay **un solo endpoint** y el cliente evita traer datos de más o de menos."
- "`GraphQLNonNull` = obligatorio, `GraphQLList` = arreglo."
- "Los errores llegan en `errors` y **normalmente con HTTP 200**, por eso el cliente debe mirar ese campo."
- "Sequelize es un ORM: trabajo con objetos en vez de SQL."
