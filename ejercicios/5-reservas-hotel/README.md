# Ejercicio 5 · Reservas hoteleras (parcial anterior)

[← Ejercicios](../README.md) · [Índice](../../README.md)

| Versión | Me piden… |
|---|---|
| [1](#versión-1--reservar) | Reservar, consultar tarifa e historial |
| [2](#versión-2--además-cancelar) | Además cancelar |
| [3](#versión-3--comparar-habitaciones-disponibles) | Ver disponibles de todos los hoteles |

## Enunciado resumido
Hay dos hoteles que hablan distinto: **Hotel A** por **REST** y **Hotel B** por **GraphQL**. Un **Motor de reservas** (REST con **JWT**) atiende al cliente y habla con el hotel que corresponda. El hotel B además tiene un **SOAP** de tarifas e historial. Hay cliente **web** y de **escritorio**.

```
 Web / Forms ──REST + JWT──> Motor :8000 ──REST────> Hotel A (Laravel :8001)
                                        └──GraphQL─> Hotel B (Node :4000)
 Forms ──SOAP──> Tarifas e historial
```

## Proyectos

| Carpeta | Tecnología | Puerto | Base |
|---|---|---|---|
| `hotel-a-rest` | Laravel | 8001 | `bd_hotel_a` |
| `hotel-b-graphql` | Node | 4000 | `bd_hotel_b` |
| `motor-reservas` | Laravel + JWT | 8000 | `bd_motor` |
| `tarifa-soap` | C# ASMX | lo da Visual Studio | — |
| `cliente-web` | HTML | — | — |
| `cliente-forms` | C# Forms | — | — |

**Crea las tres bases** en phpMyAdmin antes de empezar.

## Orden de arranque
Hotel A → Hotel B → Motor → SOAP → clientes.

## Datos de prueba
| Hotel | Habitación | Tipo | Tarifa |
|---|---|---|---|
| A | A101 | Simple | 150 |
| A | A201 | Doble | 250 |
| B | B101 | Simple | 180 |
| B | B201 | Suite | 400 |

Usuario: `admin@test.com` / `123456`.

---

## Versión 1 · Reservar

### Paso 1 · Hotel A
`.env`: `DB_DATABASE=bd_hotel_a`

**1.1** Crear:
```bash
php artisan install:api
php artisan make:model Habitacion -msfc --api
```
**1.2** Pega los 5 archivos de abajo.

**1.3** Recién entonces:
```bash
php artisan migrate:fresh --seed
php artisan serve --port=8001
```

#### Migración (database/migrations/…_create_habitaciones_table.php)
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('habitaciones', function (Blueprint $table) {
            $table->id();
            $table->string('codHabitacion', 10)->unique();
            $table->string('tipo', 30);
            $table->integer('capacidad');
            $table->decimal('tarifa', 10, 2);
            $table->boolean('disponible')->default(true);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('habitaciones');
    }
};
```
> artisan crea la migración con un nombre de tabla "adivinado" (puede salir `habitacions`). Al pegar este contenido queda `habitaciones`.

#### app/Models/Habitacion.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Habitacion extends Model
{
    use HasFactory;

    protected $table = 'habitaciones';   // sin esto Laravel buscaria "habitacions"
    protected $fillable = ['codHabitacion', 'tipo', 'capacidad', 'tarifa', 'disponible'];
    protected $casts = ['disponible' => 'boolean', 'tarifa' => 'float'];

    public function getRouteKeyName(): string
    {
        return 'codHabitacion';
    }
}
```
#### app/Http/Controllers/HabitacionController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Habitacion;
use Illuminate\Http\Request;

class HabitacionController extends Controller
{
    public function index()
    {
        return response()->json(Habitacion::all());
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'codHabitacion' => 'required|string',
            'tipo'          => 'required|string',
            'capacidad'     => 'required|integer|min:1',
            'tarifa'        => 'required|numeric|gt:0',
        ]);

        if (Habitacion::where('codHabitacion', $datos['codHabitacion'])->exists()) {
            return response()->json(['error' => 'La habitacion ya existe'], 409);
        }

        return response()->json(Habitacion::create($datos)->refresh(), 201);
    }

    public function show(Habitacion $habitacion)
    {
        return response()->json($habitacion);
    }

    // PATCH /api/habitaciones/{cod}   { "disponible": false }
    public function update(Request $request, Habitacion $habitacion)
    {
        $datos = $request->validate([
            'tipo'       => 'sometimes|string',
            'capacidad'  => 'sometimes|integer|min:1',
            'tarifa'     => 'sometimes|numeric|gt:0',
            'disponible' => 'sometimes|boolean',
        ]);

        $habitacion->update($datos);
        return response()->json($habitacion);
    }

    public function destroy(Habitacion $habitacion)
    {
        $habitacion->delete();
        return response()->noContent();
    }
}
```
#### routes/api.php
```php
<?php

use App\Http\Controllers\HabitacionController;
use Illuminate\Support\Facades\Route;

// parameters() es obligatorio: Laravel creeria que el singular es "habitacione"
Route::apiResource('habitaciones', HabitacionController::class)
    ->parameters(['habitaciones' => 'habitacion']);
```
#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Habitacion;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Habitacion::create(['codHabitacion' => 'A101', 'tipo' => 'Simple', 'capacidad' => 1, 'tarifa' => 150]);
        Habitacion::create(['codHabitacion' => 'A201', 'tipo' => 'Doble', 'capacidad' => 2, 'tarifa' => 250]);
    }
}
```

### Paso 2 · Hotel B (GraphQL)
`npm run dev` (crea la tabla y los datos solo).
#### database.js
```javascript
const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('bd_hotel_b', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Habitacion = sequelize.define('Habitacion', {
  codHabitacion: { type: Sequelize.STRING(10), allowNull: false, unique: true },
  tipo:          { type: Sequelize.STRING(30), allowNull: false },
  capacidad:     { type: Sequelize.INTEGER, allowNull: false },
  tarifa:        { type: Sequelize.FLOAT, allowNull: false },
  disponible:    { type: Sequelize.BOOLEAN, allowNull: false, defaultValue: true }
}, {
  tableName: 'habitaciones',
  timestamps: false
});

module.exports = { sequelize, Habitacion };
```
#### schema.js
```javascript
const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Habitacion } = require('./database');

const HabitacionType = new GraphQLObjectType({
  name: 'Habitacion',
  fields: {
    codHabitacion: { type: GraphQLString },
    tipo:          { type: GraphQLString },
    capacidad:     { type: GraphQLInt },
    tarifa:        { type: GraphQLFloat },
    disponible:    { type: GraphQLBoolean }
  }
});

async function buscar(codHabitacion) {
  const h = await Habitacion.findOne({ where: { codHabitacion } });
  if (!h) throw new Error('No existe la habitacion ' + codHabitacion);
  return h;
}

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    habitaciones: {
      type: new GraphQLList(HabitacionType),
      resolve: () => Habitacion.findAll()
    },
    habitacion: {
      type: HabitacionType,
      args: { codHabitacion: { type: new GraphQLNonNull(GraphQLString) } },
      resolve: (parent, args) => buscar(args.codHabitacion)
    }
  }
});

const Mutation = new GraphQLObjectType({
  name: 'Mutation',
  fields: {
    actualizarDisponibilidad: {
      type: HabitacionType,
      args: {
        codHabitacion: { type: new GraphQLNonNull(GraphQLString) },
        disponible:    { type: new GraphQLNonNull(GraphQLBoolean) }
      },
      async resolve(parent, args) {
        const h = await buscar(args.codHabitacion);
        return h.update({ disponible: args.disponible });
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery, mutation: Mutation });
```
#### server.js
```javascript
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
```
#### package.json
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

### Paso 3 · Motor de reservas
`.env`: `DB_DATABASE=bd_motor`, `JWT_SECRET=...` y `JWT_ALGORITHM=HS256`.

**3.1** Crear:
```bash
php artisan install:api
composer require firebase/php-jwt
php artisan make:model Reserva -msfc --api
php artisan make:controller LoginController
php artisan make:middleware JwtMiddleware
```
(`composer require` necesita internet; si copiaste tu base preparada **ya está**: sáltalo.)

**3.2** Pega los 7 archivos de abajo.

**3.3** Recién entonces:
```bash
php artisan migrate:fresh --seed
php artisan serve
```

#### Migración (database/migrations/…_create_reservas_table.php)
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('reservas', function (Blueprint $table) {
            $table->id();
            $table->date('fecha');
            $table->string('idHuesped', 20);
            $table->string('codHabitacion', 10);
            $table->string('hotelDestino', 1);          // A o B
            $table->integer('noches');
            $table->decimal('total', 10, 2);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('reservas');
    }
};
```
#### app/Models/Reserva.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Reserva extends Model
{
    use HasFactory;

    protected $table = 'reservas';
    protected $fillable = ['fecha', 'idHuesped', 'codHabitacion', 'hotelDestino', 'noches', 'total'];
}
```
#### app/Http/Controllers/ReservaController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Reserva;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Http;

// INTERMEDIARIO: un hotel habla REST y el otro GraphQL. El cliente no se entera.
class ReservaController extends Controller
{
    private const HOTEL_A = 'http://localhost:8001/api/habitaciones';   // REST
    private const HOTEL_B = 'http://localhost:4000/graphql';            // GraphQL

    // GET /api/reserva
    public function index()
    {
        return response()->json(Reserva::all());
    }

    // POST /api/reserva
    public function store(Request $request)
    {
        $datos = $request->validate([
            'fecha'         => 'required|date',
            'idHuesped'     => 'required|string',
            'codHabitacion' => 'required|alpha_num',
            'hotelDestino'  => 'required|in:A,B',
            'noches'        => 'required|integer|min:1',
        ]);

        try {
            // 1) Preguntar al hotel
            $habitacion = $this->consultar($datos['hotelDestino'], $datos['codHabitacion']);
            if (!$habitacion) {
                return response()->json(['confirmada' => false, 'error' => 'La habitacion no existe'], 404);
            }
            // 2) Verificar disponibilidad y tarifa
            if (!$habitacion['disponible']) {
                return response()->json(['confirmada' => false, 'error' => 'No esta disponible'], 409);
            }
            // 3) Marcarla como ocupada en el hotel
            if (!$this->ocupar($datos['hotelDestino'], $datos['codHabitacion'])) {
                return response()->json(['confirmada' => false, 'error' => 'El hotel no acepto el cambio'], 502);
            }
        } catch (ConnectionException $e) {
            return response()->json(['confirmada' => false, 'error' => 'El hotel no responde'], 503);
        }

        // 4) Guardar la reserva y devolver el resultado
        $datos['total'] = $habitacion['tarifa'] * $datos['noches'];
        $reserva = Reserva::create($datos);

        return response()->json(['confirmada' => true, 'tarifa' => $habitacion['tarifa'], 'reserva' => $reserva], 201);
    }

    private function consultar(string $hotel, string $cod): ?array
    {
        if ($hotel === 'A') {
            $r = Http::acceptJson()->timeout(5)->get(self::HOTEL_A . '/' . $cod);
            return $r->successful() ? $r->json() : null;
        }

        $r = Http::timeout(5)->post(self::HOTEL_B, [
            'query'     => 'query($c: String!) { habitacion(codHabitacion: $c) { tarifa disponible } }',
            'variables' => ['c' => $cod],
        ]);
        return $r->json('data.habitacion');      // null si no existe
    }

    private function ocupar(string $hotel, string $cod): bool
    {
        if ($hotel === 'A') {
            return Http::acceptJson()->timeout(5)
                ->patch(self::HOTEL_A . '/' . $cod, ['disponible' => false])
                ->successful();
        }

        $r = Http::timeout(5)->post(self::HOTEL_B, [
            'query'     => 'mutation($c: String!) { actualizarDisponibilidad(codHabitacion: $c, disponible: false) { disponible } }',
            'variables' => ['c' => $cod],
        ]);
        // GraphQL responde 200 aunque falle: hay que mirar "errors"
        return $r->successful() && $r->json('errors') === null;
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
    // POST /api/login   { "email": "...", "password": "..." }
    public function login(Request $request)
    {
        $request->validate(['email' => 'required', 'password' => 'required']);

        $user = User::where('email', $request->email)->first();
        if (!$user || !Hash::check($request->password, $user->password)) {
            return response()->json(['error' => 'Credenciales invalidas'], 401);
        }

        $payload = [
            'iat'  => time(),            // cuando se creo
            'exp'  => time() + 3600,     // vence en 1 hora
            'data' => ['id' => $user->id, 'email' => $user->email],
        ];

        $token = JWT::encode($payload, env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256'));

        return response()->json(['token' => $token, 'type' => 'bearer']);
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
    // Portero: sin token valido no se pasa
    public function handle(Request $request, Closure $next)
    {
        $jwt = str_replace('Bearer ', '', $request->header('Authorization', ''));

        try {
            $decoded = JWT::decode($jwt, new Key(env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256')));
        } catch (\Exception $e) {
            return response()->json(['error' => 'Token invalido o ausente'], 401);
        }

        $request->attributes->add(['usuario' => $decoded->data]);
        return $next($request);
    }
}
```
#### routes/api.php
```php
<?php

use App\Http\Controllers\LoginController;
use App\Http\Controllers\ReservaController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);      // libre

Route::middleware(JwtMiddleware::class)->group(function () {   // con token
    Route::get('/reserva', [ReservaController::class, 'index']);
    Route::post('/reserva', [ReservaController::class, 'store']);
});
```
#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::create(['name' => 'Admin', 'email' => 'admin@test.com', 'password' => '123456']);
    }
}
```

### Paso 4 · SOAP de tarifas
[Crear como la plantilla 05](../../plantillas/05-soap-csharp/README.md).
#### Servicio.asmx
```aspx
<%@ WebService Language="C#" CodeBehind="~/App_Code/Servicio.cs" Class="Servicio" %>
```
#### App_Code/Servicio.cs
```csharp
using System.Collections.Generic;
using System.Web.Services;
using System.Web.Services.Protocols;

// Hotel Mirador Andino: tarifas e historial
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class Servicio : System.Web.Services.WebService
{
    private static readonly Dictionary<string, double> tarifas = new Dictionary<string, double>
    {
        { "B101", 180 }, { "B201", 400 }
    };

    private static readonly Dictionary<string, List<Movimiento>> movimientos = new Dictionary<string, List<Movimiento>>
    {
        { "B101", new List<Movimiento> { new Movimiento { fecha = "2026-10-01", noches = 2 },
                                         new Movimiento { fecha = "2026-10-15", noches = 1 } } }
    };

    [WebMethod]
    public double consultarTarifa(string codHabitacion)
    {
        double tarifa;
        if (!tarifas.TryGetValue(codHabitacion ?? "", out tarifa))
        {
            throw new SoapException("No existe la habitacion", SoapException.ClientFaultCode);
        }
        return tarifa;
    }

    // Un historial son varios movimientos, por eso devuelve un arreglo
    [WebMethod]
    public Movimiento[] historial(string codHabitacion)
    {
        List<Movimiento> lista;
        return movimientos.TryGetValue(codHabitacion ?? "", out lista) ? lista.ToArray() : new Movimiento[0];
    }
}

public class Movimiento
{
    public string fecha { get; set; }
    public int noches { get; set; }
}
```

### Paso 5 · Cliente web
```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Reservas</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 600px; margin: 20px auto; }
    input, select, button { padding: 6px; margin: 4px 0; }
    pre { background: #f2f2f2; padding: 10px; white-space: pre-wrap; }
  </style>
</head>
<body>
  <h2>Motor de reservas</h2>

  <input id="email" value="admin@test.com">
  <input id="password" type="password" value="123456">
  <button id="entrar" onclick="entrar()">Entrar</button>

  <h3>Reservar</h3>
  <input id="fecha" type="date">
  <input id="idHuesped" placeholder="id huesped" value="H1">
  <input id="codHabitacion" placeholder="codigo habitacion" value="A101">
  <select id="hotelDestino">
    <option value="A">A - Gran Sucre (REST)</option>
    <option value="B">B - Mirador Andino (GraphQL)</option>
  </select>
  <input id="noches" type="number" min="1" value="2">
  <button id="reservar" onclick="reservar()">Reservar</button>

  <pre id="salida">Primero inicia sesion</pre>

  <script>
    const API = 'http://localhost:8000/api';
    let token = '';
    const valor = id => document.getElementById(id).value.trim();
    const mostrar = texto => document.getElementById('salida').textContent = texto;
    document.getElementById('fecha').value = new Date().toISOString().slice(0, 10);

    async function enviar(ruta, cuerpo) {
      const headers = { Accept: 'application/json', 'Content-Type': 'application/json' };
      if (token) headers.Authorization = 'Bearer ' + token;
      const r = await fetch(API + ruta, { method: 'POST', headers, body: JSON.stringify(cuerpo) });
      return { codigo: r.status, datos: await r.json() };
    }

    async function entrar() {
      try {
        const r = await enviar('/login', { email: valor('email'), password: valor('password') });
        token = r.datos.token || '';
        mostrar(token ? 'Sesion iniciada' : 'Error ' + r.codigo + ': ' + r.datos.error);
      } catch (e) { mostrar('El motor no responde: ' + e.message); }
    }

    async function reservar() {
      try {
        const r = await enviar('/reserva', {
          fecha: valor('fecha'), idHuesped: valor('idHuesped'), codHabitacion: valor('codHabitacion'),
          hotelDestino: valor('hotelDestino'), noches: Number(valor('noches'))
        });
        mostrar(r.datos.confirmada
          ? 'RESERVA CONFIRMADA. Total: ' + r.datos.reserva.total + ' Bs'
          : 'NO CONFIRMADA (' + r.codigo + '): ' + (r.datos.error || r.datos.message));
      } catch (e) { mostrar('El motor no responde: ' + e.message); }
    }
  </script>
</body>
</html>
```

Pulsa **Entrar**, luego **Reservar** `A101` / hotel A / 2 noches → confirmada, total 300. Repite → "No esta disponible". Cambia a `B101` / hotel B → total 360.

### Paso 6 · Cliente de escritorio
[Proyecto Forms](../../plantillas/11-cliente-forms-csharp/README.md) + referencia `System.Web.Extensions` + referencia de servicio **`srHotel`**.
```csharp
// Cliente 2:  REST -> registrar reservas        SOAP -> tarifa e historial
// ANTES DE PEGAR:
//   - Agregar referencia de servicio al SOAP del hotel con el nombre  srHotel
//   - Agregar referencia  System.Web.Extensions
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace WindowsFormsApp1   // <-- dejar el nombre que tenga TU proyecto
{
    public partial class Form1 : Form
    {
        const string API = "http://localhost:8000/api";

        readonly Dictionary<string, TextBox> cajas = new Dictionary<string, TextBox>();
        TextBox salida;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Recepcion";
            ClientSize = new Size(470, 440);

            string[] campos = { "fecha", "idHuesped", "codHabitacion", "hotelDestino", "noches" };
            string[] valores = { DateTime.Today.ToString("yyyy-MM-dd"), "H1", "B101", "B", "2" };
            for (int i = 0; i < campos.Length; i++)
            {
                Controls.Add(new Label { Text = campos[i], Location = new Point(10, 13 + i * 28), AutoSize = true });
                cajas[campos[i]] = new TextBox { Text = valores[i], Location = new Point(120, 10 + i * 28), Width = 220 };
                Controls.Add(cajas[campos[i]]);
            }

            Boton("Reservar", 10, () => Reservar());
            Boton("Tarifa", 130, () => Tarifa());
            Boton("Historial", 250, () => Historial());

            salida = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical,
                                   Location = new Point(10, 200), Size = new Size(450, 230) };
            Controls.Add(salida);
        }

        void Boton(string texto, int x, Func<string> accion)
        {
            var b = new Button { Text = texto, Location = new Point(x, 160), Width = 110 };
            b.Click += (s, e) =>
            {
                try { salida.Text = accion(); }
                catch (WebException ex)
                {
                    var r = ex.Response as HttpWebResponse;
                    salida.Text = r == null ? "El servidor no responde: " + ex.Message
                        : "Error " + (int)r.StatusCode + "\r\n" + new StreamReader(r.GetResponseStream()).ReadToEnd();
                }
                catch (Exception ex) { salida.Text = "Error: " + ex.Message; }
            };
            Controls.Add(b);
        }

        // ---------- REST ----------
        string Enviar(string ruta, object cuerpo, string token)
        {
            var web = new WebClient { Encoding = Encoding.UTF8 };
            web.Headers["Accept"] = "application/json";
            web.Headers["Content-Type"] = "application/json";
            if (token != null) web.Headers["Authorization"] = "Bearer " + token;
            return web.UploadString(API + ruta, "POST", new JavaScriptSerializer().Serialize(cuerpo));
        }

        string Reservar()
        {
            string login = Enviar("/login", new { email = "admin@test.com", password = "123456" }, null);
            var datos = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(login);

            var reserva = new Dictionary<string, string>();
            foreach (var par in cajas) reserva[par.Key] = par.Value.Text.Trim();

            return Enviar("/reserva", reserva, datos["token"].ToString());
        }

        // ---------- SOAP ----------
        string Tarifa()
        {
            var hotel = new srHotel.ServicioSoapClient();
            return "Tarifa: " + hotel.consultarTarifa(cajas["codHabitacion"].Text.Trim()) + " Bs";
        }

        string Historial()
        {
            var hotel = new srHotel.ServicioSoapClient();
            var texto = new StringBuilder("Historial:\r\n");
            foreach (var m in hotel.historial(cajas["codHabitacion"].Text.Trim()))
            {
                texto.AppendLine(m.fecha + " - " + m.noches + " noche(s)");
            }
            return texto.ToString();
        }
    }
}
```

Botón *Tarifa* con `B101` → 180. *Historial* → dos movimientos.

### Qué hace
El motor valida, pregunta al hotel si está libre, la marca ocupada y recién guarda la reserva con el total.

### Cómo defenderlo
- "El motor es un **intermediario/adaptador**: el cliente usa un solo formato aunque un hotel sea REST y otro GraphQL (**heterogeneidad**)."
- "Primero marco la habitación y después guardo; si el hotel no responde devuelvo **503** y no guardo nada."
- "**GraphQL responde 200 aunque falle**, por eso reviso `errors`."
- "Puse `timeout(5)` para no quedarme colgado."
- "Códigos: 201 creada, 404 no existe, 409 ocupada, 422 datos malos, 503 hotel caído."
- "Limitación honesta: dos reservas **exactamente simultáneas** podrían ver libre la misma habitación; se resolvería con un bloqueo o una actualización condicional en el hotel."

---

## Versión 2 · Además cancelar
```php
<?php
// 1) En routes/api.php, DENTRO del grupo con JwtMiddleware:
Route::delete('/reserva/{reserva}', [ReservaController::class, 'destroy']);

// 2) En ReservaController agregar:
public function destroy(Reserva $reserva)
{
    // Liberar la habitacion en el hotel y recien borrar la reserva
    $cod = $reserva->codHabitacion;
    try {
        if ($reserva->hotelDestino === 'A') {
            $ok = Http::acceptJson()->timeout(5)
                ->patch(self::HOTEL_A . '/' . $cod, ['disponible' => true])->successful();
        } else {
            $r = Http::timeout(5)->post(self::HOTEL_B, [
                'query'     => 'mutation($c: String!) { actualizarDisponibilidad(codHabitacion: $c, disponible: true) { disponible } }',
                'variables' => ['c' => $cod],
            ]);
            $ok = $r->successful() && $r->json('errors') === null;
        }
    } catch (ConnectionException $e) {
        return response()->json(['error' => 'El hotel no responde'], 503);
    }

    if (!$ok) {
        return response()->json(['error' => 'El hotel no acepto el cambio'], 502);
    }

    $reserva->delete();
    return response()->noContent();
}
```

Probar:
```bat
curl -X DELETE http://localhost:8000/api/reserva/1 -H "Accept: application/json" -H "Authorization: Bearer TOKEN"
```
Responde **204** y la habitación vuelve a estar disponible.

**Cómo defenderlo:** "Es la operación inversa: libero en el hotel y **solo si salió bien** borro la reserva."

---

## Versión 3 · Comparar habitaciones disponibles
```php
<?php
// 1) En routes/api.php (con o sin token, segun pidan):
Route::get('/disponibles', [ReservaController::class, 'disponibles']);

// 2) En ReservaController agregar: junta los dos hoteles y ordena por precio
public function disponibles()
{
    $a = Http::acceptJson()->timeout(5)->get(self::HOTEL_A)->json() ?? [];
    $b = Http::timeout(5)->post(self::HOTEL_B, [
        'query' => '{ habitaciones { codHabitacion tipo capacidad tarifa disponible } }',
    ])->json('data.habitaciones') ?? [];

    $todas = collect($a)->map(fn ($h) => $h + ['hotel' => 'A'])
        ->concat(collect($b)->map(fn ($h) => $h + ['hotel' => 'B']))
        ->where('disponible', true)
        ->sortBy('tarifa')
        ->values();

    return response()->json($todas);
}
```

`GET http://localhost:8000/api/disponibles`

**Cómo defenderlo:** "Junto dos fuentes distintas en una sola lista con el mismo formato, filtro las libres y ordeno por tarifa."

> Para reiniciar todo: `migrate:fresh --seed` en Hotel A y Motor; en Hotel B borra la tabla `habitaciones` en phpMyAdmin y reinicia Node.
