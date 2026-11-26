# 13 · API REST en Node (Express)

[← Plantillas](../README.md) · [Índice](../../README.md) · **[EXTRA]** (la Práctica 5 pidió "REST con Node.js")

**Qué es:** el mismo CRUD de la [plantilla 06](../06-rest-laravel/README.md) pero en Node: `express` para las rutas y `sequelize` para MySQL (igual que el GraphQL del docente).

| Archivo | Qué hace |
|---|---|
| `database.js` | Conexión a MySQL y el modelo `Recurso` (tabla `recursos_rest`) |
| `server.js` | Las 5 rutas REST en el puerto **3000** |
| `package.json` | Dependencias y `npm run dev` |

## Pasos
1. phpMyAdmin → crear la base `bd_examen`.
2. Sin internet: `C:\guia\00-preparacion\sin-internet\nuevo-node.bat rest-examen` y `cd C:\examen\rest-examen` ([detalle](../../00-preparacion/sin-internet/README.md)).
   Con internet: `npm init -y`, `npm install express@4 mysql2 sequelize cors` y `npm install -D nodemon`.
3. Pega `database.js` y `server.js`.
4. `npm run dev` → `http://localhost:3000/api/recursos` (la tabla y 2 datos se crean solos).

## database.js
```js
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
  tableName: 'recursos_rest',   // nombre de la tabla en MySQL
  timestamps: false
});

module.exports = { sequelize, Recurso };
```

## server.js
```js
const express = require('express');
const cors = require('cors');
const { sequelize, Recurso } = require('./database');

const app = express();
const PUERTO = 3000;

app.use(cors());
app.use(express.json());

// GET /api/recursos            GET /api/recursos?estado=activo
app.get('/api/recursos', async (req, res) => {
  const where = {};
  if (req.query.estado) where.estado = req.query.estado;
  res.json(await Recurso.findAll({ where }));
});

// GET /api/recursos/A1
app.get('/api/recursos/:codigo', async (req, res) => {
  const r = await Recurso.findOne({ where: { codigo: req.params.codigo } });
  if (!r) return res.status(404).json({ error: 'No existe ' + req.params.codigo });
  res.json(r);
});

// POST /api/recursos   { "codigo": "B1", "nombre": "Nuevo", "monto": 5 }
app.post('/api/recursos', async (req, res) => {
  const { codigo, nombre, monto } = req.body || {};
  if (!codigo || !nombre || isNaN(Number(monto))) {
    return res.status(422).json({ error: 'Faltan codigo, nombre o monto numerico' });
  }
  if (await Recurso.findOne({ where: { codigo } })) {
    return res.status(409).json({ error: 'El codigo ya existe' });
  }
  // AQUI LOGICA (reglas antes de guardar)
  const nuevo = await Recurso.create({ codigo, nombre, monto: Number(monto) });
  res.status(201).json(nuevo);
});

// PUT o PATCH /api/recursos/A1   { "estado": "pagado" }
async function actualizar(req, res) {
  const r = await Recurso.findOne({ where: { codigo: req.params.codigo } });
  if (!r) return res.status(404).json({ error: 'No existe ' + req.params.codigo });
  const cambios = {};
  for (const campo of ['nombre', 'monto', 'estado']) {
    if (req.body[campo] !== undefined) cambios[campo] = req.body[campo];
  }
  // AQUI LOGICA
  res.json(await r.update(cambios));
}
app.put('/api/recursos/:codigo', actualizar);
app.patch('/api/recursos/:codigo', actualizar);

// DELETE /api/recursos/A1
app.delete('/api/recursos/:codigo', async (req, res) => {
  const borrados = await Recurso.destroy({ where: { codigo: req.params.codigo } });
  if (!borrados) return res.status(404).json({ error: 'No existe ' + req.params.codigo });
  res.status(204).send();
});

// JSON mal escrito en el cuerpo
app.use((err, req, res, next) => res.status(400).json({ error: 'JSON invalido' }));

async function iniciar() {
  await sequelize.authenticate();
  await sequelize.sync();
  if (await Recurso.count() === 0) {
    await Recurso.bulkCreate([
      { codigo: 'A1', nombre: 'Ejemplo uno', monto: 100 },
      { codigo: 'A2', nombre: 'Ejemplo dos', monto: 200 }
    ]);
  }
  app.listen(PUERTO, () => console.log(`REST en http://localhost:${PUERTO}/api/recursos`));
}

iniciar().catch(err => console.error('No pude conectar a la base de datos:', err.message));
```

## package.json
```json
{
  "name": "rest-examen",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "serve": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "cors": "^2.8.5",
    "express": "^4.19.2",
    "mysql2": "^3.9.7",
    "sequelize": "^6.37.3"
  },
  "devDependencies": {
    "nodemon": "^3.1.10"
  }
}
```

## Probar (símbolo del sistema)
```bat
curl http://localhost:3000/api/recursos
curl -X POST http://localhost:3000/api/recursos -H "Content-Type: application/json" -d "{\"codigo\":\"B1\",\"nombre\":\"Nuevo\",\"monto\":5}"
curl -X PATCH http://localhost:3000/api/recursos/A1 -H "Content-Type: application/json" -d "{\"estado\":\"pagado\"}"
curl "http://localhost:3000/api/recursos?estado=pagado"
curl -X DELETE http://localhost:3000/api/recursos/A2
```

| Prueba | Responde |
|---|---|
| POST nuevo | **201** con el registro |
| POST con el mismo código | **409** |
| POST sin nombre o monto | **422** |
| GET o PATCH de un código que no existe | **404** |
| DELETE | **204** |
| JSON mal escrito | **400** |

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Otro recurso o campos | Cambia el modelo en `database.js` y los campos en `server.js` |
| Usar la tabla que creó Laravel | `tableName` igual, `timestamps: true, createdAt: 'created_at', updatedAt: 'updated_at'` y **quita** `sync()` |
| Protegerlo con JWT | Revisa el token en un `app.use(...)` antes de las rutas (mismo formato que [JwtJava](../../ejercicios/3-pago-en-linea/README.md)) |
| Llamar a otro servicio | `fetch` (Node 18+) → [logica/consumir.js](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |
| REST y GraphQL en el mismo servidor | Agrega `app.use('/graphql', graphqlHTTP({ schema, graphiql: true }))` de la [plantilla 08](../08-graphql-node/README.md) |

## Cómo defenderlo
- "`app.get`, `app.post`… asocian un **verbo HTTP** y una **ruta** con una función."
- "`express.json()` convierte el cuerpo JSON en `req.body`."
- "Respondo el código correcto: 201 creado, 404 no existe, 409 repetido, 422 datos malos, 204 borrado."
- "`cors()` deja que una página web de otro puerto lo consuma."
