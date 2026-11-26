# 06 · API REST en Laravel

[← Plantillas](../README.md) · [Índice](../../README.md) · **[INGE]**

**Qué es:** un CRUD completo (listar, ver, crear, cambiar, borrar) sobre un recurso, con validación y códigos HTTP correctos.

| Archivo (dentro de `archivos/`) | Qué es |
|---|---|
| `database/migrations/..._create_recursos_table.php` | Crea la tabla |
| `app/Models/Recurso.php` | La clase de la tabla |
| `app/Http/Controllers/RecursoController.php` | Qué pasa en cada petición |
| `routes/api.php` | Las direcciones |
| `database/seeders/RecursoSeeder.php` | Datos de prueba |
| `database/seeders/DatabaseSeeder.php` | Llama a los seeders y crea el usuario de prueba |

## Pasos (comandos [INGE], en este orden)

**1.** Proyecto: `laravel new central-rest` (con internet) o [copia tu base](../../00-preparacion/README.md#d2-proyecto-laravel-nuevo) (sin internet).

**2.** Configura la base de datos en `.env`.

**3.**
```bash
php artisan install:api
php artisan make:model Recurso -msfc --api
```
Eso crea modelo, migración, seeder, factory y controlador.

**4.** Reemplaza el contenido de cada archivo creado por el de abajo. La migración que creó artisan tiene **otra fecha** en el nombre: pega el contenido **dentro de ese archivo** (no crees otro).

**5.**
```bash
php artisan migrate:fresh --seed
php artisan route:list --path=api
php artisan serve
```

**6.** Navegador: `http://localhost:8000/api/recursos` → salen 2 registros.

## 1) Migración
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('recursos', function (Blueprint $table) {
            $table->id();
            // CAMBIAR columnas segun el enunciado
            $table->string('codigo', 30)->unique();
            $table->string('nombre', 100);
            $table->decimal('monto', 10, 2)->default(0);
            $table->string('estado', 20)->default('activo');
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('recursos');
    }
};
```

## 2) app/Models/Recurso.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Recurso extends Model
{
    use HasFactory;

    protected $table = 'recursos';   // nombre EXACTO de la tabla en la migracion

    // Campos que se pueden guardar desde la API (los mismos de la migracion)
    protected $fillable = ['codigo', 'nombre', 'monto', 'estado'];
}
```

## 3) app/Http/Controllers/RecursoController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Recurso;
use Illuminate\Http\Request;

class RecursoController extends Controller
{
    // GET /api/recursos
    public function index()
    {
        // AQUI LOGICA (filtros)
        return response()->json(Recurso::all());
    }

    // POST /api/recursos
    public function store(Request $request)
    {
        // Si algo no cumple, responde 422 solo
        $datos = $request->validate([
            'codigo' => 'required|string',
            'nombre' => 'required|string',
            'monto'  => 'required|numeric',
        ]);

        if (Recurso::where('codigo', $datos['codigo'])->exists()) {
            return response()->json(['error' => 'El codigo ya existe'], 409);
        }

        // AQUI LOGICA (reglas antes de guardar, llamar a otros servicios)

        $recurso = Recurso::create($datos);
        return response()->json($recurso->refresh(), 201);
    }

    // GET /api/recursos/{recurso}   (si no existe responde 404 solo)
    public function show(Recurso $recurso)
    {
        // AQUI LOGICA
        return response()->json($recurso);
    }

    // PUT o PATCH /api/recursos/{recurso}
    public function update(Request $request, Recurso $recurso)
    {
        $datos = $request->validate([
            'nombre' => 'sometimes|string',
            'monto'  => 'sometimes|numeric',
            'estado' => 'sometimes|string',
        ]);

        // AQUI LOGICA

        $recurso->update($datos);
        return response()->json($recurso);
    }

    // DELETE /api/recursos/{recurso}
    public function destroy(Recurso $recurso)
    {
        // AQUI LOGICA (si piden borrado logico, ver la carpeta logica/)
        $recurso->delete();
        return response()->noContent();   // 204
    }
}
```

## 4) routes/api.php
```php
<?php

use App\Http\Controllers\RecursoController;
use Illuminate\Support\Facades\Route;

// Todas estas rutas empiezan con /api
// parameters(...) es OBLIGATORIO en espanol: sin eso Laravel puede inventar
// {multum} o {cuentum} y el controlador recibe un registro vacio.
Route::apiResource('recursos', RecursoController::class)
    ->parameters(['recursos' => 'recurso']);
```

## 5) database/seeders/RecursoSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Recurso;
use Illuminate\Database\Seeder;

class RecursoSeeder extends Seeder
{
    public function run(): void
    {
        // Datos de prueba. Si el examen da una tabla de datos, copiarla aqui.
        Recurso::create(['codigo' => 'A1', 'nombre' => 'Ejemplo uno', 'monto' => 100]);
        Recurso::create(['codigo' => 'A2', 'nombre' => 'Ejemplo dos', 'monto' => 200]);
    }
}
```

## 6) database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // Usuario para probar el login (la clave se guarda cifrada sola)
        User::create(['name' => 'Admin', 'email' => 'admin@test.com', 'password' => '123456']);

        // Registrar aqui cada seeder, para que --seed lo ejecute
        $this->call([
            RecursoSeeder::class,
        ]);
    }
}
```

## ⚠️ La trampa del plural en español
Laravel adivina singulares **en inglés**: de `multa` saca `{multum}`, y el controlador recibe un registro **vacío** sin dar error claro.
**Solución (ya está en la plantilla):** `->parameters(['multas' => 'multa'])` y `protected $table` en el modelo. Revisa siempre con `route:list`.

## Probar [INGE: curl] (símbolo del sistema)
```bat
curl http://localhost:8000/api/recursos -H "Accept: application/json"
curl -X POST http://localhost:8000/api/recursos -H "Accept: application/json" -H "Content-Type: application/json" -d "{\"codigo\":\"B1\",\"nombre\":\"Nuevo\",\"monto\":5}"
curl -X PATCH http://localhost:8000/api/recursos/1 -H "Accept: application/json" -H "Content-Type: application/json" -d "{\"estado\":\"pagado\"}"
curl -X DELETE http://localhost:8000/api/recursos/1 -H "Accept: application/json"
```

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Buscar por código en vez de id | [getRouteKeyName](../../logica/README.md#laravel-buscar-por-código) |
| Borrado lógico | [borrado lógico](../../logica/README.md#laravel-borrado-lógico) |
| Filtros (`?estado=`) o totales | [filtros](../../logica/README.md#laravel-filtros-y-totales) |
| Débito / abono de saldo | [operación sobre saldo](../../logica/README.md#laravel-débito-y-abono) |
| Dos tablas relacionadas | [uno a muchos](../../logica/README.md#laravel-relación-uno-a-muchos) |
| Protegerlo con token | [plantilla 07](../07-jwt-laravel/README.md) |
| Llamar a otro servicio | [DesdeLaravel](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |
| Documentación Swagger | `composer require dedoc/scramble` **[INGE]** (necesita internet antes) → `/docs/api` |
| El navegador bloquea por CORS | `php artisan config:publish cors` **[INGE]** y revisar `config/cors.php` |

Reglas de validación útiles: `required`, `numeric`, `date`, `digits_between:5,10`, `in:A,B`.

## Cómo defenderlo
- "REST modela **recursos** con URL propia y usa los **verbos HTTP** para las acciones."
- "`apiResource` crea las 5 rutas: index, store, show, update, destroy."
- "`validate` responde **422** solo; si pido un id que no existe, el *route model binding* responde **404**."
- "201 es creado, 204 borrado sin contenido, 409 conflicto."
- "`$fillable` protege: solo esos campos se pueden llenar desde afuera."
- "Es **sin estado**: cada petición lleva todo lo que necesita."
