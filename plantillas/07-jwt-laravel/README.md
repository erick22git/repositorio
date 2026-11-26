# 07 · Login con JWT en Laravel

[← Plantillas](../README.md) · [Índice](../../README.md) · **[INGE]**

**Qué es:** el usuario hace login y recibe un **token**; con ese token puede usar las rutas protegidas.

Primero ten funcionando la [plantilla REST](../06-rest-laravel/README.md) (el `DatabaseSeeder` ya crea el usuario `admin@test.com` / `123456`).

| Archivo | Qué hace |
|---|---|
| `LoginController.php` | Verifica correo y clave, y firma el token |
| `JwtMiddleware.php` | El portero: sin token válido responde 401 |
| `routes/api.php` | `/login` libre y el resto protegido |

## Pasos (comandos [INGE])
```bash
composer require firebase/php-jwt        # necesita internet: hazlo en tu proyecto base
php artisan make:controller LoginController
php artisan make:middleware JwtMiddleware
```
En `.env`:
```ini
JWT_SECRET=clave_secreta_del_examen_2026_de_32_letras_o_mas
JWT_ALGORITHM=HS256
```
> Con las versiones nuevas de la librería, un secreto **corto da error** `Provided key is too short`. Usa 32 caracteres o más.

Pega los archivos y prueba. No hace falta migrar de nuevo si ya corriste `--seed`.

## app/Http/Controllers/LoginController.php
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

## app/Http/Middleware/JwtMiddleware.php
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

## routes/api.php
```php
<?php

use App\Http\Controllers\LoginController;
use App\Http\Controllers\RecursoController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);   // libre

Route::apiResource('recursos', RecursoController::class)    // protegido
    ->parameters(['recursos' => 'recurso'])
    ->middleware(JwtMiddleware::class);
```

## Probar (símbolo del sistema)
```bat
curl -X POST http://localhost:8000/api/login -H "Accept: application/json" -H "Content-Type: application/json" -d "{\"email\":\"admin@test.com\",\"password\":\"123456\"}"
REM copia el token y pegalo en vez de TOKEN
curl http://localhost:8000/api/recursos -H "Accept: application/json" -H "Authorization: Bearer TOKEN"
```
Sin token debe responder **401**.

## Plan B: no pude instalar la librería (sin internet)
Versión a mano, mismo método que usa la resolución del segundo parcial en el repo del curso (`hash_hmac` + base64url).
Copia estos 3 archivos (`JwtSimple.php` va en **`app/Support/`**, crea la carpeta). Las rutas son las mismas.

### app/Support/JwtSimple.php
```php
<?php

// JWT a mano (HS256), por si NO se puede instalar firebase/php-jwt.
// Copiar en app/Support/JwtSimple.php
namespace App\Support;

class JwtSimple
{
    private static function b64(string $texto): string
    {
        return rtrim(strtr(base64_encode($texto), '+/', '-_'), '=');
    }

    // header.payload.firma
    public static function crear(array $data, string $secreto): string
    {
        $h = self::b64(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
        $p = self::b64(json_encode(['iat' => time(), 'exp' => time() + 3600, 'data' => $data]));
        $f = self::b64(hash_hmac('sha256', "$h.$p", $secreto, true));
        return "$h.$p.$f";
    }

    // Devuelve el payload si el token es valido, o null si no
    public static function verificar(string $token, string $secreto): ?array
    {
        $partes = explode('.', $token);
        if (count($partes) !== 3) return null;

        [$h, $p, $f] = $partes;
        $esperada = self::b64(hash_hmac('sha256', "$h.$p", $secreto, true));
        if (!hash_equals($esperada, $f)) return null;

        $payload = json_decode(base64_decode(strtr($p, '-_', '+/')), true);
        if (!$payload || ($payload['exp'] ?? 0) < time()) return null;

        return $payload;
    }
}
```

### app/Http/Controllers/LoginController.php
```php
<?php

// Version SIN libreria. Reemplaza a app/Http/Controllers/LoginController.php
namespace App\Http\Controllers;

use App\Models\User;
use App\Support\JwtSimple;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class LoginController extends Controller
{
    public function login(Request $request)
    {
        $request->validate(['email' => 'required', 'password' => 'required']);

        $user = User::where('email', $request->email)->first();
        if (!$user || !Hash::check($request->password, $user->password)) {
            return response()->json(['error' => 'Credenciales invalidas'], 401);
        }

        $token = JwtSimple::crear(['id' => $user->id, 'email' => $user->email], env('JWT_SECRET'));
        return response()->json(['token' => $token, 'type' => 'bearer']);
    }
}
```

### app/Http/Middleware/JwtMiddleware.php
```php
<?php

// Version SIN libreria. Reemplaza a app/Http/Middleware/JwtMiddleware.php
namespace App\Http\Middleware;

use App\Support\JwtSimple;
use Closure;
use Illuminate\Http\Request;

class JwtMiddleware
{
    public function handle(Request $request, Closure $next)
    {
        $jwt = str_replace('Bearer ', '', $request->header('Authorization', ''));
        $payload = JwtSimple::verificar($jwt, env('JWT_SECRET'));

        if ($payload === null) {
            return response()->json(['error' => 'Token invalido o ausente'], 401);
        }

        $request->attributes->add(['usuario' => $payload['data']]);
        return $next($request);
    }
}
```

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Proteger solo algunas rutas | `->only(...)` o meterlas en un `Route::middleware(JwtMiddleware::class)->group(...)` |
| Saber quién es el usuario | `$request->attributes->get('usuario')` |
| Token que dure más | Cambiar `time() + 3600` (segundos) |
| Un servicio da el token y **otro** lo valida | Los dos con el **mismo** `JWT_SECRET` → [ejercicio 3](../../ejercicios/3-pago-en-linea/README.md) |
| Login con otro dato (CI, usuario) | Cambiar el `where` del `LoginController` y la migración de `users` |

## Cómo defenderlo
- "El JWT tiene **cabecera.payload.firma** en Base64URL. El payload se puede leer; **no se puede modificar** sin que falle la firma."
- "**No va cifrado**, va **firmado** (HS256, clave compartida)."
- "El servidor no guarda sesiones: con la clave verifica cualquier token (**sin estado**)."
- "`exp` hace que venza. `Hash::check` compara la clave contra el hash guardado."
- "401 = no autenticado."
