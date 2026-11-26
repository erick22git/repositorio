# Ejercicio 11 · Inscripción de materias: GraphQL en Laravel que consulta REST en Node + cliente web

[← Ejercicios](../README.md) · [Índice](../../README.md) · **[VARIANTE TIPO EXAMEN]** · probado (Node, Laravel y la página web)

Practica la combinación **al revés** del ejercicio 10: aquí el **GraphQL está en Laravel** y es **Laravel quien llama** a un REST hecho en **Node** (con `Http::get`). El cliente es una **página web** (HTML + JavaScript `fetch`).

## Enunciado (inventado, estilo examen)
La universidad tiene dos sistemas:
1. **Kardex** (REST en Node + Express, base `bd_kardex`): datos del estudiante por RU — nombre, carrera, si está **habilitado** y su **deuda**.
2. **Inscripciones** (GraphQL en Laravel, base `bd_inscripciones`): materias ofertadas con **cupo** e inscripciones de la gestión 2/2026.

`inscribir(ru, sigla)` debe consultar el kardex y rechazar si:
- el estudiante no existe, no está habilitado o **tiene deuda** (muestra cuánto debe);
- ya está inscrito en esa materia;
- ya tiene **4 materias** en la gestión;
- la materia no tiene cupo, o no existe;
- el kardex está apagado (mensaje claro, sin que se caiga Laravel).

También: `retirar(ru, sigla)`, `crearMateria(...)`, y consultas `materias`, `materia(sigla)` (con inscritos, disponibles y lista de estudiantes) e `inscripcionesDe(ru)`.

Cliente web: escribir el RU, ver materias con cupo libre, inscribirse y retirarse con botones.

```
 Navegador (inscripciones.html) ──GraphQL──> inscripciones-graphql-laravel :8000 ──> MySQL bd_inscripciones
                                                       │
                                                       └──REST (Http::get)──> kardex-rest-node :3000 ──> MySQL bd_kardex
```

## Orden de arranque
1. XAMPP → MySQL. phpMyAdmin → crear **`bd_kardex`** y **`bd_inscripciones`**.
2. `kardex-rest-node` (crea su tabla y 5 estudiantes la primera vez) → 3. `inscripciones-graphql-laravel` → 4. abrir la página.

### Paso 1 · Kardex REST en Node
```bat
C:\guia\00-preparacion\sin-internet\nuevo-node.bat kardex
cd C:\examen\kardex
```
Copia `database.js` y `server.js` (y reemplaza `package.json` si quieres). Con internet: `npm install express@4 mysql2 sequelize cors`.
```bash
node server.js
```
Estudiantes de prueba:

| RU | Nombre | Situación |
|---|---|---|
| 111111 | Jhonn Llanos Rojas | puede inscribirse |
| 222222 | Erick Mendoza Vargas | puede inscribirse |
| 333333 | Maria Flores Cruz | **debe Bs 350** |
| 444444 | Carlos Rojas Paz | **no habilitado** |
| 555555 | Lucia Gareca Soto | puede inscribirse |

Prueba en Postman / navegador:

| Petición | Esperado |
|---|---|
| GET `http://localhost:3000/api/estudiantes/333333` | 200 con `deuda: 350` |
| GET `.../api/estudiantes/9` | 404 |
| POST `.../api/estudiantes` `{"ru":"111111","nombre":"x","carrera":"y"}` | 409 |
| POST `.../api/estudiantes` `{"ru":"666666"}` | 400 |
| POST `.../api/estudiantes` `{"ru":"666666","nombre":"Ana Paz","carrera":"Sistemas"}` | 201 |
| PATCH `.../api/estudiantes/333333` `{"deuda":0}` | 200 (ya puede inscribirse) |

### Paso 2 · Inscripciones GraphQL en Laravel
```bat
C:\guia\00-preparacion\sin-internet\nuevo-laravel.bat inscripciones
cd C:\examen\inscripciones
```
`.env`: `DB_DATABASE=bd_inscripciones`. (Con internet: `composer require webonyx/graphql-php`. La base offline ya lo trae.)
```bash
php artisan install:api
php artisan make:model Materia -m
php artisan make:model Inscripcion -m
php artisan make:controller GraphQLController --invokable
```
Crea la carpeta `app/GraphQL` y pega `Esquema.php`. Pega el resto de archivos (primero la migración de **materias**, después la de **inscripciones**).
```bash
php artisan migrate:fresh --seed
php artisan serve
```
La materia **SIS258 tiene cupo 3** a propósito, para probar "sin cupo" rápido.

> `Http` es el cliente HTTP que **ya trae Laravel** (no hay que instalar nada). Se usa igual para llamar a cualquier REST: `Http::withToken($token)->get(...)`, `Http::post($url, [...])`.

### Paso 3 · Probar
- Página: `http://localhost:8000/inscripciones.html` (está en `public/`, por eso no hay problema de CORS).
- Consola GraphQL: copia `public/graphiql.html` de la [plantilla 14](../../plantillas/14-graphql-laravel/README.md) y abre `http://localhost:8000/graphiql.html`.

```graphql
{ materias { sigla nombre cupo disponibles } }

mutation { inscribir(ru: "111111", sigla: "SIS258") { id estudiante materia { sigla } } }

{ materia(sigla: "SIS258") { inscritos disponibles estudiantes { ru estudiante } } }

{ inscripcionesDe(ru: "111111") { materia { sigla nombre horario } } }

mutation { retirar(ru: "111111", sigla: "SIS258") }
```

Resultado de la corrida real:
```
inscribir 111111 SIS258        {"id":1,"estudiante":"Jhonn Llanos Rojas","materia":{"sigla":"SIS258"}}
otra vez                       "El estudiante ya esta inscrito en SIS258"
333333 (debe)                  "El estudiante tiene una deuda pendiente de Bs 350"
444444                         "El estudiante no esta habilitado para inscribirse"
RU 999                         "No existe el estudiante con RU 999"
sigla XXX                      "No existe la materia XXX"
222222 y 555555                ok (se llena el cupo 3)
666666 SIS258                  "La materia SIS258 ya no tiene cupo"
111111 quinta materia          "No puede inscribirse a mas de 4 materias"
retirar dos veces              "Se retiro a Jhonn Llanos Rojas de SIS258" / "El estudiante no esta inscrito en SIS258"
crearMateria SIS258            "Ya existe la materia SIS258"
PATCH deuda 0 y reintentar     {"estudiante":"Maria Flores Cruz"}
kardex apagado                 "El servicio de kardex (Node) no responde"
página web (navegador real)    "Ana Paz inscrito en MAT207" / "La materia SIS258 ya no tiene cupo"
```

---

## Archivos

### kardex-rest-node

#### package.json
```json
{
  "name": "kardex-rest",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "serve": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "cors": "^2.8.5",
    "express": "^4.22.3",
    "mysql2": "^3.9.7",
    "sequelize": "^6.37.3"
  },
  "devDependencies": {
    "nodemon": "^3.1.10"
  }
}
```

#### database.js
```js
const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('bd_kardex', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Estudiante = sequelize.define('Estudiante', {
  ru:         { type: Sequelize.STRING(10), allowNull: false, unique: true },
  nombre:     { type: Sequelize.STRING(120), allowNull: false },
  carrera:    { type: Sequelize.STRING(80), allowNull: false },
  habilitado: { type: Sequelize.BOOLEAN, allowNull: false, defaultValue: true },
  deuda:      { type: Sequelize.FLOAT, allowNull: false, defaultValue: 0 }
}, {
  tableName: 'estudiantes',
  timestamps: false
});

module.exports = { sequelize, Estudiante };
```

#### server.js
```js
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
```

### inscripciones-graphql-laravel

#### database/migrations/…_create_materias_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('materias', function (Blueprint $table) {
            $table->id();
            $table->string('sigla', 10)->unique();
            $table->string('nombre', 100);
            $table->string('docente', 100);
            $table->string('horario', 50);
            $table->unsignedInteger('cupo');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('materias');
    }
};
```

#### database/migrations/…_create_inscripciones_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('inscripciones', function (Blueprint $table) {
            $table->id();
            $table->string('ru', 10);
            $table->string('estudiante', 120);
            $table->foreignId('materia_id')->constrained('materias')->cascadeOnDelete();
            $table->string('gestion', 10);
            $table->timestamps();
            $table->unique(['ru', 'materia_id', 'gestion']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('inscripciones');
    }
};
```

#### app/Models/Materia.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Materia extends Model
{
    protected $table = 'materias';
    protected $fillable = ['sigla', 'nombre', 'docente', 'horario', 'cupo'];

    protected function casts(): array
    {
        return ['cupo' => 'integer'];
    }

    public function inscripciones(): HasMany
    {
        return $this->hasMany(Inscripcion::class, 'materia_id');
    }
}
```

#### app/Models/Inscripcion.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Inscripcion extends Model
{
    protected $table = 'inscripciones';
    protected $fillable = ['ru', 'estudiante', 'materia_id', 'gestion'];

    public function materia(): BelongsTo
    {
        return $this->belongsTo(Materia::class, 'materia_id');
    }
}
```

> `protected $table = 'inscripciones'` es **obligatorio**: Laravel pluraliza en inglés y buscaría la tabla `inscripcions`.

#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Materia;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Materia::create(['sigla' => 'SIS258', 'nombre' => 'Sistemas Distribuidos', 'docente' => 'Ing. Docente', 'horario' => 'Lun-Mie 07:00', 'cupo' => 3]);
        Materia::create(['sigla' => 'SIS325', 'nombre' => 'Base de Datos II', 'docente' => fake()->name(), 'horario' => 'Mar-Jue 09:00', 'cupo' => 30]);
        Materia::create(['sigla' => 'COM350', 'nombre' => 'Arquitectura de Software', 'docente' => fake()->name(), 'horario' => 'Lun-Mie 11:00', 'cupo' => 25]);
        Materia::create(['sigla' => 'SIS427', 'nombre' => 'Tecnologias Emergentes', 'docente' => fake()->name(), 'horario' => 'Vie 14:00', 'cupo' => 20]);
        Materia::create(['sigla' => 'SIS421', 'nombre' => 'Inteligencia Artificial II', 'docente' => fake()->name(), 'horario' => 'Mar-Jue 16:00', 'cupo' => 20]);
        Materia::create(['sigla' => 'MAT207', 'nombre' => 'Investigacion Operativa', 'docente' => fake()->name(), 'horario' => 'Sab 08:00', 'cupo' => 40]);
    }
}
```

#### app/GraphQL/Esquema.php
```php
<?php

namespace App\GraphQL;

use App\Models\Inscripcion;
use App\Models\Materia;
use GraphQL\Error\UserError;
use GraphQL\Type\Definition\ObjectType;
use GraphQL\Type\Definition\Type;
use GraphQL\Type\Schema;
use Illuminate\Support\Facades\Http;

class Esquema
{
    const KARDEX = 'http://127.0.0.1:3000/api/estudiantes/';
    const GESTION = '2/2026';
    const MAXIMO_MATERIAS = 4;

    private static function estudiante($ru)
    {
        try {
            $r = Http::timeout(5)->acceptJson()->get(self::KARDEX . urlencode($ru));
        } catch (\Exception $e) {
            throw new UserError('El servicio de kardex (Node) no responde');
        }
        if ($r->status() == 404) {
            throw new UserError('No existe el estudiante con RU ' . $ru);
        }
        if (!$r->successful()) {
            throw new UserError('Error ' . $r->status() . ' al consultar el kardex');
        }
        return $r->json();
    }

    public static function crear(): Schema
    {
        $materiaType = new ObjectType([
            'name' => 'Materia',
            'fields' => function () use (&$inscripcionType) {
                return [
                    'id'          => Type::int(),
                    'sigla'       => Type::string(),
                    'nombre'      => Type::string(),
                    'docente'     => Type::string(),
                    'horario'     => Type::string(),
                    'cupo'        => Type::int(),
                    'inscritos'   => [
                        'type' => Type::int(),
                        'resolve' => fn ($m) => $m->inscripciones()->where('gestion', self::GESTION)->count(),
                    ],
                    'disponibles' => [
                        'type' => Type::int(),
                        'resolve' => fn ($m) => $m->cupo - $m->inscripciones()->where('gestion', self::GESTION)->count(),
                    ],
                    'estudiantes' => [
                        'type' => Type::listOf($inscripcionType),
                        'resolve' => fn ($m) => $m->inscripciones()->where('gestion', self::GESTION)->orderBy('estudiante')->get(),
                    ],
                ];
            },
        ]);

        $inscripcionType = new ObjectType([
            'name' => 'Inscripcion',
            'fields' => function () use ($materiaType) {
                return [
                    'id'         => Type::int(),
                    'ru'         => Type::string(),
                    'estudiante' => Type::string(),
                    'gestion'    => Type::string(),
                    'materia'    => ['type' => $materiaType, 'resolve' => fn ($i) => $i->materia],
                ];
            },
        ]);

        $query = new ObjectType([
            'name' => 'RootQueryType',
            'fields' => [
                'materias' => [
                    'type' => Type::listOf($materiaType),
                    'resolve' => fn () => Materia::orderBy('sigla')->get(),
                ],
                'materia' => [
                    'type' => $materiaType,
                    'args' => ['sigla' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        $m = Materia::where('sigla', strtoupper($args['sigla']))->first();
                        if (!$m) throw new UserError('No existe la materia ' . $args['sigla']);
                        return $m;
                    },
                ],
                'inscripcionesDe' => [
                    'type' => Type::listOf($inscripcionType),
                    'args' => ['ru' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        return Inscripcion::where('ru', $args['ru'])->where('gestion', self::GESTION)->get();
                    },
                ],
            ],
        ]);

        $mutation = new ObjectType([
            'name' => 'Mutation',
            'fields' => [
                'inscribir' => [
                    'type' => $inscripcionType,
                    'args' => [
                        'ru'    => Type::nonNull(Type::string()),
                        'sigla' => Type::nonNull(Type::string()),
                    ],
                    'resolve' => function ($root, $args) {
                        $materia = Materia::where('sigla', strtoupper($args['sigla']))->first();
                        if (!$materia) throw new UserError('No existe la materia ' . $args['sigla']);

                        $est = self::estudiante($args['ru']);
                        if (!$est['habilitado']) {
                            throw new UserError('El estudiante no esta habilitado para inscribirse');
                        }
                        if ($est['deuda'] > 0) {
                            throw new UserError('El estudiante tiene una deuda pendiente de Bs ' . $est['deuda']);
                        }

                        $ya = Inscripcion::where('ru', $args['ru'])->where('materia_id', $materia->id)
                            ->where('gestion', self::GESTION)->exists();
                        if ($ya) throw new UserError('El estudiante ya esta inscrito en ' . $materia->sigla);

                        $cantidad = Inscripcion::where('ru', $args['ru'])->where('gestion', self::GESTION)->count();
                        if ($cantidad >= self::MAXIMO_MATERIAS) {
                            throw new UserError('No puede inscribirse a mas de ' . self::MAXIMO_MATERIAS . ' materias');
                        }

                        $ocupados = $materia->inscripciones()->where('gestion', self::GESTION)->count();
                        if ($ocupados >= $materia->cupo) {
                            throw new UserError('La materia ' . $materia->sigla . ' ya no tiene cupo');
                        }

                        return Inscripcion::create([
                            'ru'         => $args['ru'],
                            'estudiante' => $est['nombre'],
                            'materia_id' => $materia->id,
                            'gestion'    => self::GESTION,
                        ]);
                    },
                ],
                'retirar' => [
                    'type' => Type::string(),
                    'args' => [
                        'ru'    => Type::nonNull(Type::string()),
                        'sigla' => Type::nonNull(Type::string()),
                    ],
                    'resolve' => function ($root, $args) {
                        $materia = Materia::where('sigla', strtoupper($args['sigla']))->first();
                        if (!$materia) throw new UserError('No existe la materia ' . $args['sigla']);
                        $i = Inscripcion::where('ru', $args['ru'])->where('materia_id', $materia->id)
                            ->where('gestion', self::GESTION)->first();
                        if (!$i) throw new UserError('El estudiante no esta inscrito en ' . $materia->sigla);
                        $i->delete();
                        return 'Se retiro a ' . $i->estudiante . ' de ' . $materia->sigla;
                    },
                ],
                'crearMateria' => [
                    'type' => $materiaType,
                    'args' => [
                        'sigla'   => Type::nonNull(Type::string()),
                        'nombre'  => Type::nonNull(Type::string()),
                        'docente' => Type::nonNull(Type::string()),
                        'horario' => Type::nonNull(Type::string()),
                        'cupo'    => Type::nonNull(Type::int()),
                    ],
                    'resolve' => function ($root, $args) {
                        $args['sigla'] = strtoupper($args['sigla']);
                        if ($args['cupo'] < 1) throw new UserError('El cupo debe ser mayor a 0');
                        if (Materia::where('sigla', $args['sigla'])->exists()) {
                            throw new UserError('Ya existe la materia ' . $args['sigla']);
                        }
                        return Materia::create($args);
                    },
                ],
            ],
        ]);

        return new Schema(['query' => $query, 'mutation' => $mutation]);
    }
}
```

> Los `'fields' => function () {...}` (en vez de un arreglo directo) sirven porque `Materia` usa `Inscripcion` y al revés: así se pueden referenciar entre ellos.

#### app/Http/Controllers/GraphQLController.php
```php
<?php

namespace App\Http\Controllers;

use App\GraphQL\Esquema;
use GraphQL\GraphQL;
use Illuminate\Http\Request;

class GraphQLController extends Controller
{
    public function __invoke(Request $request)
    {
        $resultado = GraphQL::executeQuery(
            Esquema::crear(),
            $request->input('query'),
            null,
            null,
            $request->input('variables')
        );
        return response()->json($resultado->toArray());
    }
}
```

#### routes/api.php
```php
<?php

use App\Http\Controllers\GraphQLController;
use Illuminate\Support\Facades\Route;

Route::post('/graphql', GraphQLController::class);
```

#### public/inscripciones.html
```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Inscripciones</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 900px; margin: 20px auto; }
    table { border-collapse: collapse; width: 100%; margin-top: 10px; }
    th, td { border: 1px solid #999; padding: 5px; text-align: left; }
    input { padding: 4px; }
    #mensaje { margin-top: 10px; font-weight: bold; }
  </style>
</head>
<body>
  <h2>Inscripcion de materias - gestion 2/2026</h2>

  RU: <input id="ru" value="111111">
  <button onclick="misMaterias()">Mis materias</button>
  <div id="mensaje"></div>

  <h3>Materias ofertadas</h3>
  <table>
    <thead><tr><th>Sigla</th><th>Materia</th><th>Docente</th><th>Horario</th><th>Cupo libre</th><th></th></tr></thead>
    <tbody id="materias"></tbody>
  </table>

  <h3>Mis inscripciones</h3>
  <table>
    <thead><tr><th>Sigla</th><th>Materia</th><th>Horario</th><th></th></tr></thead>
    <tbody id="mias"></tbody>
  </table>

  <script>
    const URL_GRAPHQL = '/api/graphql';

    async function graphql(query, variables) {
      const r = await fetch(URL_GRAPHQL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ query, variables })
      });
      const datos = await r.json();
      if (datos.errors) throw new Error(datos.errors[0].message);
      return datos.data;
    }

    function mostrar(texto, error) {
      const m = document.getElementById('mensaje');
      m.textContent = texto;
      m.style.color = error ? 'red' : 'green';
    }

    async function cargarMaterias() {
      const d = await graphql('{ materias { sigla nombre docente horario disponibles } }');
      document.getElementById('materias').innerHTML = d.materias.map(m =>
        `<tr><td>${m.sigla}</td><td>${m.nombre}</td><td>${m.docente}</td><td>${m.horario}</td>
         <td>${m.disponibles}</td><td><button onclick="inscribir('${m.sigla}')">Inscribir</button></td></tr>`
      ).join('');
    }

    async function misMaterias() {
      try {
        const d = await graphql('query($ru:String!){ inscripcionesDe(ru:$ru){ materia { sigla nombre horario } } }',
                                { ru: document.getElementById('ru').value });
        document.getElementById('mias').innerHTML = d.inscripcionesDe.map(i =>
          `<tr><td>${i.materia.sigla}</td><td>${i.materia.nombre}</td><td>${i.materia.horario}</td>
           <td><button onclick="retirar('${i.materia.sigla}')">Retirar</button></td></tr>`
        ).join('');
      } catch (e) { mostrar(e.message, true); }
    }

    async function inscribir(sigla) {
      try {
        const d = await graphql('mutation($ru:String!,$s:String!){ inscribir(ru:$ru, sigla:$s){ estudiante materia { sigla } } }',
                                { ru: document.getElementById('ru').value, s: sigla });
        mostrar(d.inscribir.estudiante + ' inscrito en ' + d.inscribir.materia.sigla, false);
      } catch (e) { mostrar(e.message, true); }
      cargarMaterias();
      misMaterias();
    }

    async function retirar(sigla) {
      try {
        const d = await graphql('mutation($ru:String!,$s:String!){ retirar(ru:$ru, sigla:$s) }',
                                { ru: document.getElementById('ru').value, s: sigla });
        mostrar(d.retirar, false);
      } catch (e) { mostrar(e.message, true); }
      cargarMaterias();
      misMaterias();
    }

    cargarMaterias().catch(e => mostrar('No responde el servidor GraphQL: ' + e.message, true));
  </script>
</body>
</html>
```

---

## Variantes que puede pedir el docente
| Si piden… | Cambia esto |
|---|---|
| El kardex con JWT | En Node, un middleware que lea `req.headers.authorization`; en Laravel `Http::withToken($token)->get(...)` (ver cómo se loguea Node en el [ejercicio 10](../10-clinica/README.md), `laravel.js`) |
| El kardex en Laravel y el GraphQL en Node | Es el [ejercicio 10](../10-clinica/README.md) con otros nombres |
| Que al inscribirse se **descuente** algo en el otro servicio | Después de crear, `Http::patch(self::KARDEX . $ru, ['deuda' => $est['deuda'] + 50])` (cobro de matrícula) |
| Choque de horarios | Antes de crear: buscar si alguna de sus inscripciones tiene el mismo `horario` → `UserError('Choque de horario con ...')` |
| Cliente C# | Mismo método `GraphQL()` del [ejercicio 10](../10-clinica/README.md) cambiando la URL a `http://localhost:8000/api/graphql` |
| Cliente web servido aparte (no en `public/`) | Laravel 12 ya responde CORS para `api/*`; si no, `php artisan config:publish cors` y poner `'allowed_origins' => ['*']` |
