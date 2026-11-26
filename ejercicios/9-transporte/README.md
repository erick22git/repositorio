# Ejercicio 9 · Terminal de buses: salidas y asientos (2º parcial del docente)

[← Ejercicios](../README.md) · [Índice](../../README.md) · **[2º PARCIAL]** · probado (Laravel) · C# sin compilar

Es el ejercicio `transporte` que el docente dejó en su repo `SistemasDistribuidos2-26` (controladores vacíos y un `ApiController` con `creartoken`). Aquí está **resuelto completo**, con los mismos nombres de campos que usa él y con el **JWT hecho a mano** (base64url + `hash_hmac`) en vez de una librería, igual que en su resolución.

## Enunciado resumido
API REST para una terminal de buses:
- **Salidas**: destino, fecha, hora, bus, precio, capacidad. Cualquiera puede verlas; crear/editar/borrar pide token.
- **Asientos** (venta de pasajes): salida_id, numero_asiento, ci_pasajero. Todo pide token.
- Reglas: no vender un asiento con número mayor a la capacidad (**422**), no vender dos veces el mismo asiento (**409**), no vender si la salida está llena (**422**), la salida debe existir (**422** de validación).
- Login `POST /api/login` que devuelve el token; sin token o con token falso → **401**.
- Cliente C# Windows Forms que inicia sesión, lista salidas y vende asientos.

```
 Postman / C# Forms ──REST + Bearer──> transporte-laravel :8000 ──> MySQL bd_transporte
```

## Paso a paso

### 1. Proyecto Laravel
```bat
C:\guia\00-preparacion\sin-internet\nuevo-laravel.bat transporte
cd C:\examen\transporte
```
(Con internet: `composer create-project laravel/laravel transporte` y `php artisan install:api`.)

En `.env`: `DB_DATABASE=bd_transporte` y crear esa base en phpMyAdmin.

```bash
php artisan install:api
php artisan make:model Salida -mf
php artisan make:model Asiento -m
php artisan make:controller SalidaController
php artisan make:controller AsientoController --api
php artisan make:controller ApiController
php artisan make:middleware TokenMiddleware
```
Pega cada archivo de abajo (el contenido de las migraciones va **dentro** de las que creó artisan; respeta el orden: primero `salidas`, después `asientos`, porque asientos tiene la llave foránea).

```bash
php artisan migrate:fresh --seed
php artisan serve
```

El seeder crea el usuario **admin@test.com / 123456**, 5 salidas con Faker y deja vendido el asiento 1 de la salida 1 (para probar el 409 sin preparar nada).

### 2. Probar en Postman (en este orden)
Headers en todas: `Accept: application/json`. En las que llevan cuerpo: `Content-Type: application/json`.

| # | Método y URL | Cuerpo | Esperado |
|---|---|---|---|
| 1 | POST `/api/asientos` sin token | `{}` | 401 `Token invalido o ausente` |
| 2 | POST `/api/login` | `{"email":"admin@test.com","password":"x"}` | 401 `Credenciales invalidas` |
| 3 | POST `/api/login` | `{"email":"admin@test.com","password":"123456"}` | 200 `{token, type}` → copiar el token en *Authorization → Bearer Token* |
| 4 | POST `/api/salidas` | `{"destino":"Sucre","fecha":"2026-12-20","hora":"08:00","bus":"1234-ABC","precio":80,"capacidad":2}` | 201 (id 6) |
| 5 | POST `/api/asientos` | `{"salida_id":6,"numero_asiento":1,"ci_pasajero":"111"}` | 201 |
| 6 | lo mismo otra vez | | 409 `El asiento ya esta vendido` |
| 7 | POST `/api/asientos` | `{"salida_id":6,"numero_asiento":5,"ci_pasajero":"222"}` | 422 `El bus solo tiene 2 asientos` |
| 8 | POST `/api/asientos` | `{"salida_id":99,...}` | 422 (validación `exists`) |
| 9 | GET `/api/salidas/6` | | 200 con `disponibles` y la lista de `asientos` |
| 10 | GET `/api/salidas/77` | | 404 |
| 11 | DELETE `/api/salidas/6` | | 200 `Salida eliminada` (borra primero sus asientos) |
| 12 | GET `/api/asientos` con `Bearer abc.def.ghi` | | 401 |

Resultado de la corrida real (SQLite, mismo código):
```
--sin token            {"mensaje":"Token invalido o ausente"} 401
--login malo           {"mensaje":"Credenciales invalidas"} 401
--crear salida         {...,"capacidad":2,"id":6} 201
--vender 1             {"salida_id":6,"numero_asiento":1,"ci_pasajero":"111",...} 201
--repetir              {"mensaje":"El asiento ya esta vendido"} 409
--asiento 5            {"mensaje":"El bus solo tiene 2 asientos"} 422
--salida inexistente   {"message":"The selected salida id is invalid.",...} 422
--seed (salida 1, nro 1) {"mensaje":"El asiento ya esta vendido"} 409
--show 6               {...,"disponibles":1,"asientos":[{...}]} 200
--404                  {"mensaje":"Salida no encontrada"} 404
--delete               {"mensaje":"Salida eliminada"} 200
--token falso          {"mensaje":"Token invalido o ausente"} 401
```

### 3. Cliente C# (Windows Forms .NET Framework)
- Opción rápida: abrir `cliente-forms/proyecto-vs/ClienteForms.sln` (ya tiene la referencia `System.Web.Extensions`).
- Opción a mano: proyecto nuevo *Aplicación de Windows Forms (.NET Framework)* → *Agregar referencia → Ensamblados → System.Web.Extensions* → reemplazar `Form1.cs` por el de abajo (los controles se crean por código, no hay que arrastrar nada).

Flujo: **Iniciar sesión** → **Ver salidas** → escribir id de salida, número de asiento y CI → **Vender asiento**. Si el servidor responde 409/422, la caja muestra el código y el `mensaje` que mandó Laravel.

---

## Archivos

#### database/migrations/…_create_salidas_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('salidas', function (Blueprint $table) {
            $table->id();
            $table->string('destino', 100);
            $table->date('fecha');
            $table->time('hora');
            $table->string('bus', 20);
            $table->decimal('precio', 8, 2);
            $table->unsignedInteger('capacidad');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('salidas');
    }
};
```

#### database/migrations/…_create_asientos_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('asientos', function (Blueprint $table) {
            $table->id();
            $table->foreignId('salida_id')->constrained('salidas')->cascadeOnDelete();
            $table->unsignedInteger('numero_asiento');
            $table->string('ci_pasajero', 20);
            $table->timestamps();
            $table->unique(['salida_id', 'numero_asiento']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('asientos');
    }
};
```

#### app/Models/Salida.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Salida extends Model
{
    use HasFactory;

    protected $table = 'salidas';
    protected $fillable = ['destino', 'fecha', 'hora', 'bus', 'precio', 'capacidad'];

    protected function casts(): array
    {
        return ['fecha' => 'date:Y-m-d', 'precio' => 'decimal:2', 'capacidad' => 'integer'];
    }

    public function asientos(): HasMany
    {
        return $this->hasMany(Asiento::class, 'salida_id');
    }
}
```

#### app/Models/Asiento.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Asiento extends Model
{
    use HasFactory;

    protected $table = 'asientos';
    protected $fillable = ['salida_id', 'numero_asiento', 'ci_pasajero'];

    protected function casts(): array
    {
        return ['salida_id' => 'integer', 'numero_asiento' => 'integer'];
    }

    public function salida(): BelongsTo
    {
        return $this->belongsTo(Salida::class, 'salida_id');
    }
}
```

#### database/factories/SalidaFactory.php
```php
<?php

namespace Database\Factories;

use Illuminate\Database\Eloquent\Factories\Factory;

class SalidaFactory extends Factory
{
    public function definition(): array
    {
        return [
            'destino'   => fake()->randomElement(['La Paz', 'Cochabamba', 'Santa Cruz', 'Potosí', 'Oruro', 'Tarija']),
            'fecha'     => fake()->dateTimeBetween('now', '+30 days')->format('Y-m-d'),
            'hora'      => fake()->randomElement(['07:00', '10:30', '14:00', '19:00', '21:30']),
            'bus'       => strtoupper(fake()->bothify('####-???')),
            'precio'    => fake()->randomElement([50, 70, 90, 120, 150]),
            'capacidad' => fake()->randomElement([40, 44, 48]),
        ];
    }
}
```

#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Asiento;
use App\Models\Salida;
use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::create(['name' => 'Boletero', 'email' => 'admin@test.com', 'password' => '123456']);

        Salida::factory()->count(5)->create();
        Asiento::create(['salida_id' => 1, 'numero_asiento' => 1, 'ci_pasajero' => '11021654']);
    }
}
```

#### app/Http/Controllers/ApiController.php (login + JWT a mano)
```php
<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;


class ApiController extends Controller
{
    private static $Secreto = 'clave_terminal_2026';

    private static function base64url($texto)
    {
        return rtrim(strtr(base64_encode($texto), '+/', '-_'), '=');
    }

    private static function creartoken($email)
    {
        $h = self::base64url(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
        $p = self::base64url(json_encode(['email' => $email, 'iat' => time(), 'exp' => time() + 3600]));
        $f = self::base64url(hash_hmac('sha256', "$h.$p", self::$Secreto, true));

        return "$h.$p.$f";
    }


    public static function verificartoken($token)
    {
        $partes = explode('.', (string) $token);
        if (count($partes) !== 3) return null;
        [$h, $p, $f] = $partes;
        $esperada = self::base64url(hash_hmac('sha256', "$h.$p", self::$Secreto, true));
        if (!hash_equals($esperada, $f)) return null;
        $payload = json_decode(base64_decode(strtr($p, '-_', '+/')), true);
        if (!$payload || ($payload['exp'] ?? 0) < time()) return null;
        return $payload;
    }


    public function login(Request $request)
    {
        $request->validate(['email' => 'required|email', 'password' => 'required']);
        $usuario = User::where('email', $request->email)->first();
        if (!$usuario || !Hash::check($request->password, $usuario->password)) {
            return response()->json(['mensaje' => 'Credenciales invalidas'], 401);
        }
        return response()->json(['token' => self::creartoken($usuario->email), 'type' => 'bearer'], 200);
    }
}
```

#### app/Http/Middleware/TokenMiddleware.php
```php
<?php

namespace App\Http\Middleware;

use App\Http\Controllers\ApiController;
use Closure;
use Illuminate\Http\Request;

class TokenMiddleware
{
    public function handle(Request $request, Closure $next)
    {
        $payload = ApiController::verificartoken($request->bearerToken());
        if ($payload === null) {
            return response()->json(['mensaje' => 'Token invalido o ausente'], 401);
        }
        $request->attributes->add(['usuario' => $payload['email']]);
        return $next($request);
    }
}
```

> No hace falta registrarlo en `bootstrap/app.php`: en las rutas se usa la clase directamente (`TokenMiddleware::class`).

#### app/Http/Controllers/SalidaController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Salida;
use Illuminate\Http\Request;

class SalidaController extends Controller
{
    public function index(Request $request)
    {
        $consulta = Salida::withCount('asientos')->orderBy('fecha')->orderBy('hora');
        if ($request->filled('destino')) {
            $consulta->where('destino', $request->destino);
        }
        return response()->json($consulta->get(), 200);
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'destino'   => 'required|string|max:100',
            'fecha'     => 'required|date',
            'hora'      => 'required|date_format:H:i',
            'bus'       => 'required|string|max:20',
            'precio'    => 'required|numeric|min:0',
            'capacidad' => 'required|integer|min:1',
        ]);
        return response()->json(Salida::create($datos), 201);
    }

    public function show($id)
    {
        $salida = Salida::with('asientos')->find($id);
        if (!$salida) {
            return response()->json(['mensaje' => 'Salida no encontrada'], 404);
        }
        $salida->disponibles = $salida->capacidad - $salida->asientos->count();
        return response()->json($salida, 200);
    }

    public function update(Request $request, $id)
    {
        $salida = Salida::find($id);
        if (!$salida) {
            return response()->json(['mensaje' => 'Salida no encontrada'], 404);
        }
        $datos = $request->validate([
            'destino'   => 'sometimes|string|max:100',
            'fecha'     => 'sometimes|date',
            'hora'      => 'sometimes|date_format:H:i',
            'bus'       => 'sometimes|string|max:20',
            'precio'    => 'sometimes|numeric|min:0',
            'capacidad' => 'sometimes|integer|min:1',
        ]);
        if (isset($datos['capacidad']) && $datos['capacidad'] < $salida->asientos()->count()) {
            return response()->json(['mensaje' => 'Ya hay mas asientos vendidos que esa capacidad'], 422);
        }
        $salida->update($datos);
        return response()->json($salida, 200);
    }

    public function destroy($id)
    {
        $salida = Salida::find($id);
        if (!$salida) {
            return response()->json(['mensaje' => 'Salida no encontrada'], 404);
        }
        $salida->asientos()->delete();
        $salida->delete();
        return response()->json(['mensaje' => 'Salida eliminada'], 200);
    }
}
```

#### app/Http/Controllers/AsientoController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Asiento;
use App\Models\Salida;
use Illuminate\Http\Request;

class AsientoController extends Controller
{
    public function index(Request $request)
    {
        $consulta = Asiento::query()->orderBy('numero_asiento');
        if ($request->filled('salida_id')) {
            $consulta->where('salida_id', $request->salida_id);
        }
        return response()->json($consulta->get(), 200);
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'salida_id'      => 'required|integer|exists:salidas,id',
            'numero_asiento' => 'required|integer|min:1',
            'ci_pasajero'    => 'required|string|max:20',
        ]);

        $salida = Salida::find($datos['salida_id']);
        if ($datos['numero_asiento'] > $salida->capacidad) {
            return response()->json(['mensaje' => "El bus solo tiene {$salida->capacidad} asientos"], 422);
        }
        if ($salida->asientos()->count() >= $salida->capacidad) {
            return response()->json(['mensaje' => 'La salida ya esta llena'], 422);
        }
        $ocupado = Asiento::where('salida_id', $salida->id)->where('numero_asiento', $datos['numero_asiento'])->exists();
        if ($ocupado) {
            return response()->json(['mensaje' => 'El asiento ya esta vendido'], 409);
        }

        return response()->json(Asiento::create($datos), 201);
    }

    public function show($id)
    {
        $asiento = Asiento::with('salida')->find($id);
        if (!$asiento) {
            return response()->json(['mensaje' => 'Asiento no encontrado'], 404);
        }
        return response()->json($asiento, 200);
    }

    public function update(Request $request, $id)
    {
        $asiento = Asiento::find($id);
        if (!$asiento) {
            return response()->json(['mensaje' => 'Asiento no encontrado'], 404);
        }
        $datos = $request->validate(['ci_pasajero' => 'required|string|max:20']);
        $asiento->update($datos);
        return response()->json($asiento, 200);
    }

    public function destroy($id)
    {
        $asiento = Asiento::find($id);
        if (!$asiento) {
            return response()->json(['mensaje' => 'Asiento no encontrado'], 404);
        }
        $asiento->delete();
        return response()->json(['mensaje' => 'Venta anulada'], 200);
    }
}
```

#### routes/api.php
```php
<?php

use App\Http\Controllers\ApiController;
use App\Http\Controllers\AsientoController;
use App\Http\Controllers\SalidaController;
use App\Http\Middleware\TokenMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [ApiController::class, 'login']);

Route::get('/salidas', [SalidaController::class, 'index']);
Route::get('/salidas/{id}', [SalidaController::class, 'show']);

Route::middleware(TokenMiddleware::class)->group(function () {
    Route::post('/salidas', [SalidaController::class, 'store']);
    Route::put('/salidas/{id}', [SalidaController::class, 'update']);
    Route::delete('/salidas/{id}', [SalidaController::class, 'destroy']);
    Route::apiResource('asientos', AsientoController::class);
});
```

#### cliente-forms/Form1.cs
```csharp
using System;
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
        const string API = "http://localhost:8000/api";

        string token = "";
        TextBox txtEmail, txtPassword, txtSalida, txtAsiento, txtCi, txtResultado;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Terminal de buses - venta de pasajes";
            ClientSize = new Size(560, 470);

            txtEmail = Caja("Email", 10, "admin@test.com");
            txtPassword = Caja("Password", 40, "123456");
            txtPassword.PasswordChar = '*';
            txtSalida = Caja("Salida (id)", 80, "1");
            txtAsiento = Caja("Nro asiento", 110, "");
            txtCi = Caja("CI pasajero", 140, "");

            Boton("Iniciar sesion", 10, 175, (s, e) => Ejecutar(IniciarSesion));
            Boton("Ver salidas", 140, 175, (s, e) => Ejecutar(VerSalidas));
            Boton("Ver salida", 270, 175, (s, e) => Ejecutar(VerSalida));
            Boton("Vender asiento", 400, 175, (s, e) => Ejecutar(Vender));

            txtResultado = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical, ReadOnly = true,
                                         Location = new Point(10, 215), Size = new Size(540, 245) };
            Controls.Add(txtResultado);
        }

        TextBox Caja(string titulo, int y, string valor)
        {
            Controls.Add(new Label { Text = titulo, Location = new Point(10, y + 3), AutoSize = true });
            var t = new TextBox { Location = new Point(110, y), Width = 250, Text = valor };
            Controls.Add(t);
            return t;
        }

        void Boton(string texto, int x, int y, EventHandler accion)
        {
            var b = new Button { Text = texto, Location = new Point(x, y), Width = 120 };
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

        string IniciarSesion()
        {
            string r = Web().UploadString(API + "/login", "POST", Json(new { email = txtEmail.Text, password = txtPassword.Text }));
            var d = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(r);
            token = d["token"].ToString();
            return "Sesion iniciada. Token:" + Environment.NewLine + token;
        }

        string VerSalidas()
        {
            var lista = new JavaScriptSerializer().Deserialize<List<Dictionary<string, object>>>(Web().DownloadString(API + "/salidas"));
            var sb = new StringBuilder();
            foreach (var s in lista)
            {
                sb.AppendLine(s["id"] + ") " + s["destino"] + "  " + s["fecha"] + " " + s["hora"] + "  bus " + s["bus"]
                              + "  Bs " + s["precio"] + "  vendidos " + s["asientos_count"] + "/" + s["capacidad"]);
            }
            return sb.Length == 0 ? "No hay salidas" : sb.ToString();
        }

        string VerSalida()
        {
            var s = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(Web().DownloadString(API + "/salidas/" + txtSalida.Text));
            var sb = new StringBuilder();
            sb.AppendLine("Destino: " + s["destino"] + "   Fecha: " + s["fecha"] + " " + s["hora"]);
            sb.AppendLine("Bus: " + s["bus"] + "   Precio: Bs " + s["precio"]);
            sb.AppendLine("Disponibles: " + s["disponibles"] + " de " + s["capacidad"]);
            sb.AppendLine("Asientos vendidos:");
            foreach (Dictionary<string, object> a in (System.Collections.ArrayList)s["asientos"])
            {
                sb.AppendLine("  Nro " + a["numero_asiento"] + " - CI " + a["ci_pasajero"]);
            }
            return sb.ToString();
        }

        string Vender()
        {
            if (token == "") return "Primero inicie sesion";
            int nro;
            if (!int.TryParse(txtAsiento.Text, out nro)) return "El numero de asiento debe ser un numero";
            if (txtCi.Text.Trim() == "") return "Ingrese el CI del pasajero";

            var cuerpo = new { salida_id = int.Parse(txtSalida.Text), numero_asiento = nro, ci_pasajero = txtCi.Text.Trim() };
            Web().UploadString(API + "/asientos", "POST", Json(cuerpo));
            return "Asiento " + nro + " vendido al CI " + txtCi.Text + Environment.NewLine + Environment.NewLine + VerSalida();
        }
    }
}
```

---

## Variantes que puede pedir el docente
| Si piden… | Cambia esto |
|---|---|
| Usar `firebase/php-jwt` en vez de hacerlo a mano | Ver [plantilla 07](../../plantillas/07-jwt-laravel/README.md); `creartoken` pasa a `JWT::encode(...)` y el middleware a `JWT::decode(...)` |
| Roles (solo "boletero" vende) | Agregar `rol` al payload en `creartoken` y en el middleware devolver 403 si `$payload['rol'] !== 'boletero'` |
| Reporte de ventas por salida | `Salida::withCount('asientos')->get()` y `capacidad - asientos_count` (ya lo usa `index`) |
| Que el pasajero exista en otro servicio (SEGIP) | En `AsientoController@store`, antes de crear: `Http::get('http://localhost:4000/...')` y si no existe → 404/422 |
| Precio total vendido | `$salida->precio * $salida->asientos()->count()` |
