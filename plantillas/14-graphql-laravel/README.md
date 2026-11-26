# 14 · GraphQL en Laravel (sin Lighthouse)

[← Plantillas](../README.md) · [Índice](../../README.md) · **[EXTRA]** (la Práctica 5 pidió "GraphQL con Laravel")

**Qué es:** un endpoint `POST /api/graphql` en Laravel. El esquema se escribe **igual que en Node** (tipos, queries, mutations con `resolve`), con la librería `webonyx/graphql-php`, que **ya viene en `base-laravel.zip`** y no necesita internet.

> ¿Por qué no Lighthouse? Lighthouse necesita `composer require` (internet) y una de sus dependencias está en GitLab. Si en tu PC sí lo tienes instalado, también vale; esta versión es el plan seguro.

| Archivo (dentro de `archivos/`) | Qué es |
|---|---|
| `database/migrations/…_create_recursos_table.php`, `app/Models/Recurso.php`, seeders | Los mismos de la [plantilla 06](../06-rest-laravel/README.md) |
| `app/GraphQL/Esquema.php` | Tipos, queries y mutations (como `schema.js`) |
| `app/Http/Controllers/GraphQLController.php` | Recibe `{query, variables}` y ejecuta |
| `routes/api.php` | `POST /api/graphql` |
| `public/graphiql.html` | Página para probar consultas **sin internet** |

## Pasos
1. `nuevo-laravel.bat graphql-laravel` ([sin internet](../../00-preparacion/sin-internet/README.md)), base `bd_examen` en `.env` y en phpMyAdmin.
2.
```bash
php artisan install:api
php artisan make:model Recurso -msfc
```
3. Pega los archivos (crea la carpeta `app/GraphQL`). La migración va **dentro** del archivo que creó artisan.
4.
```bash
php artisan migrate:fresh --seed
php artisan serve
```
5. Navegador: `http://localhost:8000/graphiql.html` → **Ejecutar**.

## app/GraphQL/Esquema.php
```php
<?php

namespace App\GraphQL;

use App\Models\Recurso;
use GraphQL\Error\UserError;
use GraphQL\Type\Definition\ObjectType;
use GraphQL\Type\Definition\Type;
use GraphQL\Type\Schema;

// Mismo esquema que la plantilla 08 de Node, escrito en PHP
class Esquema
{
    public static function crear(): Schema
    {
        // 1) TIPO: que campos se pueden pedir
        $recursoType = new ObjectType([
            'name' => 'Recurso',
            'fields' => [
                'id'     => Type::string(),
                'codigo' => Type::string(),
                'nombre' => Type::string(),
                'monto'  => Type::float(),
                'estado' => Type::string(),
            ],
        ]);

        // 2) QUERIES: leer
        $query = new ObjectType([
            'name' => 'RootQueryType',
            'fields' => [
                'recursos' => [
                    'type' => Type::listOf($recursoType),
                    'args' => ['estado' => Type::string()],
                    'resolve' => function ($root, $args) {
                        $q = Recurso::query();
                        if (!empty($args['estado'])) $q->where('estado', $args['estado']);
                        return $q->get();
                    },
                ],
                'recurso' => [
                    'type' => $recursoType,
                    'args' => ['codigo' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        $r = Recurso::where('codigo', $args['codigo'])->first();
                        if (!$r) throw new UserError('No existe ' . $args['codigo']);
                        // AQUI LOGICA
                        return $r;
                    },
                ],
            ],
        ]);

        // 3) MUTATIONS: crear / cambiar
        $mutation = new ObjectType([
            'name' => 'Mutation',
            'fields' => [
                'crearRecurso' => [
                    'type' => $recursoType,
                    'args' => [
                        'codigo' => Type::nonNull(Type::string()),
                        'nombre' => Type::nonNull(Type::string()),
                        'monto'  => Type::float(),
                    ],
                    'resolve' => function ($root, $args) {
                        if (Recurso::where('codigo', $args['codigo'])->exists()) {
                            throw new UserError('El codigo ya existe');
                        }
                        // AQUI LOGICA (validaciones antes de guardar)
                        return Recurso::create($args)->refresh();
                    },
                ],
                'cambiarEstado' => [
                    'type' => $recursoType,
                    'args' => [
                        'codigo' => Type::nonNull(Type::string()),
                        'estado' => Type::nonNull(Type::string()),
                    ],
                    'resolve' => function ($root, $args) {
                        $r = Recurso::where('codigo', $args['codigo'])->first();
                        if (!$r) throw new UserError('No existe ' . $args['codigo']);
                        $r->update(['estado' => $args['estado']]);
                        return $r;
                    },
                ],
            ],
        ]);

        return new Schema(['query' => $query, 'mutation' => $mutation]);
    }
}
```

## app/Http/Controllers/GraphQLController.php
```php
<?php

namespace App\Http\Controllers;

use App\GraphQL\Esquema;
use GraphQL\GraphQL;
use Illuminate\Http\Request;

// Un solo endpoint:  POST /api/graphql   { "query": "...", "variables": {...} }
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

## routes/api.php
```php
<?php

use App\Http\Controllers\GraphQLController;
use Illuminate\Support\Facades\Route;

// http://localhost:8000/api/graphql
Route::post('/graphql', GraphQLController::class);
```

## public/graphiql.html
```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Probar GraphQL</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 900px; margin: 20px auto; }
    textarea, pre { width: 100%; font-family: monospace; }
    pre { background: #f2f2f2; padding: 10px; white-space: pre-wrap; }
  </style>
</head>
<body>
  <h2>Probar GraphQL (sin internet)</h2>
  <p>Consulta</p>
  <textarea id="query" rows="8">{ recursos { codigo nombre monto estado } }</textarea>
  <p>Variables (JSON, opcional)</p>
  <textarea id="variables" rows="3"></textarea>
  <button onclick="enviar()">Ejecutar</button>
  <pre id="salida"></pre>
  <script>
    async function enviar() {
      const texto = document.getElementById('variables').value.trim();
      const r = await fetch('/api/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ query: document.getElementById('query').value, variables: texto ? JSON.parse(texto) : null })
      });
      document.getElementById('salida').textContent = JSON.stringify(await r.json(), null, 2);
    }
  </script>
</body>
</html>
```

Migración, modelo y seeders: copia los de la [plantilla 06](../06-rest-laravel/README.md) (también están en `archivos/`).

## Probar
En `graphiql.html` o con curl:
```bat
curl http://localhost:8000/api/graphql -H "Content-Type: application/json" -d "{\"query\":\"{ recursos { codigo nombre monto estado } }\"}"
```
```graphql
{ recurso(codigo: "A1") { nombre estado } }
mutation { crearRecurso(codigo: "B1", nombre: "Nuevo", monto: 5) { codigo estado } }
mutation { cambiarEstado(codigo: "A1", estado: "pagado") { codigo estado } }
{ recursos(estado: "pagado") { codigo } }
```
Con variables: consulta `query($c: String!) { recurso(codigo: $c) { nombre } }` y variables `{ "c": "A1" }`.

Un código que no existe responde `{"errors":[{"message":"No existe ZZ"...}],"data":{"recurso":null}}` (**HTTP 200**, como todo GraphQL).

## Equivalencias Node ↔ Laravel
| Node (`schema.js`) | Laravel (`Esquema.php`) |
|---|---|
| `new GraphQLObjectType({ name, fields })` | `new ObjectType(['name' => ..., 'fields' => [...]])` |
| `GraphQLString`, `GraphQLFloat`, `GraphQLInt`, `GraphQLBoolean` | `Type::string()`, `Type::float()`, `Type::int()`, `Type::boolean()` |
| `new GraphQLList(T)` / `new GraphQLNonNull(T)` | `Type::listOf($t)` / `Type::nonNull($t)` |
| `resolve(parent, args)` | `'resolve' => function ($root, $args) { ... }` |
| `throw new Error('...')` | `throw new UserError('...')` |
| `Recurso.findAll()` (Sequelize) | `Recurso::all()` (Eloquent) |

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Otro campo | Migración, `$fillable` y el `fields` del tipo |
| Consultarlo desde C#, Java o web | Es un POST con `{"query": ...}` → [logica/consumir](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) (cambia la URL a `/api/graphql`) |
| Que un resolver llame a otro servicio | `Http::get(...)` dentro del `resolve` ([DesdeLaravel](../../logica/README.md#desdelaravelphp)) |

## Cómo defenderlo
- "Hay **un solo endpoint**; el cliente manda la consulta y elige los campos."
- "El esquema define tipos y cada campo tiene un **resolver** que va a la base con Eloquent."
- "Los errores vuelven en `errors` con HTTP 200, por eso el cliente revisa ese campo."
