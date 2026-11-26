# Ejercicio 10 · Clínica: citas con GraphQL (Node) que consulta REST (Laravel + JWT) y cliente C#

[← Ejercicios](../README.md) · [Índice](../../README.md) · **[VARIANTE TIPO EXAMEN]** · probado (Laravel y Node) · C# sin compilar

Junta **todo lo que dijo el docente** en un solo ejercicio: REST en Laravel con JWT, GraphQL en Node, **un servicio llamando a otro** (Node → Laravel con token) y **C# consumiendo los dos** (REST y GraphQL).

## Enunciado (inventado, estilo examen)
Una clínica tiene dos servicios:
1. **API REST de pacientes** (Laravel, protegida con JWT): CRUD de pacientes por CI (`ci`, `nombres`, `apellidos`, `telefono`, `seguro`).
2. **Servidor GraphQL de citas** (Node): reservar, listar, cancelar y atender citas. Antes de reservar **consulta a la API REST** si el paciente existe (se loguea solo y manda el token).

Reglas de negocio de `reservarCita`:
- Especialidades válidas y su costo: Medicina General 50, Pediatría 80, Cardiología 150, Traumatología 120. **Si el paciente tiene seguro, el costo es 0.**
- Horas válidas: cada 30 min de 08:00 a 11:30 y de 14:00 a 17:30.
- No se puede reservar un horario ocupado (misma especialidad, fecha y hora).
- Un paciente no puede tener dos citas de la misma especialidad el mismo día.
- Si el paciente no está en la API REST → error. Si la API REST está apagada → error claro.

Cliente C# Windows Forms: iniciar sesión, buscar/registrar paciente (REST), ver horarios libres, reservar, listar y cancelar (GraphQL).

```
                ┌──REST + Bearer──> pacientes-laravel :8000 ──> MySQL bd_clinica (pacientes, users)
 C# Forms ──────┤                          ▲
                └──GraphQL──> citas-graphql-node :4000 ──> MySQL bd_clinica (citas)
                                    └──REST + Bearer (login automático)
```

## Orden de arranque
1. XAMPP → MySQL. phpMyAdmin → crear la base **`bd_clinica`** (las dos apps usan la misma base, tablas distintas).
2. `pacientes-laravel` → 3. `citas-graphql-node` → 4. cliente C#.

### Paso 1 · REST de pacientes en Laravel (con firebase/php-jwt)
```bat
C:\guia\00-preparacion\sin-internet\nuevo-laravel.bat pacientes
cd C:\examen\pacientes
```
La base offline ya trae `firebase/php-jwt` y en `.env` ya están `JWT_SECRET` y `JWT_ALGORITHM`. Solo cambia `DB_DATABASE=bd_clinica`.
```bash
php artisan install:api
php artisan make:model Paciente -m
php artisan make:controller PacienteController --api
php artisan make:controller LoginController
php artisan make:middleware JwtMiddleware
```
Pega los archivos y:
```bash
php artisan migrate:fresh --seed
php artisan serve
```
Usuario: **admin@test.com / 123456**. Pacientes fijos: `11021654` Juan Perez Segovia (con seguro) y `5551234` Ana Rojas Flores (sin seguro), más 5 con Faker.

> Detalle importante: en las rutas está `->parameters(['pacientes' => 'ci'])`, así `GET /api/pacientes/11021654` busca **por CI** y no por id.

Prueba rápida (Postman, header `Accept: application/json`):

| Petición | Esperado |
|---|---|
| GET `/api/pacientes` sin token | 401 |
| POST `/api/login` `{"email":"admin@test.com","password":"123456"}` | 200 con token |
| GET `/api/pacientes/11021654` (Bearer) | 200 Juan |
| GET `/api/pacientes/999` | 404 `Paciente no registrado` |
| POST `/api/pacientes` `{"ci":"5551234","nombres":"X","apellidos":"Y"}` | 422 (CI repetido) |
| POST `/api/pacientes` `{"ci":"8887766","nombres":"Rosa","apellidos":"Mamani Quispe","seguro":true}` | 201 |

### Paso 2 · GraphQL de citas en Node
```bat
C:\guia\00-preparacion\sin-internet\nuevo-node.bat citas
cd C:\examen\citas
```
Copia `database.js`, `laravel.js`, `schema.js`, `server.js`. (Con internet: `npm install express@4 express-graphql@0.12.0 graphql@15 mysql2 sequelize`.)
```bash
node server.js
```
Abre `http://localhost:4000/graphql` (GraphiQL) y prueba:

```graphql
{ paciente(ci: "11021654") { nombres apellidos seguro } }

{ horariosLibres(especialidad: "Cardiologia", fecha: "2026-12-01") }

mutation { reservarCita(ci: "11021654", especialidad: "Cardiologia", fecha: "2026-12-01", hora: "09:00") { id paciente costo estado } }

{ citas(fecha: "2026-12-01") { id paciente hora estado costo } }

mutation { cancelarCita(id: 1) { id estado } }
```

> `laravel.js` usa `fetch`, que viene en **Node 18 o más nuevo** (`node -v`). Si la PC tiene Node 16, usar `axios` o el módulo `http`.

Resultado de la corrida real (mismo código, SQLite):
```
paciente 11021654          {"nombres":"Juan","apellidos":"Perez Segovia","seguro":true}
paciente 999               "El paciente 999 no esta registrado"
reservar Juan 09:00        {"id":1,"paciente":"Juan Perez Segovia","costo":0,"estado":"reservada"}
reservar Ana 09:00         "Ese horario ya esta ocupado"
reservar Ana 09:30         {"id":2,"paciente":"Ana Rojas Flores","costo":150}
reservar Ana 10:00         "El paciente ya tiene una cita de Cardiologia ese dia"
reservar CI 999            "El paciente 999 no esta registrado"
especialidad Odontologia   "Especialidad no valida. Opciones: Medicina General, Pediatria, Cardiologia, Traumatologia"
fecha 01/12/2026           "La fecha debe tener formato AAAA-MM-DD"
hora 12:00                 "Hora no valida. Atencion cada 30 minutos de 08:00 a 11:30 y de 14:00 a 17:30"
horariosLibres             [... sin "09:00" ni "09:30" ...]
cancelarCita 1             {"id":1,"estado":"cancelada"}
cancelarCita 1 otra vez    "La cita ya fue cancelada"
Laravel apagado            "La API de pacientes (Laravel) no responde"
```

### Paso 3 · Cliente C#
- Rápido: abrir `cliente-forms/proyecto-vs/ClienteForms.sln`.
- A mano: Windows Forms (.NET Framework) → referencia **System.Web.Extensions** → reemplazar `Form1.cs`.

Flujo: **Iniciar sesión** → escribir CI → **Buscar paciente** (si da 404, llenar nombres/apellidos y **Registrar paciente**) → elegir especialidad y fecha → **Horarios libres** → elegir hora → **Reservar cita** → **Citas del CI** / **Cancelar cita**.

Cómo llama C# a GraphQL: un `POST` normal con el JSON `{ "query": "...", "variables": {...} }`. GraphQL devuelve **200 aunque haya error**, por eso el método `GraphQL()` revisa si vino `errors` y lanza la excepción con el `message`.

---

## Archivos

### pacientes-laravel

#### database/migrations/…_create_pacientes_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('pacientes', function (Blueprint $table) {
            $table->id();
            $table->string('ci', 20)->unique();
            $table->string('nombres', 80);
            $table->string('apellidos', 80);
            $table->string('telefono', 20)->nullable();
            $table->boolean('seguro')->default(false);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('pacientes');
    }
};
```

#### app/Models/Paciente.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Paciente extends Model
{
    protected $table = 'pacientes';
    protected $fillable = ['ci', 'nombres', 'apellidos', 'telefono', 'seguro'];

    protected function casts(): array
    {
        return ['seguro' => 'boolean'];
    }
}
```

#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Paciente;
use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::create(['name' => 'Recepcion', 'email' => 'admin@test.com', 'password' => '123456']);

        Paciente::create(['ci' => '11021654', 'nombres' => 'Juan', 'apellidos' => 'Perez Segovia', 'telefono' => '70011223', 'seguro' => true]);
        Paciente::create(['ci' => '5551234', 'nombres' => 'Ana', 'apellidos' => 'Rojas Flores', 'telefono' => '71122334', 'seguro' => false]);

        for ($i = 0; $i < 5; $i++) {
            Paciente::create([
                'ci'        => (string) fake()->unique()->numberBetween(1000000, 9999999),
                'nombres'   => fake()->firstName(),
                'apellidos' => fake()->lastName() . ' ' . fake()->lastName(),
                'telefono'  => '7' . fake()->numerify('#######'),
                'seguro'    => fake()->boolean(),
            ]);
        }
    }
}
```

#### app/Http/Controllers/LoginController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\User;
use Firebase\JWT\JWT;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class LoginController extends Controller
{
    public function login(Request $request)
    {
        $request->validate(['email' => 'required', 'password' => 'required']);

        $user = User::where('email', $request->email)->first();
        if (!$user || !Hash::check($request->password, $user->password)) {
            return response()->json(['mensaje' => 'Credenciales invalidas'], 401);
        }

        $payload = [
            'iat'  => time(),
            'exp'  => time() + 3600,
            'data' => ['id' => $user->id, 'email' => $user->email],
        ];

        return response()->json([
            'token' => JWT::encode($payload, env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256')),
            'type'  => 'bearer',
        ]);
    }
}
```

#### app/Http/Middleware/JwtMiddleware.php
```php
<?php

namespace App\Http\Middleware;

use Closure;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Illuminate\Http\Request;

class JwtMiddleware
{
    public function handle(Request $request, Closure $next)
    {
        try {
            $decoded = JWT::decode((string) $request->bearerToken(), new Key(env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256')));
        } catch (\Exception $e) {
            return response()->json(['mensaje' => 'Token invalido o ausente'], 401);
        }

        $request->attributes->add(['usuario' => $decoded->data]);
        return $next($request);
    }
}
```

#### app/Http/Controllers/PacienteController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Paciente;
use Illuminate\Http\Request;

class PacienteController extends Controller
{
    public function index()
    {
        return response()->json(Paciente::orderBy('apellidos')->get(), 200);
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'ci'        => 'required|string|max:20|unique:pacientes,ci',
            'nombres'   => 'required|string|max:80',
            'apellidos' => 'required|string|max:80',
            'telefono'  => 'nullable|string|max:20',
            'seguro'    => 'boolean',
        ]);
        return response()->json(Paciente::create($datos), 201);
    }

    public function show($ci)
    {
        $paciente = Paciente::where('ci', $ci)->first();
        if (!$paciente) {
            return response()->json(['mensaje' => 'Paciente no registrado'], 404);
        }
        return response()->json($paciente, 200);
    }

    public function update(Request $request, $ci)
    {
        $paciente = Paciente::where('ci', $ci)->first();
        if (!$paciente) {
            return response()->json(['mensaje' => 'Paciente no registrado'], 404);
        }
        $datos = $request->validate([
            'nombres'   => 'sometimes|string|max:80',
            'apellidos' => 'sometimes|string|max:80',
            'telefono'  => 'nullable|string|max:20',
            'seguro'    => 'sometimes|boolean',
        ]);
        $paciente->update($datos);
        return response()->json($paciente, 200);
    }

    public function destroy($ci)
    {
        $paciente = Paciente::where('ci', $ci)->first();
        if (!$paciente) {
            return response()->json(['mensaje' => 'Paciente no registrado'], 404);
        }
        $paciente->delete();
        return response()->json(['mensaje' => 'Paciente eliminado'], 200);
    }
}
```

#### routes/api.php
```php
<?php

use App\Http\Controllers\LoginController;
use App\Http\Controllers\PacienteController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);

Route::apiResource('pacientes', PacienteController::class)
    ->parameters(['pacientes' => 'ci'])
    ->middleware(JwtMiddleware::class);
```

### citas-graphql-node

#### package.json
```json
{
  "name": "citas-graphql",
  "version": "1.0.0",
  "main": "server.js",
  "scripts": {
    "serve": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.22.3",
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

#### database.js
```js
const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('bd_clinica', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Cita = sequelize.define('Cita', {
  ci_paciente:  { type: Sequelize.STRING(20), allowNull: false },
  paciente:     { type: Sequelize.STRING(170), allowNull: false },
  especialidad: { type: Sequelize.STRING(50), allowNull: false },
  fecha:        { type: Sequelize.DATEONLY, allowNull: false },
  hora:         { type: Sequelize.STRING(5), allowNull: false },
  costo:        { type: Sequelize.FLOAT, allowNull: false, defaultValue: 0 },
  estado:       { type: Sequelize.STRING(20), allowNull: false, defaultValue: 'reservada' }
}, {
  tableName: 'citas',
  timestamps: false
});

module.exports = { sequelize, Cita };
```

#### laravel.js (cliente REST con login automático)
```js
const API = 'http://127.0.0.1:8000/api';
const USUARIO = { email: 'admin@test.com', password: '123456' };

let token = '';

async function login() {
  const r = await fetch(API + '/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(USUARIO)
  });
  if (!r.ok) throw new Error('No pude iniciar sesion en la API de pacientes');
  token = (await r.json()).token;
}

async function pedir(url) {
  if (!token) await login();
  let r = await fetch(url, { headers: { Accept: 'application/json', Authorization: 'Bearer ' + token } });
  if (r.status === 401) {
    await login();
    r = await fetch(url, { headers: { Accept: 'application/json', Authorization: 'Bearer ' + token } });
  }
  return r;
}

async function buscarPaciente(ci) {
  let r;
  try {
    r = await pedir(API + '/pacientes/' + encodeURIComponent(ci));
  } catch (e) {
    throw new Error('La API de pacientes (Laravel) no responde');
  }
  if (r.status === 404) return null;
  if (!r.ok) throw new Error('Error ' + r.status + ' al consultar el paciente');
  return r.json();
}

module.exports = { buscarPaciente };
```

#### schema.js
```js
const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Cita } = require('./database');
const { buscarPaciente } = require('./laravel');

const ESPECIALIDADES = { 'Medicina General': 50, 'Pediatria': 80, 'Cardiologia': 150, 'Traumatologia': 120 };
const HORAS = ['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
               '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30'];

const PacienteType = new GraphQLObjectType({
  name: 'Paciente',
  fields: {
    ci:        { type: GraphQLString },
    nombres:   { type: GraphQLString },
    apellidos: { type: GraphQLString },
    telefono:  { type: GraphQLString },
    seguro:    { type: GraphQLBoolean }
  }
});

const CitaType = new GraphQLObjectType({
  name: 'Cita',
  fields: {
    id:           { type: GraphQLInt },
    ci_paciente:  { type: GraphQLString },
    paciente:     { type: GraphQLString },
    especialidad: { type: GraphQLString },
    fecha:        { type: GraphQLString },
    hora:         { type: GraphQLString },
    costo:        { type: GraphQLFloat },
    estado:       { type: GraphQLString }
  }
});

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    citas: {
      type: new GraphQLList(CitaType),
      args: { ci: { type: GraphQLString }, fecha: { type: GraphQLString }, estado: { type: GraphQLString } },
      resolve(parent, args) {
        const where = {};
        if (args.ci) where.ci_paciente = args.ci;
        if (args.fecha) where.fecha = args.fecha;
        if (args.estado) where.estado = args.estado;
        return Cita.findAll({ where, order: [['fecha', 'ASC'], ['hora', 'ASC']] });
      }
    },
    cita: {
      type: CitaType,
      args: { id: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const c = await Cita.findByPk(args.id);
        if (!c) throw new Error('No existe la cita ' + args.id);
        return c;
      }
    },
    paciente: {
      type: PacienteType,
      args: { ci: { type: new GraphQLNonNull(GraphQLString) } },
      async resolve(parent, args) {
        const p = await buscarPaciente(args.ci);
        if (!p) throw new Error('El paciente ' + args.ci + ' no esta registrado');
        return p;
      }
    },
    horariosLibres: {
      type: new GraphQLList(GraphQLString),
      args: {
        especialidad: { type: new GraphQLNonNull(GraphQLString) },
        fecha:        { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, args) {
        const ocupadas = await Cita.findAll({
          where: { especialidad: args.especialidad, fecha: args.fecha, estado: 'reservada' }
        });
        const tomadas = ocupadas.map(c => c.hora);
        return HORAS.filter(h => !tomadas.includes(h));
      }
    }
  }
});

const Mutation = new GraphQLObjectType({
  name: 'Mutation',
  fields: {
    reservarCita: {
      type: CitaType,
      args: {
        ci:           { type: new GraphQLNonNull(GraphQLString) },
        especialidad: { type: new GraphQLNonNull(GraphQLString) },
        fecha:        { type: new GraphQLNonNull(GraphQLString) },
        hora:         { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, args) {
        if (!(args.especialidad in ESPECIALIDADES)) {
          throw new Error('Especialidad no valida. Opciones: ' + Object.keys(ESPECIALIDADES).join(', '));
        }
        if (!/^\d{4}-\d{2}-\d{2}$/.test(args.fecha) || isNaN(Date.parse(args.fecha))) {
          throw new Error('La fecha debe tener formato AAAA-MM-DD');
        }
        if (!HORAS.includes(args.hora)) {
          throw new Error('Hora no valida. Atencion cada 30 minutos de 08:00 a 11:30 y de 14:00 a 17:30');
        }

        const paciente = await buscarPaciente(args.ci);
        if (!paciente) throw new Error('El paciente ' + args.ci + ' no esta registrado');

        const ocupado = await Cita.findOne({
          where: { especialidad: args.especialidad, fecha: args.fecha, hora: args.hora, estado: 'reservada' }
        });
        if (ocupado) throw new Error('Ese horario ya esta ocupado');

        const mismoDia = await Cita.findOne({
          where: { ci_paciente: args.ci, fecha: args.fecha, especialidad: args.especialidad, estado: 'reservada' }
        });
        if (mismoDia) throw new Error('El paciente ya tiene una cita de ' + args.especialidad + ' ese dia');

        return Cita.create({
          ci_paciente: args.ci,
          paciente: paciente.nombres + ' ' + paciente.apellidos,
          especialidad: args.especialidad,
          fecha: args.fecha,
          hora: args.hora,
          costo: paciente.seguro ? 0 : ESPECIALIDADES[args.especialidad]
        });
      }
    },
    cancelarCita: {
      type: CitaType,
      args: { id: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const c = await Cita.findByPk(args.id);
        if (!c) throw new Error('No existe la cita ' + args.id);
        if (c.estado !== 'reservada') throw new Error('La cita ya fue ' + c.estado);
        c.estado = 'cancelada';
        await c.save();
        return c;
      }
    },
    atenderCita: {
      type: CitaType,
      args: { id: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const c = await Cita.findByPk(args.id);
        if (!c) throw new Error('No existe la cita ' + args.id);
        if (c.estado !== 'reservada') throw new Error('La cita ya fue ' + c.estado);
        c.estado = 'atendida';
        await c.save();
        return c;
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery, mutation: Mutation });
```

#### server.js
```js
const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const schema = require('./schema');
const { sequelize } = require('./database');

const app = express();
const PUERTO = 4000;

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
  app.listen(PUERTO, () => console.log(`GraphQL de citas en http://localhost:${PUERTO}/graphql`));
}

iniciar().catch(err => console.error('No pude conectar a la base de datos:', err.message));
```

### cliente-forms/Form1.cs
```csharp
using System;
using System.Collections;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace WindowsFormsApp1
{
    public partial class Form1 : Form
    {
        const string REST = "http://localhost:8000/api";
        const string GRAPHQL = "http://localhost:4000/graphql";

        string token = "";
        TextBox txtCi, txtNombres, txtApellidos, txtFecha, txtIdCita, txtResultado;
        ComboBox cmbEspecialidad, cmbHora;
        CheckBox chkSeguro;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Clinica - reserva de citas";
            ClientSize = new Size(620, 520);

            txtCi = Caja("CI", 10);
            txtNombres = Caja("Nombres", 40);
            txtApellidos = Caja("Apellidos", 70);
            chkSeguro = new CheckBox { Text = "Tiene seguro", Location = new Point(380, 70), AutoSize = true };
            Controls.Add(chkSeguro);

            Controls.Add(new Label { Text = "Especialidad", Location = new Point(10, 113), AutoSize = true });
            cmbEspecialidad = new ComboBox { Location = new Point(110, 110), Width = 160, DropDownStyle = ComboBoxStyle.DropDownList };
            cmbEspecialidad.Items.AddRange(new object[] { "Medicina General", "Pediatria", "Cardiologia", "Traumatologia" });
            cmbEspecialidad.SelectedIndex = 0;
            Controls.Add(cmbEspecialidad);

            txtFecha = Caja("Fecha", 140);
            txtFecha.Text = DateTime.Today.AddDays(1).ToString("yyyy-MM-dd");

            Controls.Add(new Label { Text = "Hora", Location = new Point(10, 173), AutoSize = true });
            cmbHora = new ComboBox { Location = new Point(110, 170), Width = 160, DropDownStyle = ComboBoxStyle.DropDownList };
            Controls.Add(cmbHora);

            txtIdCita = Caja("Id cita", 200);

            Boton("Iniciar sesion", 10, 235, (s, e) => Ejecutar(IniciarSesion));
            Boton("Buscar paciente", 130, 235, (s, e) => Ejecutar(BuscarPaciente));
            Boton("Registrar paciente", 250, 235, (s, e) => Ejecutar(RegistrarPaciente));
            Boton("Horarios libres", 370, 235, (s, e) => Ejecutar(HorariosLibres));
            Boton("Reservar cita", 10, 265, (s, e) => Ejecutar(Reservar));
            Boton("Citas del CI", 130, 265, (s, e) => Ejecutar(CitasDelPaciente));
            Boton("Cancelar cita", 250, 265, (s, e) => Ejecutar(Cancelar));

            txtResultado = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical, ReadOnly = true,
                                         Location = new Point(10, 305), Size = new Size(600, 205) };
            Controls.Add(txtResultado);
        }

        TextBox Caja(string titulo, int y)
        {
            Controls.Add(new Label { Text = titulo, Location = new Point(10, y + 3), AutoSize = true });
            var t = new TextBox { Location = new Point(110, y), Width = 250 };
            Controls.Add(t);
            return t;
        }

        void Boton(string texto, int x, int y, EventHandler accion)
        {
            var b = new Button { Text = texto, Location = new Point(x, y), Width = 115 };
            b.Click += accion;
            Controls.Add(b);
        }

        void Ejecutar(Func<string> accion)
        {
            try { txtResultado.Text = accion(); }
            catch (WebException ex)
            {
                var r = ex.Response as HttpWebResponse;
                if (r == null) { txtResultado.Text = "El servidor no respondio: " + ex.Message; return; }
                string cuerpo = new StreamReader(r.GetResponseStream()).ReadToEnd();
                txtResultado.Text = "Error " + (int)r.StatusCode + Environment.NewLine + Mensaje(cuerpo);
            }
            catch (Exception ex) { txtResultado.Text = "Error: " + ex.Message; }
        }

        static string Mensaje(string json)
        {
            try
            {
                var d = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(json);
                if (d.ContainsKey("mensaje")) return d["mensaje"].ToString();
                if (d.ContainsKey("message")) return d["message"].ToString();
            }
            catch { }
            return json;
        }

        WebClient Web()
        {
            var w = new WebClient { Encoding = Encoding.UTF8 };
            w.Headers["Accept"] = "application/json";
            w.Headers["Content-Type"] = "application/json";
            if (token != "") w.Headers["Authorization"] = "Bearer " + token;
            return w;
        }

        static string Json(object o) { return new JavaScriptSerializer().Serialize(o); }

        static Dictionary<string, object> Leer(string json)
        {
            return new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(json);
        }

        Dictionary<string, object> GraphQL(string query, object variables)
        {
            string r = Web().UploadString(GRAPHQL, "POST", Json(new { query, variables }));
            var d = Leer(r);
            if (d.ContainsKey("errors"))
            {
                var primero = (Dictionary<string, object>)((ArrayList)d["errors"])[0];
                throw new Exception(primero["message"].ToString());
            }
            return (Dictionary<string, object>)d["data"];
        }

        string IniciarSesion()
        {
            var d = Leer(Web().UploadString(REST + "/login", "POST", Json(new { email = "admin@test.com", password = "123456" })));
            token = d["token"].ToString();
            return "Sesion iniciada en la API de pacientes";
        }

        string BuscarPaciente()
        {
            if (token == "") return "Primero inicie sesion";
            var p = Leer(Web().DownloadString(REST + "/pacientes/" + txtCi.Text.Trim()));
            txtNombres.Text = p["nombres"].ToString();
            txtApellidos.Text = p["apellidos"].ToString();
            chkSeguro.Checked = (bool)p["seguro"];
            return "Paciente: " + p["nombres"] + " " + p["apellidos"] + Environment.NewLine
                 + "Seguro: " + ((bool)p["seguro"] ? "si (la consulta es gratis)" : "no");
        }

        string RegistrarPaciente()
        {
            if (token == "") return "Primero inicie sesion";
            if (txtCi.Text.Trim() == "" || txtNombres.Text.Trim() == "" || txtApellidos.Text.Trim() == "")
                return "CI, nombres y apellidos son obligatorios";
            var cuerpo = new { ci = txtCi.Text.Trim(), nombres = txtNombres.Text.Trim(), apellidos = txtApellidos.Text.Trim(), seguro = chkSeguro.Checked };
            Web().UploadString(REST + "/pacientes", "POST", Json(cuerpo));
            return "Paciente registrado";
        }

        string HorariosLibres()
        {
            var data = GraphQL("query($e:String!,$f:String!){ horariosLibres(especialidad:$e, fecha:$f) }",
                               new { e = cmbEspecialidad.Text, f = txtFecha.Text });
            cmbHora.Items.Clear();
            foreach (var h in (ArrayList)data["horariosLibres"]) cmbHora.Items.Add(h);
            if (cmbHora.Items.Count > 0) cmbHora.SelectedIndex = 0;
            return cmbHora.Items.Count + " horarios libres para " + cmbEspecialidad.Text + " el " + txtFecha.Text;
        }

        string Reservar()
        {
            if (cmbHora.Text == "") return "Primero presione 'Horarios libres' y elija una hora";
            var data = GraphQL(
                "mutation($ci:String!,$e:String!,$f:String!,$h:String!){ reservarCita(ci:$ci, especialidad:$e, fecha:$f, hora:$h){ id paciente costo } }",
                new { ci = txtCi.Text.Trim(), e = cmbEspecialidad.Text, f = txtFecha.Text, h = cmbHora.Text });
            var c = (Dictionary<string, object>)data["reservarCita"];
            txtIdCita.Text = c["id"].ToString();
            return "Cita " + c["id"] + " reservada para " + c["paciente"] + Environment.NewLine
                 + cmbEspecialidad.Text + " - " + txtFecha.Text + " " + cmbHora.Text + Environment.NewLine
                 + "Costo: Bs " + c["costo"];
        }

        string CitasDelPaciente()
        {
            var data = GraphQL("query($ci:String){ citas(ci:$ci){ id especialidad fecha hora estado costo } }", new { ci = txtCi.Text.Trim() });
            var sb = new StringBuilder();
            foreach (Dictionary<string, object> c in (ArrayList)data["citas"])
            {
                sb.AppendLine(c["id"] + ") " + c["fecha"] + " " + c["hora"] + "  " + c["especialidad"] + "  " + c["estado"] + "  Bs " + c["costo"]);
            }
            return sb.Length == 0 ? "El paciente no tiene citas" : sb.ToString();
        }

        string Cancelar()
        {
            int id;
            if (!int.TryParse(txtIdCita.Text, out id)) return "Escriba el id de la cita";
            var data = GraphQL("mutation($id:Int!){ cancelarCita(id:$id){ id estado } }", new { id });
            var c = (Dictionary<string, object>)data["cancelarCita"];
            return "Cita " + c["id"] + " " + c["estado"];
        }
    }
}
```

---

## Variantes que puede pedir el docente
| Si piden… | Cambia esto |
|---|---|
| Al revés: GraphQL en Laravel y REST en Node | GraphQL con [plantilla 14](../../plantillas/14-graphql-laravel/README.md) y REST con [plantilla 13](../../plantillas/13-rest-node/README.md); el `Http::withToken($t)->get(...)` de Laravel reemplaza a `laravel.js` |
| Que GraphQL también pida token | En `server.js`: `graphqlHTTP(req => ({ schema, context: { token: req.headers.authorization } }))` y en el resolver revisar `context.token` |
| SOAP en vez de GraphQL para citas | Servicio ASMX ([plantilla 05](../../plantillas/05-soap-csharp/README.md)) o Java ([plantilla 15](../../plantillas/15-soap-java/README.md)) que llama al REST con `WebClient` / `HttpClient` |
| Cliente web en vez de C# | [plantilla 10](../../plantillas/10-cliente-web/README.md): `fetch(GRAPHQL, {method:'POST', body: JSON.stringify({query, variables})})` (por eso el CORS en `server.js`) |
| Reporte: total recaudado por especialidad | Query nueva que haga `Cita.findAll({ where: { estado: 'atendida' } })` y sume `costo` agrupando por `especialidad` |
