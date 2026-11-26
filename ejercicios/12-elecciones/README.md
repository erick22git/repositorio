# Ejercicio 12 · Cómputo electoral: REST Laravel con JWT y **roles** (como `elecicion-api` del docente) + GraphQL Node de resultados + cliente web

[← Ejercicios](../README.md) · [Índice](../../README.md) · **[INGE + VARIANTE]** · probado (Laravel, Node y la página web)

Parte **exactamente** del proyecto `elecicion-api` del repositorio del docente (candidatos con `nombres, apellidos, cargo, ambito`, `LoginController` con `firebase/php-jwt`, `JwtMiddleware` que guarda el usuario en `$request->attributes`, y **403 "Acceso denegado"** si el `role` no es `admin`) y lo lleva a un enunciado completo de examen.

## Enunciado (estilo examen)
Sistema de cómputo de la elección universitaria:
1. **API REST** (Laravel + JWT, base `eleccion_api`):
   - Usuarios con `role`: **admin** (todo), **jurado** (registra actas), **user** (solo consulta).
   - Candidatos (CRUD, solo admin crea/edita/borra), mesas (número, recinto, habilitados) y **actas** por mesa con votos por candidato, blancos y nulos.
   - Reglas del acta: una sola acta por mesa (**409**), la suma de votos + blancos + nulos no puede pasar los **habilitados** (**422**), candidatos de otro cargo o repetidos (**422**), usuario sin permiso (**403**), sin token (**401**).
2. **Servidor GraphQL de resultados** (Node, **lee la misma base**): resultados por cargo con porcentajes, avance de mesas computadas y situación (**gana** con más del 50 % de válidos, o **segunda vuelta** entre los dos primeros, o empate); detalle por mesa con participación.
3. **Cliente web**: iniciar sesión, registrar acta y ver resultados actualizados.

```
                         ┌──REST + Bearer (login, mesas, candidatos, actas)──> eleccion-api :8000 ──┐
 Navegador eleccion.html ┤                                                                          ├──> MySQL eleccion_api
                         └──GraphQL (resultados)──> resultados-graphql-node :4000 ──(solo lee)──────┘
```

Usuarios (clave **password** para todos, la que pone la factory de Laravel):

| Email | Rol | Puede |
|---|---|---|
| test@example.com | admin | todo (igual que el repo del docente) |
| jurado@example.com | jurado | registrar actas |
| test2@example.com | user | solo consultar (igual que el repo del docente) |

## Orden de arranque
1. XAMPP → MySQL → crear la base **`eleccion_api`**.
2. `eleccion-api` (crea tablas y datos) → 3. `resultados-graphql-node` → 4. abrir `http://localhost:8000/eleccion.html`.

### Paso 1 · API REST en Laravel (comandos del README del docente)
```bat
C:\guia\00-preparacion\sin-internet\nuevo-laravel.bat eleccion-api
cd C:\examen\eleccion-api
```
`.env`: `DB_DATABASE=eleccion_api` (la base offline ya tiene `JWT_SECRET` y `JWT_ALGORITHM`; con internet: `composer require firebase/php-jwt` y agregarlos).
```bash
php artisan install:api
php artisan make:migration add_role_to_users_table --table=users
php artisan make:model Candidato -mfc --api
php artisan make:model Mesa -mc --api
php artisan make:model Acta -mc --api
php artisan make:model Voto
php artisan make:middleware JwtMiddleware
php artisan make:controller LoginController
```
- La migración de **actas** crea también la tabla **votos** (así no importa el orden de dos archivos). Borra la migración que creó `make:model Acta -m` o pega ahí el contenido.
- Orden de las migraciones (por fecha del nombre): role → candidatos → mesas → actas/votos.

```bash
php artisan migrate:fresh --seed
php artisan serve
```

> El seeder usa `User::factory()->create([... 'role' => 'admin'])` igual que el docente: la factory **no** necesita que `role` esté en `$fillable`. Si usas `User::create([...])`, agrega `'role'` al `$fillable` de `app/Models/User.php`.

### Paso 2 · Probar el REST (Postman)
Header `Accept: application/json` siempre. Para el token: *Authorization → Bearer Token*.

| # | Petición | Con | Esperado |
|---|---|---|---|
| 1 | POST `/api/login` `{"email":"test@example.com","password":"x"}` | — | 400 `Contraseña invalida` |
| 2 | GET `/api/candidatos` | sin token | 401 `No autorizado ...` |
| 3 | GET `/api/candidatos?cargo=Rector` | token de test2 | 200 (3 candidatos) |
| 4 | POST `/api/candidatos` | token de test2 | **403** `Acceso denegado` |
| 5 | POST `/api/candidatos` `{"nombres":"Ana","apellidos":"Mamani","cargo":"Director","ambito":"Carrera"}` | token admin | 201 |
| 6 | POST `/api/actas` | token de test2 | 403 `solo jurados registran actas` |
| 7 | POST `/api/actas` (cuerpo abajo) | token jurado | 201 |
| 8 | el mismo otra vez | token jurado | **409** `La mesa 1 ya tiene acta registrada` |
| 9 | acta de la mesa 2 con 150 + 100 votos | token jurado | **422** `La mesa tiene 200 habilitados y el acta suma 250 votos` |
| 10 | acta con `candidato_id: 4` (un decano) y `cargo: Rector` | token jurado | 422 `Hay candidatos que no son de Rector` |
| 11 | DELETE `/api/actas/1` | token jurado | 403 |
| 12 | GET `/api/mesas/1` | cualquier token | 200 con el acta y sus votos |

Cuerpo del acta (paso 7):
```json
{
  "mesa_id": 1, "cargo": "Rector", "blancos": 5, "nulos": 3,
  "votos": [
    { "candidato_id": 1, "cantidad": 80 },
    { "candidato_id": 2, "cantidad": 60 },
    { "candidato_id": 3, "cantidad": 30 }
  ]
}
```
Para el paso 13 del GraphQL registra también la mesa 2: `blancos 2, nulos 1`, votos `50, 90, 40`.

### Paso 3 · GraphQL de resultados en Node
```bat
C:\guia\00-preparacion\sin-internet\nuevo-node.bat resultados
cd C:\examen\resultados
```
Copia `database.js`, `schema.js`, `server.js` y `node server.js`. **No** hace `sync()`: las tablas son de Laravel y Node solo las lee (por eso `timestamps: false` y los nombres de tabla exactos).

En `http://localhost:4000/graphql`:
```graphql
{ resultados(cargo: "Rector") {
    mesasTotal mesasComputadas avance validos blancos nulos emitidos situacion
    candidatos { nombre votos porcentaje } } }

{ mesa(numero: 1) { recinto computada participacion blancos candidatos { nombre votos porcentaje } } }
```

Resultado de la corrida real (con las actas de las mesas 1 y 2):
```
resultados Rector  mesasComputadas 2 de 5, avance 40, validos 350, blancos 7, nulos 4, emitidos 361
                   Sonia Paredes Vaca 150 (42.86%), Mario Quiroga Leon 130 (37.14%), Hugo Cardozo Rios 70 (20%)
                   "Segunda vuelta entre Sonia Paredes Vaca y Mario Quiroga Leon"
resultados Decano  "Sin votos computados"
resultados Alcalde "No hay candidatos para el cargo Alcalde"
mesa 1             participacion 89, Mario 80 (47.06%) ...
mesa 5             computada "no"
mesa 50            "No existe la mesa 50"
```

### Paso 4 · Cliente web
`http://localhost:8000/eleccion.html` (en `public/` de Laravel; el GraphQL de Node tiene CORS abierto).
Corrida real en navegador: login del jurado → "Jurado Mesa (jurado)"; acta de la mesa 3 → "Acta registrada" y los resultados pasan a **3 de 5 (60%)**; la misma mesa otra vez → "Error 409: La mesa 3 ya tiene acta registrada"; con test2 → "Error 403: Acceso denegado: solo jurados registran actas".

---

## Archivos

### eleccion-api

#### database/migrations/…_add_role_to_users_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->string('role')->default('user');
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn('role');
        });
    }
};
```

#### database/migrations/…_create_candidatos_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('candidatos', function (Blueprint $table) {
            $table->id();
            $table->string('nombres');
            $table->string('apellidos');
            $table->string('cargo');
            $table->string('ambito');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('candidatos');
    }
};
```

#### database/migrations/…_create_mesas_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('mesas', function (Blueprint $table) {
            $table->id();
            $table->unsignedInteger('numero')->unique();
            $table->string('recinto');
            $table->unsignedInteger('habilitados');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('mesas');
    }
};
```

#### database/migrations/…_create_actas_table.php (actas y votos)
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('actas', function (Blueprint $table) {
            $table->id();
            $table->foreignId('mesa_id')->unique()->constrained('mesas');
            $table->string('cargo');
            $table->unsignedInteger('blancos')->default(0);
            $table->unsignedInteger('nulos')->default(0);
            $table->string('registrado_por');
            $table->timestamps();
        });

        Schema::create('votos', function (Blueprint $table) {
            $table->id();
            $table->foreignId('acta_id')->constrained('actas')->cascadeOnDelete();
            $table->foreignId('candidato_id')->constrained('candidatos');
            $table->unsignedInteger('cantidad');
            $table->timestamps();
            $table->unique(['acta_id', 'candidato_id']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('votos');
        Schema::dropIfExists('actas');
    }
};
```

#### app/Models/Candidato.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Candidato extends Model
{
    use HasFactory;

    protected $fillable = [
        'nombres',
        'apellidos',
        'cargo',
        'ambito'
    ];
}
```

#### app/Models/Mesa.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Mesa extends Model
{
    protected $fillable = ['numero', 'recinto', 'habilitados'];

    public function acta()
    {
        return $this->hasOne(Acta::class);
    }
}
```

#### app/Models/Acta.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Acta extends Model
{
    protected $fillable = ['mesa_id', 'cargo', 'blancos', 'nulos', 'registrado_por'];

    public function mesa()
    {
        return $this->belongsTo(Mesa::class);
    }

    public function votos()
    {
        return $this->hasMany(Voto::class);
    }
}
```

#### app/Models/Voto.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Voto extends Model
{
    protected $fillable = ['acta_id', 'candidato_id', 'cantidad'];

    public function candidato()
    {
        return $this->belongsTo(Candidato::class);
    }
}
```

#### database/factories/CandidatoFactory.php
```php
<?php

namespace Database\Factories;

use Illuminate\Database\Eloquent\Factories\Factory;

class CandidatoFactory extends Factory
{
    public function definition(): array
    {
        $ambitos = ['Rector' => 'Universidad', 'ViceRector' => 'Universidad', 'Decano' => 'Facultad', 'Director' => 'Carrera'];
        $cargo = $this->faker->randomElement(['Rector', 'ViceRector', 'Decano', 'Director']);
        $ambito = $ambitos[$cargo];
        return [
            'nombres' => $this->faker->firstName(),
            'apellidos' => $this->faker->lastName(),
            'cargo' => $cargo,
            'ambito' => $ambito,
        ];
    }
}
```

#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Candidato;
use App\Models\Mesa;
use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::factory()->create(['name' => 'Administrador', 'email' => 'test@example.com', 'role' => 'admin']);
        User::factory()->create(['name' => 'Jurado Mesa', 'email' => 'jurado@example.com', 'role' => 'jurado']);
        User::factory()->create(['name' => 'Test User 2', 'email' => 'test2@example.com', 'role' => 'user']);

        Candidato::create(['nombres' => 'Mario', 'apellidos' => 'Quiroga Leon', 'cargo' => 'Rector', 'ambito' => 'Universidad']);
        Candidato::create(['nombres' => 'Sonia', 'apellidos' => 'Paredes Vaca', 'cargo' => 'Rector', 'ambito' => 'Universidad']);
        Candidato::create(['nombres' => 'Hugo', 'apellidos' => 'Cardozo Rios', 'cargo' => 'Rector', 'ambito' => 'Universidad']);
        Candidato::factory()->count(6)->create(['cargo' => 'Decano', 'ambito' => 'Facultad']);

        for ($i = 1; $i <= 5; $i++) {
            Mesa::create(['numero' => $i, 'recinto' => $i <= 3 ? 'Facultad de Tecnologia' : 'Edificio Central', 'habilitados' => 200]);
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
        $usuario = User::where('email', $request->email)->first();
        if ($usuario == null) {
            return response()->json(['mensaje' => 'usuario invalido'], 400);
        }
        if (!Hash::check($request->password, $usuario->password)) {
            return response()->json(['mensaje' => 'Contraseña invalida'], 400);
        }

        $key = env('JWT_SECRET');
        $algoritmo = env('JWT_ALGORITHM');
        $time = time();
        $payload = [
            'iat' => $time,
            'exp' => $time + (60 * 60),
            'data' => [
                'user_id' => $usuario->id,
                'email' => $usuario->email,
                'name' => $usuario->name,
                'role' => $usuario->role
            ],
        ];
        $jwt = JWT::encode($payload, $key, $algoritmo);

        return response()->json([
            'mensaje' => 'Se logró autenticar al usuario',
            'token' => $jwt,
            'type' => 'bearer',
            'expires' => $time + (60 * 60),
            'usuario' => $usuario
        ], 200);
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
            $autorizacion = $request->header('Authorization');
            $jwt = substr($autorizacion, 7);
            $key = env('JWT_SECRET');
            $algoritmo = env('JWT_ALGORITHM');
            $datos = JWT::decode($jwt, new Key($key, $algoritmo));
            $request->attributes->add(['user' => $datos->data]);
        } catch (\Exception $e) {
            return response()->json(['status' => 'No autorizado ' . $e->getMessage()], 401);
        }
        return $next($request);
    }
}
```

#### app/Http/Controllers/CandidatoController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Candidato;
use Illuminate\Http\Request;

class CandidatoController extends Controller
{
    public function index(Request $request)
    {
        $consulta = Candidato::query();
        if ($request->filled('cargo')) {
            $consulta->where('cargo', $request->cargo);
        }
        return response()->json($consulta->get(), 200);
    }

    public function store(Request $request)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $datos = $request->validate([
            'nombres' => 'required|string',
            'apellidos' => 'required|string',
            'cargo' => 'required|in:Rector,ViceRector,Decano,Director',
            'ambito' => 'required|string',
        ]);
        $candidato = Candidato::create($datos);
        return response()->json($candidato, 201);
    }

    public function show(Candidato $candidato)
    {
        return response()->json($candidato, 200);
    }

    public function update(Request $request, Candidato $candidato)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $candidato->update($request->only(['nombres', 'apellidos', 'cargo', 'ambito']));
        return response()->json($candidato, 200);
    }

    public function destroy(Request $request, Candidato $candidato)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $candidato->delete();
        return response()->json($candidato, 204);
    }
}
```

#### app/Http/Controllers/MesaController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Mesa;
use Illuminate\Http\Request;

class MesaController extends Controller
{
    public function index()
    {
        $mesas = Mesa::with('acta')->orderBy('numero')->get();
        return response()->json($mesas, 200);
    }

    public function store(Request $request)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $datos = $request->validate([
            'numero' => 'required|integer|min:1|unique:mesas,numero',
            'recinto' => 'required|string',
            'habilitados' => 'required|integer|min:1',
        ]);
        return response()->json(Mesa::create($datos), 201);
    }

    public function show(Mesa $mesa)
    {
        return response()->json($mesa->load('acta.votos.candidato'), 200);
    }
}
```

#### app/Http/Controllers/ActaController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Acta;
use App\Models\Candidato;
use App\Models\Mesa;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class ActaController extends Controller
{
    public function index()
    {
        return response()->json(Acta::with('mesa', 'votos.candidato')->get(), 200);
    }

    public function store(Request $request)
    {
        $usuario = $request->attributes->get('user');
        if (!in_array($usuario->role, ['admin', 'jurado'])) {
            return response()->json(['message' => 'Acceso denegado: solo jurados registran actas'], 403);
        }

        $datos = $request->validate([
            'mesa_id' => 'required|integer|exists:mesas,id',
            'cargo' => 'required|in:Rector,ViceRector,Decano,Director',
            'blancos' => 'required|integer|min:0',
            'nulos' => 'required|integer|min:0',
            'votos' => 'required|array|min:1',
            'votos.*.candidato_id' => 'required|integer|exists:candidatos,id',
            'votos.*.cantidad' => 'required|integer|min:0',
        ]);

        $mesa = Mesa::find($datos['mesa_id']);
        if ($mesa->acta) {
            return response()->json(['message' => 'La mesa ' . $mesa->numero . ' ya tiene acta registrada'], 409);
        }

        $ids = array_column($datos['votos'], 'candidato_id');
        if (count($ids) !== count(array_unique($ids))) {
            return response()->json(['message' => 'Un candidato esta repetido en el acta'], 422);
        }
        $deOtroCargo = Candidato::whereIn('id', $ids)->where('cargo', '!=', $datos['cargo'])->count();
        if ($deOtroCargo > 0) {
            return response()->json(['message' => 'Hay candidatos que no son de ' . $datos['cargo']], 422);
        }

        $total = array_sum(array_column($datos['votos'], 'cantidad')) + $datos['blancos'] + $datos['nulos'];
        if ($total > $mesa->habilitados) {
            return response()->json([
                'message' => "La mesa tiene {$mesa->habilitados} habilitados y el acta suma {$total} votos"
            ], 422);
        }

        $acta = DB::transaction(function () use ($datos, $usuario) {
            $acta = Acta::create([
                'mesa_id' => $datos['mesa_id'],
                'cargo' => $datos['cargo'],
                'blancos' => $datos['blancos'],
                'nulos' => $datos['nulos'],
                'registrado_por' => $usuario->email,
            ]);
            foreach ($datos['votos'] as $v) {
                $acta->votos()->create($v);
            }
            return $acta;
        });

        return response()->json($acta->load('votos.candidato'), 201);
    }

    public function show(Acta $acta)
    {
        return response()->json($acta->load('mesa', 'votos.candidato'), 200);
    }

    public function destroy(Request $request, Acta $acta)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $acta->delete();
        return response()->json(['message' => 'Acta anulada'], 200);
    }
}
```

> `DB::transaction` guarda el acta y sus votos **juntos**: si falla un voto, no queda un acta a medias.

#### routes/api.php
```php
<?php

use App\Http\Controllers\ActaController;
use App\Http\Controllers\CandidatoController;
use App\Http\Controllers\LoginController;
use App\Http\Controllers\MesaController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);

Route::apiResource('candidatos', CandidatoController::class)->middleware(JwtMiddleware::class);
Route::apiResource('mesas', MesaController::class)->only(['index', 'store', 'show'])->middleware(JwtMiddleware::class);
Route::apiResource('actas', ActaController::class)->except(['update'])->middleware(JwtMiddleware::class);
```

#### public/eleccion.html
```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Computo electoral</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 950px; margin: 20px auto; }
    section { border: 1px solid #999; padding: 10px; margin-bottom: 15px; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #999; padding: 5px; text-align: left; }
    input, select { padding: 4px; }
    .barra { background: #555; height: 14px; }
    #mensaje { font-weight: bold; }
  </style>
</head>
<body>
  <h2>Computo electoral - eleccion de Rector</h2>
  <div id="mensaje"></div>

  <section>
    <h3>1. Iniciar sesion (API REST Laravel)</h3>
    Email: <input id="email" value="jurado@example.com">
    Password: <input id="password" type="password" value="password">
    <button onclick="login()">Entrar</button>
    <span id="usuario"></span>
  </section>

  <section>
    <h3>2. Registrar acta (solo jurado o admin)</h3>
    Mesa: <select id="mesa"></select>
    Blancos: <input id="blancos" type="number" value="0" min="0" style="width:70px">
    Nulos: <input id="nulos" type="number" value="0" min="0" style="width:70px">
    <table>
      <thead><tr><th>Candidato</th><th>Votos</th></tr></thead>
      <tbody id="candidatos"></tbody>
    </table>
    <button onclick="registrarActa()">Registrar acta</button>
  </section>

  <section>
    <h3>3. Resultados (GraphQL Node)</h3>
    <button onclick="resultados()">Actualizar</button>
    <p id="resumen"></p>
    <table>
      <thead><tr><th>Candidato</th><th>Votos</th><th>%</th><th></th></tr></thead>
      <tbody id="resultados"></tbody>
    </table>
  </section>

  <script>
    const API = '/api';
    const GRAPHQL = 'http://localhost:4000/graphql';
    let token = '';

    function mostrar(texto, error) {
      const m = document.getElementById('mensaje');
      m.textContent = texto;
      m.style.color = error ? 'red' : 'green';
    }

    async function rest(metodo, ruta, cuerpo) {
      const r = await fetch(API + ruta, {
        method: metodo,
        headers: { 'Content-Type': 'application/json', Accept: 'application/json', Authorization: 'Bearer ' + token },
        body: cuerpo ? JSON.stringify(cuerpo) : undefined
      });
      const datos = await r.json();
      if (!r.ok) throw new Error('Error ' + r.status + ': ' + (datos.message || datos.mensaje || datos.status));
      return datos;
    }

    async function login() {
      try {
        const d = await rest('POST', '/login', {
          email: document.getElementById('email').value,
          password: document.getElementById('password').value
        });
        token = d.token;
        document.getElementById('usuario').textContent = d.usuario.name + ' (' + d.usuario.role + ')';
        mostrar(d.mensaje, false);
        await cargarDatos();
      } catch (e) { mostrar(e.message, true); }
    }

    async function cargarDatos() {
      const mesas = await rest('GET', '/mesas');
      document.getElementById('mesa').innerHTML = mesas.map(m =>
        `<option value="${m.id}">Mesa ${m.numero} - ${m.recinto} (${m.habilitados} hab.)${m.acta ? ' - YA COMPUTADA' : ''}</option>`
      ).join('');
      const candidatos = await rest('GET', '/candidatos?cargo=Rector');
      document.getElementById('candidatos').innerHTML = candidatos.map(c =>
        `<tr><td>${c.nombres} ${c.apellidos}</td>
         <td><input type="number" min="0" value="0" data-id="${c.id}" class="voto" style="width:80px"></td></tr>`
      ).join('');
    }

    async function registrarActa() {
      if (!token) return mostrar('Primero inicie sesion', true);
      const votos = [...document.querySelectorAll('.voto')].map(i => ({
        candidato_id: Number(i.dataset.id),
        cantidad: Number(i.value)
      }));
      try {
        await rest('POST', '/actas', {
          mesa_id: Number(document.getElementById('mesa').value),
          cargo: 'Rector',
          blancos: Number(document.getElementById('blancos').value),
          nulos: Number(document.getElementById('nulos').value),
          votos
        });
        mostrar('Acta registrada', false);
        await cargarDatos();
        await resultados();
      } catch (e) { mostrar(e.message, true); }
    }

    async function resultados() {
      const query = `{ resultados(cargo: "Rector") {
        mesasTotal mesasComputadas avance validos blancos nulos situacion
        candidatos { nombre votos porcentaje } } }`;
      try {
        const r = await fetch(GRAPHQL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ query })
        });
        const d = await r.json();
        if (d.errors) throw new Error(d.errors[0].message);
        const res = d.data.resultados;
        document.getElementById('resumen').textContent =
          `Mesas computadas: ${res.mesasComputadas} de ${res.mesasTotal} (${res.avance}%) - ` +
          `validos ${res.validos}, blancos ${res.blancos}, nulos ${res.nulos}. ${res.situacion}`;
        document.getElementById('resultados').innerHTML = res.candidatos.map(c =>
          `<tr><td>${c.nombre}</td><td>${c.votos}</td><td>${c.porcentaje}</td>
           <td style="width:40%"><div class="barra" style="width:${c.porcentaje}%"></div></td></tr>`
        ).join('');
      } catch (e) { mostrar('Resultados: ' + e.message, true); }
    }

    resultados();
  </script>
</body>
</html>
```

### resultados-graphql-node

#### database.js
```js
const { Sequelize } = require('sequelize');

const sequelize = new Sequelize('eleccion_api', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const opciones = { timestamps: false };

const Candidato = sequelize.define('Candidato', {
  nombres:   Sequelize.STRING,
  apellidos: Sequelize.STRING,
  cargo:     Sequelize.STRING,
  ambito:    Sequelize.STRING
}, { ...opciones, tableName: 'candidatos' });

const Mesa = sequelize.define('Mesa', {
  numero:      Sequelize.INTEGER,
  recinto:     Sequelize.STRING,
  habilitados: Sequelize.INTEGER
}, { ...opciones, tableName: 'mesas' });

const Acta = sequelize.define('Acta', {
  mesa_id: Sequelize.INTEGER,
  cargo:   Sequelize.STRING,
  blancos: Sequelize.INTEGER,
  nulos:   Sequelize.INTEGER
}, { ...opciones, tableName: 'actas' });

const Voto = sequelize.define('Voto', {
  acta_id:      Sequelize.INTEGER,
  candidato_id: Sequelize.INTEGER,
  cantidad:     Sequelize.INTEGER
}, { ...opciones, tableName: 'votos' });

Acta.belongsTo(Mesa, { foreignKey: 'mesa_id' });
Mesa.hasOne(Acta, { foreignKey: 'mesa_id' });
Acta.hasMany(Voto, { foreignKey: 'acta_id' });
Voto.belongsTo(Candidato, { foreignKey: 'candidato_id' });

module.exports = { sequelize, Candidato, Mesa, Acta, Voto };
```

#### schema.js
```js
const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Candidato, Mesa, Acta, Voto } = require('./database');

const CandidatoResultadoType = new GraphQLObjectType({
  name: 'CandidatoResultado',
  fields: {
    id:         { type: GraphQLInt },
    nombre:     { type: GraphQLString },
    votos:      { type: GraphQLInt },
    porcentaje: { type: GraphQLFloat }
  }
});

const ResultadoType = new GraphQLObjectType({
  name: 'Resultado',
  fields: {
    cargo:           { type: GraphQLString },
    mesasTotal:      { type: GraphQLInt },
    mesasComputadas: { type: GraphQLInt },
    avance:          { type: GraphQLFloat },
    validos:         { type: GraphQLInt },
    blancos:         { type: GraphQLInt },
    nulos:           { type: GraphQLInt },
    emitidos:        { type: GraphQLInt },
    candidatos:      { type: new GraphQLList(CandidatoResultadoType) },
    situacion:       { type: GraphQLString }
  }
});

const MesaType = new GraphQLObjectType({
  name: 'Mesa',
  fields: {
    numero:      { type: GraphQLInt },
    recinto:     { type: GraphQLString },
    habilitados: { type: GraphQLInt },
    computada:   { type: GraphQLString },
    candidatos:  { type: new GraphQLList(CandidatoResultadoType) },
    blancos:     { type: GraphQLInt },
    nulos:       { type: GraphQLInt },
    participacion: { type: GraphQLFloat }
  }
});

function redondear(n) {
  return Math.round(n * 100) / 100;
}

async function calcular(cargo, actas) {
  const candidatos = await Candidato.findAll({ where: { cargo } });
  const conteo = {};
  candidatos.forEach(c => { conteo[c.id] = 0; });
  let blancos = 0, nulos = 0;
  for (const a of actas) {
    blancos += a.blancos;
    nulos += a.nulos;
    for (const v of a.Votos) conteo[v.candidato_id] = (conteo[v.candidato_id] || 0) + v.cantidad;
  }
  const validos = Object.values(conteo).reduce((s, x) => s + x, 0);
  const lista = candidatos.map(c => ({
    id: c.id,
    nombre: c.nombres + ' ' + c.apellidos,
    votos: conteo[c.id],
    porcentaje: validos === 0 ? 0 : redondear(conteo[c.id] * 100 / validos)
  })).sort((x, y) => y.votos - x.votos);
  return { lista, validos, blancos, nulos };
}

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    resultados: {
      type: ResultadoType,
      args: { cargo: { type: new GraphQLNonNull(GraphQLString) } },
      async resolve(parent, args) {
        const mesasTotal = await Mesa.count();
        const actas = await Acta.findAll({ where: { cargo: args.cargo }, include: [Voto] });
        const { lista, validos, blancos, nulos } = await calcular(args.cargo, actas);
        if (lista.length === 0) throw new Error('No hay candidatos para el cargo ' + args.cargo);

        let situacion;
        if (validos === 0) {
          situacion = 'Sin votos computados';
        } else if (lista[0].porcentaje > 50) {
          situacion = 'Gana ' + lista[0].nombre + ' con ' + lista[0].porcentaje + '%';
        } else if (lista.length > 1 && lista[0].votos === lista[1].votos) {
          situacion = 'Empate entre ' + lista[0].nombre + ' y ' + lista[1].nombre;
        } else {
          situacion = 'Segunda vuelta entre ' + lista[0].nombre + ' y ' + lista[1].nombre;
        }

        return {
          cargo: args.cargo,
          mesasTotal,
          mesasComputadas: actas.length,
          avance: mesasTotal === 0 ? 0 : redondear(actas.length * 100 / mesasTotal),
          validos, blancos, nulos,
          emitidos: validos + blancos + nulos,
          candidatos: lista,
          situacion
        };
      }
    },
    mesa: {
      type: MesaType,
      args: { numero: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const m = await Mesa.findOne({ where: { numero: args.numero } });
        if (!m) throw new Error('No existe la mesa ' + args.numero);
        const acta = await Acta.findOne({ where: { mesa_id: m.id }, include: [Voto] });
        if (!acta) {
          return { numero: m.numero, recinto: m.recinto, habilitados: m.habilitados, computada: 'no', candidatos: [], blancos: 0, nulos: 0, participacion: 0 };
        }
        const { lista, validos, blancos, nulos } = await calcular(acta.cargo, [acta]);
        return {
          numero: m.numero,
          recinto: m.recinto,
          habilitados: m.habilitados,
          computada: 'si',
          candidatos: lista,
          blancos, nulos,
          participacion: redondear((validos + blancos + nulos) * 100 / m.habilitados)
        };
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery });
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
  app.listen(PUERTO, () => console.log(`Resultados GraphQL en http://localhost:${PUERTO}/graphql`));
}

iniciar().catch(err => console.error('No pude conectar a la base de datos:', err.message));
```

---

## Variantes que puede pedir el docente
| Si piden… | Cambia esto |
|---|---|
| Un middleware de rol aparte (en vez de `if` en cada método) | `php artisan make:middleware RolMiddleware`; `handle($request, $next, ...$roles)` y `if (!in_array($request->attributes->get('user')->role, $roles)) return 403`; en rutas `->middleware([JwtMiddleware::class, RolMiddleware::class . ':admin,jurado'])` |
| Que el GraphQL **también** pida token | Node verifica el JWT con el **mismo** `JWT_SECRET` (`npm install jsonwebtoken` necesita internet; si no, que Node llame a `GET /api/mesas` con el token para validarlo) |
| Resultados en el REST en vez de GraphQL | `Voto::selectRaw('candidato_id, sum(cantidad) as total')->groupBy('candidato_id')->with('candidato')->get()` |
| Cliente C# | Login y actas como el [ejercicio 9](../9-transporte/README.md); resultados con el método `GraphQL()` del [ejercicio 10](../10-clinica/README.md) |
| SOAP que publique resultados | Servicio ASMX ([plantilla 05](../../plantillas/05-soap-csharp/README.md)) que llame al GraphQL con `WebClient.UploadString` |
| Anular acta y volver a registrar | `DELETE /api/actas/{id}` con admin (ya está) y registrar de nuevo |
