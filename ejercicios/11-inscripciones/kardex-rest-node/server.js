const express = require('express');
const cors = require('cors');
const { sequelize, Estudiante } = require('./database');

const app = express();
const PUERTO = 3000;

app.use(cors());
app.use(express.json());

app.get('/api/estudiantes', async (req, res) => {
  const where = {};
  if (req.query.carrera) where.carrera = req.query.carrera;
  res.json(await Estudiante.findAll({ where, order: [['ru', 'ASC']] }));
});

app.get('/api/estudiantes/:ru', async (req, res) => {
  const e = await Estudiante.findOne({ where: { ru: req.params.ru } });
  if (!e) return res.status(404).json({ mensaje: 'No existe el estudiante con RU ' + req.params.ru });
  res.json(e);
});

app.post('/api/estudiantes', async (req, res) => {
  const { ru, nombre, carrera, habilitado, deuda } = req.body || {};
  if (!ru || !nombre || !carrera) {
    return res.status(400).json({ mensaje: 'ru, nombre y carrera son obligatorios' });
  }
  if (deuda !== undefined && (isNaN(Number(deuda)) || Number(deuda) < 0)) {
    return res.status(400).json({ mensaje: 'La deuda debe ser un numero mayor o igual a 0' });
  }
  if (await Estudiante.findOne({ where: { ru } })) {
    return res.status(409).json({ mensaje: 'Ya existe un estudiante con RU ' + ru });
  }
  const nuevo = await Estudiante.create({
    ru, nombre, carrera,
    habilitado: habilitado === undefined ? true : !!habilitado,
    deuda: deuda === undefined ? 0 : Number(deuda)
  });
  res.status(201).json(nuevo);
});

app.patch('/api/estudiantes/:ru', async (req, res) => {
  const e = await Estudiante.findOne({ where: { ru: req.params.ru } });
  if (!e) return res.status(404).json({ mensaje: 'No existe el estudiante con RU ' + req.params.ru });
  const cambios = {};
  if (req.body.deuda !== undefined) {
    if (isNaN(Number(req.body.deuda)) || Number(req.body.deuda) < 0) {
      return res.status(400).json({ mensaje: 'La deuda debe ser un numero mayor o igual a 0' });
    }
    cambios.deuda = Number(req.body.deuda);
  }
  if (req.body.habilitado !== undefined) cambios.habilitado = !!req.body.habilitado;
  if (req.body.nombre) cambios.nombre = req.body.nombre;
  res.json(await e.update(cambios));
});

async function iniciar() {
  await sequelize.authenticate();
  await sequelize.sync();
  if (await Estudiante.count() === 0) {
    await Estudiante.bulkCreate([
      { ru: '111111', nombre: 'Jhonn Llanos Rojas', carrera: 'Ciencias de la Computacion' },
      { ru: '222222', nombre: 'Erick Mendoza Vargas', carrera: 'Ciencias de la Computacion' },
      { ru: '333333', nombre: 'Maria Flores Cruz', carrera: 'Ingenieria de Sistemas', deuda: 350 },
      { ru: '444444', nombre: 'Carlos Rojas Paz', carrera: 'Ingenieria de Sistemas', habilitado: false },
      { ru: '555555', nombre: 'Lucia Gareca Soto', carrera: 'Ciencias de la Computacion' }
    ]);
  }
  app.listen(PUERTO, () => console.log(`Kardex REST en http://localhost:${PUERTO}/api/estudiantes`));
}

iniciar().catch(err => console.error('No pude conectar a la base de datos:', err.message));
