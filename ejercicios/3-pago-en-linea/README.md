# Ejercicio 3 · Pago en línea (parcial anterior)

[← Ejercicios](../README.md) · [Índice](../../README.md)

| Versión | Me piden… |
|---|---|
| [1](#versión-1--realizar-el-pago) | Pagar de una cuenta a otra |
| [2](#versión-2--además-consultar-saldo) | Además consultar saldo |
| [3](#versión-3--además-historial) | Además historial |

## Enunciado resumido
Un comercio cobra a través de una **plataforma de intercambio (RMI)**. La plataforma pide el tipo de cambio al **Banco Central (socket TCP)**, obtiene un **token** del servicio **ATC (REST)** y opera las cuentas en dos bancos **REST con JWT**: uno en **PHP/Laravel** (BNB) y otro en **Java** (Unión).

```
 Comercio ─RMI→ Plataforma :1099 ─TCP fecha→ Banco Central :5002   (responde 7)
                    ├─ POST /api/login ────→ ATC :8083              (da el token)
                    ├─ REST + token ───────→ Banco BNB (Laravel) :8001
                    └─ REST + token ───────→ Banco Unión (Java)  :8085
```

## Proyectos

| Carpeta | Tecnología | Puerto | Base de datos |
|---|---|---|---|
| `banco-bnb-laravel` | Laravel | 8001 | `bd_bnb` |
| `atc-login-java` | Java | 8083 | — |
| `banco-union-java` | Java | 8085 | — |
| `banco-central-tcp` | Java | 5002 | — |
| `plataforma-rmi` | Java | 1099 | — |
| `comercio` | Java (cliente) | — | — |

## Orden de arranque
BNB → ATC → Unión → Banco Central → Plataforma → Comercio.

## Datos de prueba
Cuentas: Unión `32212` (Bs 40), `32215` (USD 50). BNB `12345` (Bs 455), `12455` (USD 300).

| Cliente | Vendedor | Monto | Moneda | Resultado |
|---|---|---|---|---|
| 32212 | 12345 | 10 | Bs | PAGO APROBADO |
| 12455 | 32215 | 5 | USD | PAGO APROBADO |
| 32212 | 12345 | 1000 | Bs | PAGO RECHAZADO (sin saldo) |

---

## Versión 1 · Realizar el pago

### Paso 1 · Banco BNB (Laravel)
1. Crea la base `bd_bnb` y [un proyecto Laravel](../../00-preparacion/README.md#d2-proyecto-laravel-nuevo). En `.env`: `DB_DATABASE=bd_bnb` y el `JWT_SECRET` de la guía (**debe ser exactamente el mismo que en Java**).
2. Comandos:
```bash
php artisan install:api
php artisan make:model Cuenta -msc --api
php artisan make:middleware JwtMiddleware
```
3. Pega los archivos (crea la carpeta `app/Support`).
4. Migra y levanta:
```bash
php artisan migrate:fresh --seed
php artisan serve --port=8001
```

#### Migración (database/migrations/…_create_cuentas_table.php)
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('cuentas', function (Blueprint $table) {
            $table->id();
            $table->string('nrocuenta', 20)->unique();
            $table->string('nombre', 100);
            $table->string('moneda', 5);                    // Bs o USD
            $table->decimal('saldo', 12, 2)->default(0);
            $table->boolean('activo')->default(true);       // borrado logico
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('cuentas');
    }
};
```
#### app/Models/Cuenta.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Cuenta extends Model
{
    use HasFactory;

    protected $table = 'cuentas';
    protected $fillable = ['nrocuenta', 'nombre', 'moneda', 'saldo', 'activo'];

    // /api/cuentas/12345 busca por nrocuenta y no por id
    public function getRouteKeyName(): string
    {
        return 'nrocuenta';
    }
}
```
#### app/Http/Controllers/CuentaController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Cuenta;
use Illuminate\Http\Request;

class CuentaController extends Controller
{
    public function index()
    {
        return response()->json(Cuenta::where('activo', true)->get());
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'nrocuenta' => 'required|string',
            'nombre'    => 'required|string',
            'moneda'    => 'required|in:Bs,USD',
        ]);

        if (Cuenta::where('nrocuenta', $datos['nrocuenta'])->exists()) {
            return response()->json(['error' => 'La cuenta ya existe'], 409);
        }

        return response()->json(Cuenta::create($datos)->refresh(), 201);
    }

    public function show(Cuenta $cuenta)
    {
        if (!$cuenta->activo) {
            return response()->json(['error' => 'Cuenta no existe'], 404);
        }
        return response()->json($cuenta);
    }

    // PATCH /api/cuentas/{nro}   { "operacion": "debito" | "abono", "monto": 10 }
    public function update(Request $request, Cuenta $cuenta)
    {
        $datos = $request->validate([
            'operacion' => 'required|in:debito,abono',
            'monto'     => 'required|numeric|gt:0',
        ]);

        if (!$cuenta->activo) {
            return response()->json(['error' => 'Cuenta no existe'], 404);
        }

        if ($datos['operacion'] === 'debito') {
            if ($cuenta->saldo < $datos['monto']) {
                return response()->json(['error' => 'Saldo insuficiente'], 409);
            }
            $cuenta->saldo -= $datos['monto'];
        } else {
            $cuenta->saldo += $datos['monto'];
        }
        $cuenta->save();

        return response()->json($cuenta);
    }

    public function destroy(Cuenta $cuenta)
    {
        $cuenta->update(['activo' => false]);
        return response()->noContent();
    }
}
```
#### app/Support/JwtSimple.php
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
#### app/Http/Middleware/JwtMiddleware.php
```php
<?php

// Valida el token que emitio el servicio ATC (mismo JWT_SECRET)
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
#### routes/api.php
```php
<?php

use App\Http\Controllers\CuentaController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::apiResource('cuentas', CuentaController::class)
    ->parameters(['cuentas' => 'cuenta'])
    ->middleware(JwtMiddleware::class);
```
#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Cuenta;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Cuenta::create(['nrocuenta' => '12345', 'nombre' => 'Cliente Tres', 'moneda' => 'Bs', 'saldo' => 455]);
        Cuenta::create(['nrocuenta' => '12455', 'nombre' => 'Cliente Cuatro', 'moneda' => 'USD', 'saldo' => 300]);
    }
}
```

### Paso 2 · ATC, el que da el token (Java)
Proyecto Java con estas dos clases → ejecuta `ServidorATC`.
#### JwtJava.java
```java
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// JWT HS256 sin librerias. Compatible con JwtSimple.php y con firebase/php-jwt
// siempre que todos usen EL MISMO SECRETO.
public class JwtJava {

    static final String SECRETO = "clave_secreta_del_examen_2026_de_32_letras_o_mas";

    static String crear(String usuario) {
        long ahora = System.currentTimeMillis() / 1000;
        String h = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String p = b64(("{\"iat\":" + ahora + ",\"exp\":" + (ahora + 3600)
                + ",\"data\":{\"user\":\"" + usuario + "\"}}").getBytes(StandardCharsets.UTF_8));
        return h + "." + p + "." + firmar(h + "." + p);
    }

    // true si la firma es correcta y no vencio
    static boolean verificar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) return false;
            if (!firmar(partes[0] + "." + partes[1]).equals(partes[2])) return false;

            String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)").matcher(payload);
            return m.find() && Long.parseLong(m.group(1)) > System.currentTimeMillis() / 1000;
        } catch (Exception e) {
            return false;
        }
    }

    private static String firmar(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return b64(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }
}
```
#### ServidorATC.java
```java
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

// Servicio ATC:  GET /api/usuarios   GET /api/usuarios/{id}   POST /api/login -> token
public class ServidorATC {

    // id -> {user, password}
    static final Map<String, String[]> USUARIOS = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        USUARIOS.put("1", new String[]{"comercio", "1234"});
        USUARIOS.put("2", new String[]{"admin", "admin"});

        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
        server.createContext("/api/login", ServidorATC::login);
        server.createContext("/api/usuarios", ServidorATC::usuarios);
        server.start();
        System.out.println("ATC en http://localhost:8083/api/login");
    }

    static void login(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { responder(ex, 405, "{\"error\":\"use POST\"}"); return; }
        String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String user = campo(cuerpo, "user");
        String password = campo(cuerpo, "password");

        for (String[] u : USUARIOS.values()) {
            if (u[0].equals(user) && u[1].equals(password)) {
                responder(ex, 200, "{\"token\":\"" + JwtJava.crear(user) + "\",\"type\":\"bearer\"}");
                return;
            }
        }
        responder(ex, 401, "{\"error\":\"credenciales invalidas\"}");
    }

    static void usuarios(HttpExchange ex) throws IOException {
        String id = ex.getRequestURI().getPath().replaceFirst("/api/usuarios/?", "");
        List<String> lista = new ArrayList<>();
        USUARIOS.forEach((k, u) -> {
            if (id.isEmpty() || id.equals(k)) lista.add("{\"id\":\"" + k + "\",\"user\":\"" + u[0] + "\"}");
        });
        if (lista.isEmpty()) responder(ex, 404, "{\"error\":\"no existe\"}");
        else responder(ex, 200, id.isEmpty() ? "[" + String.join(",", lista) + "]" : lista.get(0));
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static void responder(HttpExchange ex, int codigo, String cuerpo) throws IOException {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(codigo, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }
}
```

### Paso 3 · Banco Unión (Java)
Otro proyecto: `ServidorUnion.java` **y una copia de** `JwtJava.java` (la de arriba) → ejecuta.
#### ServidorUnion.java
```java
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

// Banco Union: REST en Java, protegido con JWT.  Recurso /api/cuentas
public class ServidorUnion {

    static final String BASE = "/api/cuentas";

    static class Cuenta {
        String nrocuenta, nombre, moneda;
        double saldo;
        boolean activo = true;          // borrado logico

        Cuenta(String n, String nom, String mon, double s) {
            nrocuenta = n; nombre = nom; moneda = mon; saldo = s;
        }

        String json() {
            return "{\"nrocuenta\":\"" + nrocuenta + "\",\"nombre\":\"" + nombre
                    + "\",\"moneda\":\"" + moneda + "\",\"saldo\":" + saldo + "}";
        }
    }

    static final Map<String, Cuenta> CUENTAS = new LinkedHashMap<>();

    static void guardar(Cuenta c) { CUENTAS.put(c.nrocuenta, c); }

    public static void main(String[] args) throws IOException {
        guardar(new Cuenta("32212", "Cliente Uno", "Bs", 40));
        guardar(new Cuenta("32215", "Cliente Dos", "USD", 50));
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);
        server.createContext(BASE, ServidorUnion::atender);
        server.start();
        System.out.println("Banco Union en http://localhost:8085" + BASE);
    }

    static synchronized void atender(HttpExchange ex) throws IOException {
        try {
            // 1) PORTERO: sin token valido no pasa nadie
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !JwtJava.verificar(auth.replace("Bearer ", ""))) {
                responder(ex, 401, error("token invalido o ausente"));
                return;
            }

            String metodo = ex.getRequestMethod();
            String nro = ex.getRequestURI().getPath().substring(BASE.length()).replace("/", "");
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            // 2) Sin numero: listar o crear
            if (nro.isEmpty()) {
                if (metodo.equals("GET")) {
                    List<String> lista = new ArrayList<>();
                    for (Cuenta c : CUENTAS.values()) if (c.activo) lista.add(c.json());
                    responder(ex, 200, "[" + String.join(",", lista) + "]");
                } else if (metodo.equals("POST")) {
                    String n = campo(cuerpo, "nrocuenta"), nom = campo(cuerpo, "nombre"), mon = campo(cuerpo, "moneda");
                    if (n == null || nom == null || mon == null) { responder(ex, 422, error("faltan datos")); return; }
                    if (CUENTAS.containsKey(n)) { responder(ex, 409, error("ya existe")); return; }
                    guardar(new Cuenta(n, nom, mon, 0));
                    responder(ex, 201, CUENTAS.get(n).json());
                } else {
                    responder(ex, 405, error("metodo no permitido"));
                }
                return;
            }

            // 3) Con numero: ver, operar o borrar
            Cuenta c = CUENTAS.get(nro);
            if (c == null || !c.activo) { responder(ex, 404, error("cuenta no existe")); return; }

            if (metodo.equals("GET")) {
                responder(ex, 200, c.json());
            } else if (metodo.equals("PATCH")) {
                String operacion = campo(cuerpo, "operacion");
                String texto = campo(cuerpo, "monto");
                double monto = texto == null ? 0 : Double.parseDouble(texto);
                if (monto <= 0) { responder(ex, 422, error("monto invalido")); return; }

                if ("debito".equals(operacion)) {
                    if (c.saldo < monto) { responder(ex, 409, error("saldo insuficiente")); return; }
                    c.saldo -= monto;
                } else if ("abono".equals(operacion)) {
                    c.saldo += monto;
                } else {
                    responder(ex, 422, error("operacion debe ser debito o abono"));
                    return;
                }
                responder(ex, 200, c.json());
            } else if (metodo.equals("DELETE")) {
                c.activo = false;
                responder(ex, 204, "");
            } else {
                responder(ex, 405, error("metodo no permitido"));
            }
        } catch (Exception e) {
            responder(ex, 500, error("error interno"));
        }
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*(\"([^\"]*)\"|[-0-9.]+)").matcher(json);
        if (!m.find()) return null;
        return m.group(2) != null ? m.group(2) : m.group(1);
    }

    static String error(String m) { return "{\"error\":\"" + m + "\"}"; }

    static void responder(HttpExchange ex, int codigo, String cuerpo) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        if (codigo == 204) {
            ex.sendResponseHeaders(204, -1);
        } else {
            ex.sendResponseHeaders(codigo, b.length);
            ex.getResponseBody().write(b);
        }
        ex.close();
    }
}
```

### Paso 4 · Banco Central (socket)
#### ServidorBancoCentral.java
```java
import java.io.*;
import java.net.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorBancoCentral {

    static final int PUERTO = 5002;   // cambiar si el enunciado pide otro

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servidor TCP escuchando en el puerto " + PUERTO);
            while (true) {
                Socket client = server.accept();            // se queda esperando un cliente
                new Thread(() -> atender(client)).start();  // un hilo por cliente
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    static void atender(Socket client) {
        try (client;
             BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream toClient = new PrintStream(client.getOutputStream())) {

            String recibido = fromClient.readLine();
            System.out.println("Llego: " + recibido);
            String respuesta = procesarSolicitud(recibido);
            toClient.println(respuesta);
            System.out.println("Respondi: " + respuesta);

        } catch (Exception e) {
            System.out.println("Error con un cliente: " + e.getMessage());
        }
    }

    // Recibe una fecha (ej. 2026-11-26) y devuelve la cotizacion del dolar
    static String procesarSolicitud(String fecha) {
        return "7";          // el enunciado dice: siempre 7
    }
}
```

### Paso 5 · Plataforma (RMI)
#### IPlataforma.java
```java
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPlataforma extends Remote {
    boolean realizarTransaccion(String nrocuentacliente, String cuentavendedor,
                                double monto, String moneda) throws RemoteException;
}
```
#### Plataforma.java
```java
import java.net.URI;
import java.net.http.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.regex.*;

// ORQUESTADOR: cotizacion (TCP) + token (ATC) + los dos bancos (REST)
public class Plataforma extends UnicastRemoteObject implements IPlataforma {
    private static final long serialVersionUID = 1L;

    static final String ATC = "http://localhost:8083/api/login";
    static final String[] BANCOS = {
            "http://localhost:8001/api/cuentas",     // BNB (PHP / Laravel)
            "http://localhost:8085/api/cuentas"      // Union (Java)
    };

    public Plataforma() throws RemoteException {
        super();
    }

    @Override
    public synchronized boolean realizarTransaccion(String origen, String destino, double monto, String moneda)
            throws RemoteException {
        try {
            if (monto <= 0 || origen.equals(destino)) return fallo("datos invalidos");

            // 1) Cotizacion del dia
            double cambio = Double.parseDouble(ClienteTCP.enviar("localhost", 5002, LocalDate.now().toString()));

            // 2) Token
            String login = pedir("POST", ATC, "{\"user\":\"comercio\",\"password\":\"1234\"}", null).body();
            String token = campo(login, "token");
            if (token == null) return fallo("ATC no dio token");

            // 3) Buscar cada cuenta en los bancos
            String[] cliente = buscar(origen, token);
            String[] vendedor = buscar(destino, token);
            if (cliente == null || vendedor == null) return fallo("cuenta no encontrada");

            // 4) Convertir el monto a la moneda de cada cuenta
            double aDebitar = convertir(monto, moneda, cliente[1], cambio);
            double aAbonar = convertir(monto, moneda, vendedor[1], cambio);

            // 5) Debitar (el banco rechaza con 409 si no alcanza)
            if (!operar(cliente[0], "debito", aDebitar, token)) return fallo("no se pudo debitar");

            // 6) Abonar. Si falla, DEVOLVER el dinero (compensacion)
            if (!operar(vendedor[0], "abono", aAbonar, token)) {
                operar(cliente[0], "abono", aDebitar, token);
                return fallo("no se pudo abonar, se devolvio");
            }
            System.out.println("Transaccion OK: " + monto + " " + moneda + " de " + origen + " a " + destino);
            return true;

        } catch (Exception e) {
            return fallo("error: " + e);
        }
    }

    private boolean fallo(String motivo) {
        System.out.println("Transaccion RECHAZADA: " + motivo);
        return false;
    }

    // Devuelve {url de la cuenta, moneda} o null
    private String[] buscar(String nro, String token) throws Exception {
        for (String banco : BANCOS) {
            HttpResponse<String> r = pedir("GET", banco + "/" + nro, null, token);
            if (r.statusCode() == 200) return new String[]{banco + "/" + nro, campo(r.body(), "moneda")};
        }
        return null;
    }

    private boolean operar(String url, String operacion, double monto, String token) throws Exception {
        String json = "{\"operacion\":\"" + operacion + "\",\"monto\":" + monto + "}";
        return pedir("PATCH", url, json, token).statusCode() == 200;
    }

    static double convertir(double monto, String de, String a, double cambio) {
        if (de.equalsIgnoreCase(a)) return monto;
        double r = de.equalsIgnoreCase("USD") ? monto * cambio : monto / cambio;
        return Math.round(r * 100) / 100.0;
    }

    private HttpResponse<String> pedir(String metodo, String url, String json, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                                      : HttpRequest.BodyPublishers.ofString(json));
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
```
#### ServidorPlataforma.java
```java
import java.rmi.registry.LocateRegistry;

public class ServidorPlataforma {
    public static void main(String[] args) throws Exception {
        LocateRegistry.createRegistry(1099).rebind("Plataforma", new Plataforma());
        System.out.println("Plataforma de Intercambio (RMI) en 1099");
    }
}
```
#### ClienteTCP.java
```java
import java.io.*;
import java.net.*;

// CLIENTE TCP. El metodo enviar() se puede copiar dentro de un servidor RMI u otro programa.
public class ClienteTCP {

    public static void main(String[] args) {
        String mensaje = args.length > 0 ? args[0] : "hola";
        System.out.println("Respuesta: " + enviar("localhost", 5002, mensaje));
    }

    static String enviar(String host, int puerto, String mensaje) {
        try (Socket client = new Socket(host, puerto);
             PrintStream toServer = new PrintStream(client.getOutputStream());
             BufferedReader fromServer = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            client.setSoTimeout(5000);          // no esperar para siempre
            toServer.println(mensaje);
            return fromServer.readLine();

        } catch (IOException e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
```

### Paso 6 · Comercio (el cliente)
Copia también `IPlataforma.java` (**mismo paquete** que en la plataforma; lo más fácil es no poner paquete en ninguno).
#### SitioComercio.java
```java
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class SitioComercio {
    public static void main(String[] args) throws Exception {
        IPlataforma plataforma = (IPlataforma) LocateRegistry.getRegistry("localhost", 1099).lookup("Plataforma");

        Scanner sc = new Scanner(System.in);
        System.out.print("Cuenta del cliente: ");
        String origen = sc.nextLine().trim();
        System.out.print("Cuenta del vendedor: ");
        String destino = sc.nextLine().trim();
        System.out.print("Monto: ");
        double monto = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Moneda (Bs / USD): ");
        String moneda = sc.nextLine().trim();

        boolean ok = plataforma.realizarTransaccion(origen, destino, monto, moneda);
        System.out.println(ok ? "PAGO APROBADO" : "PAGO RECHAZADO");
    }
}
```

### Qué hace
1. Pide la cotización del día. 2. Pide token. 3. Busca en qué banco está cada cuenta. 4. Convierte el monto a la moneda de cada cuenta. 5. Debita al cliente. 6. Abona al vendedor; **si falla, devuelve el dinero**.

### Cómo defenderlo
- "La plataforma es el **orquestador**; el comercio solo conoce un método RMI."
- "Un banco está en PHP y otro en Java y la plataforma no lo nota: **interoperabilidad** gracias a HTTP y JSON."
- "ATC y los bancos comparten la **clave secreta**: así un banco valida un token que no emitió, sin preguntar a nadie."
- "Si falla el abono hago una **compensación** (devuelvo el débito), porque no hay una transacción única entre dos bancos."
- "El token va en `Authorization: Bearer`; sin él los bancos responden **401**."
- "Limitación honesta: si la plataforma se cayera entre débito y abono quedaría inconsistente; se resolvería con un registro de transacciones o **confirmación en dos fases**."

### Probar los bancos solos (símbolo del sistema)
```bat
curl -X POST http://localhost:8083/api/login -d "{\"user\":\"comercio\",\"password\":\"1234\"}"
curl http://localhost:8085/api/cuentas -H "Authorization: Bearer TOKEN"
curl http://localhost:8001/api/cuentas -H "Accept: application/json" -H "Authorization: Bearer TOKEN"
```

---

## Versión 2 · Además consultar saldo
Cambios sobre la versión 1:
```java
// 1) En IPlataforma.java (la de la plataforma Y la copia del comercio) agregar:
String consultarSaldo(String nrocuenta) throws RemoteException;

// 2) En Plataforma.java agregar este metodo (reutiliza pedir, campo y BANCOS):
@Override
public String consultarSaldo(String nrocuenta) throws RemoteException {
    try {
        String login = pedir("POST", ATC, "{\"user\":\"comercio\",\"password\":\"1234\"}", null).body();
        String token = campo(login, "token");
        for (String banco : BANCOS) {
            HttpResponse<String> r = pedir("GET", banco + "/" + nrocuenta, null, token);
            if (r.statusCode() == 200) return r.body();
        }
        return "cuenta no encontrada";
    } catch (Exception e) {
        return "error: " + e.getMessage();
    }
}

// 3) En SitioComercio.java, donde quieras consultar:
System.out.println(plataforma.consultarSaldo(origen));
```

**Cómo defenderlo:** "Agregar una operación es declararla en la interfaz e implementarla; el cliente necesita la interfaz actualizada. Reinicio servidor y cliente."

---

## Versión 3 · Además historial
```java
// 1) En IPlataforma.java (las dos copias) agregar:
java.util.List<String> historial() throws RemoteException;

// 2) En Plataforma.java agregar el atributo y el metodo:
private final java.util.ArrayList<String> movimientos = new java.util.ArrayList<>();

@Override
public synchronized java.util.List<String> historial() throws RemoteException {
    return new java.util.ArrayList<>(movimientos);
}

// 3) En realizarTransaccion, justo antes del  "return true;" :
movimientos.add(java.time.LocalDateTime.now() + " | " + origen + " -> " + destino + " | " + monto + " " + moneda);

// 4) En SitioComercio.java:
plataforma.historial().forEach(System.out::println);
```

**Cómo defenderlo:** "El historial vive en la memoria del servidor RMI; devuelvo **una copia** para no exponer la lista original. Se pierde al reiniciar; para conservarlo se guarda en archivo o base de datos."

---

## Otras cosas que pueden cambiar
| Cambio | Qué tocar |
|---|---|
| Otro tipo de cambio o por fecha | `procesarSolicitud` del Banco Central |
| Un tercer banco | Agregar su URL al arreglo `BANCOS` |
| Registrar / eliminar cuentas | Ya existen: `POST` y `DELETE` en los dos bancos |
| El login lo debe dar Laravel | [Plantilla 07](../../plantillas/07-jwt-laravel/README.md) y cambiar la URL `ATC` (cuidado: ahí se usa `email`) |
