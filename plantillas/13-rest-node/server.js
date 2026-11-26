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
