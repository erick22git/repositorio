const express = require('express');
const cors = require('cors');
const { sequelize, Cotizacion } = require('./database');

const app = express();
app.use(cors());
app.use(express.json());

const OFICIAL = 6.97;

function fechaValida(fecha) {
  if (typeof fecha !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(fecha)) return false;
  const d = new Date(fecha + 'T00:00:00Z');
  return !isNaN(d) && d.toISOString().slice(0, 10) === fecha;
}

function formato(c) {
  return { fecha: c.fecha, cotizacion: Number(c.cotizacion), cotizacion_oficial: Number(c.cotizacion_oficial) };
}

// GET /api/cotizaciones
app.get('/api/cotizaciones', async (req, res) => {
  const lista = await Cotizacion.findAll({ order: [['fecha', 'ASC']] });
  res.json(lista.map(formato));
});

// GET /api/cotizaciones/2025-04-04
app.get('/api/cotizaciones/:fecha', async (req, res) => {
  const { fecha } = req.params;
  if (!fechaValida(fecha)) return res.status(400).json({ exito: false, mensaje: 'Fecha invalida, use AAAA-MM-DD' });
  const c = await Cotizacion.findOne({ where: { fecha } });
  if (!c) return res.status(404).json({ exito: false, mensaje: 'No se encontro la cotizacion para ' + fecha });
  res.json({ exito: true, mensaje: 'Cotizacion encontrada', cotizacion: formato(c) });
});

// POST /api/cotizaciones   { "fecha": "2025-04-13", "monto": 9.35 }
app.post('/api/cotizaciones', async (req, res) => {
  const { fecha, monto } = req.body || {};
  const valor = Number(monto);
  if (!fechaValida(fecha)) return res.status(400).json({ exito: false, mensaje: 'Fecha invalida, use AAAA-MM-DD' });
  if (monto === undefined || monto === null || monto === '' || isNaN(valor) || valor <= 0) {
    return res.status(400).json({ exito: false, mensaje: 'El monto debe ser un numero mayor a 0' });
  }
  if (await Cotizacion.findOne({ where: { fecha } })) {
    return res.status(409).json({ exito: false, mensaje: 'Ya existe una cotizacion para ' + fecha });
  }
  const c = await Cotizacion.create({ fecha, cotizacion: Math.round(valor * 100) / 100, cotizacion_oficial: OFICIAL });
  res.status(201).json({ exito: true, mensaje: 'Cotizacion registrada', cotizacion: formato(c) });
});

app.use((err, req, res, next) => res.status(400).json({ exito: false, mensaje: 'JSON invalido' }));

sequelize.authenticate()
  .then(() => app.listen(3000, () => console.log('REST en http://localhost:3000/api/cotizaciones')))
  .catch(err => console.error('No pude conectar a la base de datos:', err.message));
