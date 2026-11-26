# Ejercicio 8 · Cotizaciones del Banco Central (Práctica 5)

[← Ejercicios](../README.md) · [Índice](../../README.md) · **[PRÁCTICA 5]** · probado

Aquí las tecnologías están **al revés** de lo habitual: **SOAP en Java**, **GraphQL en Laravel** y **REST en Node**. Sirve para practicar que el docente puede cambiar qué tecnología va en cada lado.

## Enunciado resumido
Tres servicios que consultan y registran la cotización de una moneda por fecha, con **una misma base de datos**:
- `obtenerCotizacion(fecha)` → fecha, cotización y cotización oficial; si no hay, avisa que no se encontró.
- `registrarCotizacion(fecha, monto)` → la oficial se asigna (6,97); valida datos y **no permite dos cotizaciones de la misma fecha**.

Datos iniciales: del 04/04/2025 al 12/04/2025 con 7,02 · 7,05 · 7,10 · 8,20 · 8,30 · 8,40 · 8,70 · 9,00 · 9,20 (oficial 6,97).

```
 SoapUI / C# / Java ──SOAP──> soap-java :8086 ──REST──┐
 Postman / web ───────REST──> rest-node :3000 ────────┼──> MySQL bcb_cotizaciones (tabla cotizaciones)
 GraphiQL ─────────GraphQL──> graphql-laravel :8000 ──┘    (la crea la migración de Laravel)
```

| Carpeta | Tecnología | Puerto |
|---|---|---|
| `graphql-laravel` | Laravel + webonyx/graphql-php ([plantilla 14](../../plantillas/14-graphql-laravel/README.md)) | 8000 |
| `rest-node` | Node + Express + Sequelize ([plantilla 13](../../plantillas/13-rest-node/README.md)) | 3000 |
| `soap-java` | Java sin librerías ([plantilla 15](../../plantillas/15-soap-java/README.md)) | 8086 |

> **¿Por qué el SOAP llama a la API REST y no a MySQL?** Para JDBC hace falta el jar `mysql-connector-j`, que sin internet no está. Llamando a la API REST se sigue usando **la misma base** y la misma validación. Si en tu PC sí tienes el jar, ver la nota al final.

## Orden de arranque
1. XAMPP → MySQL. phpMyAdmin → crear la base **`bcb_cotizaciones`**.
2. `graphql-laravel` (crea la tabla y los datos) → 3. `rest-node` → 4. `soap-java`.

---

## Paso 1 · GraphQL en Laravel
```bat
C:\guia\00-preparacion\sin-internet\nuevo-laravel.bat graphql-laravel
cd C:\examen\graphql-laravel
```
En `.env`: `DB_DATABASE=bcb_cotizaciones`.
```bash
php artisan install:api
php artisan make:model Cotizacion -m
```
Pega los archivos (la migración va **dentro** de la que creó artisan; crea la carpeta `app/GraphQL`).
```bash
php artisan migrate:fresh --seed
php artisan serve
```
Probar: `http://localhost:8000/graphiql.html`.

#### database/migrations/…_create_cotizaciones_table.php
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('cotizaciones', function (Blueprint $table) {
            $table->id();
            $table->date('fecha')->unique();
            $table->decimal('cotizacion', 8, 2);
            $table->decimal('cotizacion_oficial', 8, 2)->default(6.97);
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('cotizaciones');
    }
};
```

#### app/Models/Cotizacion.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Cotizacion extends Model
{
    protected $table = 'cotizaciones';
    protected $fillable = ['fecha', 'cotizacion', 'cotizacion_oficial'];

    protected function casts(): array
    {
        return ['cotizacion' => 'float', 'cotizacion_oficial' => 'float'];
    }
}
```

#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Cotizacion;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // datos del enunciado (la oficial es 6.97 en todas)
        $datos = [
            '2025-04-04' => 7.02, '2025-04-05' => 7.05, '2025-04-06' => 7.10,
            '2025-04-07' => 8.20, '2025-04-08' => 8.30, '2025-04-09' => 8.40,
            '2025-04-10' => 8.70, '2025-04-11' => 9.00, '2025-04-12' => 9.20,
        ];
        foreach ($datos as $fecha => $valor) {
            Cotizacion::create(['fecha' => $fecha, 'cotizacion' => $valor, 'cotizacion_oficial' => 6.97]);
        }
    }
}
```

#### app/GraphQL/Esquema.php
```php
<?php

namespace App\GraphQL;

use App\Models\Cotizacion;
use GraphQL\Type\Definition\ObjectType;
use GraphQL\Type\Definition\Type;
use GraphQL\Type\Schema;

class Esquema
{
    const OFICIAL = 6.97;

    // fecha AAAA-MM-DD que exista en el calendario
    public static function fechaValida($fecha): bool
    {
        if (!is_string($fecha) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $fecha)) return false;
        [$a, $m, $d] = explode('-', $fecha);
        return checkdate((int) $m, (int) $d, (int) $a);
    }

    public static function crear(): Schema
    {
        $cotizacionType = new ObjectType([
            'name' => 'Cotizacion',
            'fields' => [
                'fecha'              => ['type' => Type::string(), 'resolve' => fn ($c) => substr((string) $c->fecha, 0, 10)],
                'cotizacion'         => Type::float(),
                'cotizacion_oficial' => Type::float(),
            ],
        ]);

        // misma respuesta para consultar y registrar
        $respuestaType = new ObjectType([
            'name' => 'RespuestaCotizacion',
            'fields' => [
                'exito'      => Type::nonNull(Type::boolean()),
                'mensaje'    => Type::nonNull(Type::string()),
                'cotizacion' => $cotizacionType,
            ],
        ]);

        $query = new ObjectType([
            'name' => 'Query',
            'fields' => [
                'obtenerCotizacion' => [
                    'type' => $respuestaType,
                    'args' => ['fecha' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        if (!self::fechaValida($args['fecha'])) {
                            return ['exito' => false, 'mensaje' => 'Fecha invalida, use AAAA-MM-DD', 'cotizacion' => null];
                        }
                        $c = Cotizacion::where('fecha', $args['fecha'])->first();
                        if (!$c) {
                            return ['exito' => false, 'mensaje' => 'No se encontro la cotizacion para ' . $args['fecha'], 'cotizacion' => null];
                        }
                        return ['exito' => true, 'mensaje' => 'Cotizacion encontrada', 'cotizacion' => $c];
                    },
                ],
                'cotizaciones' => [
                    'type' => Type::listOf($cotizacionType),
                    'resolve' => fn () => Cotizacion::orderBy('fecha')->get(),
                ],
            ],
        ]);

        $mutation = new ObjectType([
            'name' => 'Mutation',
            'fields' => [
                'registrarCotizacion' => [
                    'type' => $respuestaType,
                    'args' => [
                        'fecha' => Type::nonNull(Type::string()),
                        'monto' => Type::nonNull(Type::float()),
                    ],
                    'resolve' => function ($root, $args) {
                        if (!self::fechaValida($args['fecha'])) {
                            return ['exito' => false, 'mensaje' => 'Fecha invalida, use AAAA-MM-DD', 'cotizacion' => null];
                        }
                        if ($args['monto'] <= 0) {
                            return ['exito' => false, 'mensaje' => 'El monto debe ser mayor a 0', 'cotizacion' => null];
                        }
                        if (Cotizacion::where('fecha', $args['fecha'])->exists()) {
                            return ['exito' => false, 'mensaje' => 'Ya existe una cotizacion para ' . $args['fecha'], 'cotizacion' => null];
                        }
                        $c = Cotizacion::create([
                            'fecha' => $args['fecha'],
                            'cotizacion' => round($args['monto'], 2),
                            'cotizacion_oficial' => self::OFICIAL,
                        ]);
                        return ['exito' => true, 'mensaje' => 'Cotizacion registrada', 'cotizacion' => $c];
                    },
                ],
            ],
        ]);

        return new Schema(['query' => $query, 'mutation' => $mutation]);
    }
}
```

#### app/Http/Controllers/GraphQLController.php
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

#### routes/api.php
```php
<?php

use App\Http\Controllers\GraphQLController;
use Illuminate\Support\Facades\Route;

// http://localhost:8000/api/graphql
Route::post('/graphql', GraphQLController::class);
```

#### public/graphiql.html
Es el de la [plantilla 14](../../plantillas/14-graphql-laravel/README.md#publicgraphiqlhtml) con otra consulta de ejemplo (está en la carpeta).

```graphql
{ obtenerCotizacion(fecha: "2025-04-04") { exito mensaje cotizacion { fecha cotizacion cotizacion_oficial } } }
mutation { registrarCotizacion(fecha: "2025-04-13", monto: 9.35) { exito mensaje cotizacion { fecha cotizacion } } }
{ cotizaciones { fecha cotizacion } }
```

---

## Paso 2 · REST en Node
```bat
C:\guia\00-preparacion\sin-internet\nuevo-node.bat rest-node
cd C:\examen\rest-node
REM pegar database.js y server.js
npm run dev
```
No usa `sync()`: la tabla ya la creó Laravel.

#### database.js
```js
const { Sequelize } = require('sequelize');

// MISMA base que GraphQL (Laravel); la tabla la crea la migracion de Laravel
const sequelize = new Sequelize('bcb_cotizaciones', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Cotizacion = sequelize.define('Cotizacion', {
  fecha:              { type: Sequelize.DATEONLY, allowNull: false, unique: true },
  cotizacion:         { type: Sequelize.DECIMAL(8, 2), allowNull: false },
  cotizacion_oficial: { type: Sequelize.DECIMAL(8, 2), allowNull: false }
}, {
  tableName: 'cotizaciones',
  timestamps: true,
  createdAt: 'created_at',
  updatedAt: 'updated_at'
});

module.exports = { sequelize, Cotizacion };
```

#### server.js
```js
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
```

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/cotizaciones/2025-04-04` | 200 · 404 no existe · 400 fecha mala |
| GET | `/api/cotizaciones` | 200 lista |
| POST | `/api/cotizaciones` `{"fecha":"2025-04-13","monto":9.35}` | 201 · 409 repetida · 400 datos malos |

```bat
curl http://localhost:3000/api/cotizaciones/2025-04-04
curl -X POST http://localhost:3000/api/cotizaciones -H "Content-Type: application/json" -d "{\"fecha\":\"2025-04-14\",\"monto\":9.4}"
```

---

## Paso 3 · SOAP en Java
Proyecto Java con los dos archivos → ejecutar `ServidorCotizaciones` (Node debe estar encendido).
```bat
cd soap-java
javac -d out *.java
java -cp out ServidorCotizaciones
```
WSDL: `http://localhost:8086/cotizaciones?wsdl` (en SoapUI: *New SOAP Project* con esa URL).

#### ServidorCotizaciones.java
```java
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

// SOAP en Java sin librerias. Los datos los pide a la API REST de Node (misma base de datos).
public class ServidorCotizaciones {

    static final int PUERTO = 8086;
    static final String RUTA = "/cotizaciones";
    static final String NS = "http://tempuri.org/";
    static final String REST = "http://localhost:3000/api/cotizaciones";

    // ================== OPERACIONES ==================
    static String operar(String op, Map<String, String> p) throws Exception {
        if (op.equals("obtenerCotizacion")) {
            HttpResponse<String> r = pedir("GET", REST + "/" + p.getOrDefault("fecha", "").trim(), null);
            return "<obtenerCotizacionResult>" + respuesta(r.body()) + "</obtenerCotizacionResult>";
        }
        if (op.equals("registrarCotizacion")) {
            String monto = p.getOrDefault("monto", "").trim().replace(',', '.');
            String json = "{\"fecha\":\"" + p.getOrDefault("fecha", "").trim() + "\",\"monto\":\"" + monto + "\"}";
            HttpResponse<String> r = pedir("POST", REST, json);
            return "<registrarCotizacionResult>" + respuesta(r.body()) + "</registrarCotizacionResult>";
        }
        throw new IllegalArgumentException("Operacion desconocida " + op);
    }

    // convierte el JSON de la API REST en el XML de la respuesta
    static String respuesta(String json) {
        String xml = "<exito>" + "true".equals(campo(json, "exito")) + "</exito><mensaje>" + esc(campo(json, "mensaje")) + "</mensaje>";
        if (json.contains("\"cotizacion\":{")) {
            xml += "<cotizacion><fecha>" + campo(json, "fecha") + "</fecha><cotizacion>" + campo(json, "cotizacion")
                    + "</cotizacion><cotizacionOficial>" + campo(json, "cotizacion_oficial") + "</cotizacionOficial></cotizacion>";
        }
        return xml;
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        server.createContext(RUTA, ServidorCotizaciones::atender);
        server.start();
        System.out.println("SOAP en http://localhost:" + PUERTO + RUTA + "   WSDL: http://localhost:" + PUERTO + RUTA + "?wsdl");
    }

    static void atender(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("GET")) {
            responder(ex, 200, Wsdl.texto("http://localhost:" + PUERTO + RUTA));
            return;
        }
        try {
            Document doc = DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder().parse(ex.getRequestBody());
            Element body = (Element) doc.getElementsByTagNameNS("*", "Body").item(0);
            Element operacion = null;
            for (Node n = body.getFirstChild(); n != null && operacion == null; n = n.getNextSibling()) {
                if (n instanceof Element) operacion = (Element) n;
            }
            Map<String, String> params = new HashMap<>();
            for (Node n = operacion.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) params.put(n.getLocalName(), n.getTextContent());
            }
            String op = operacion.getLocalName();
            System.out.println("Llego " + op + " " + params);
            responder(ex, 200, sobre("<" + op + "Response xmlns=\"" + NS + "\">" + operar(op, params) + "</" + op + "Response>"));
        } catch (java.net.ConnectException e) {
            responder(ex, 500, sobre("<soap:Fault><faultcode>soap:Server</faultcode><faultstring>La API REST no responde</faultstring></soap:Fault>"));
        } catch (Exception e) {
            responder(ex, 500, sobre("<soap:Fault><faultcode>soap:Client</faultcode><faultstring>" + esc(String.valueOf(e.getMessage())) + "</faultstring></soap:Fault>"));
        }
    }

    static HttpResponse<String> pedir(String metodo, String url, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json").header("Content-Type", "application/json");
        b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*(\"([^\"]*)\"|[-0-9.]+|true|false)").matcher(json);
        if (!m.find()) return "";
        return m.group(2) != null ? m.group(2) : m.group(1);
    }

    static String sobre(String contenido) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + contenido + "</soap:Body></soap:Envelope>";
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void responder(HttpExchange ex, int codigo, String xml) throws IOException {
        byte[] b = xml.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
        ex.sendResponseHeaders(codigo, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }
}
```

#### Wsdl.java
```java
// Contrato WSDL del servicio de cotizaciones (escrito a mano, como en la plantilla 15)
public class Wsdl {

    static String texto(String url) {
        return """
<?xml version="1.0" encoding="utf-8"?>
<wsdl:definitions xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:tns="http://tempuri.org/"
    xmlns:s="http://www.w3.org/2001/XMLSchema" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    targetNamespace="http://tempuri.org/">
  <wsdl:types>
    <s:schema elementFormDefault="qualified" targetNamespace="http://tempuri.org/">
      <s:complexType name="Cotizacion">
        <s:sequence>
          <s:element minOccurs="0" maxOccurs="1" name="fecha" type="s:string"/>
          <s:element minOccurs="1" maxOccurs="1" name="cotizacion" type="s:double"/>
          <s:element minOccurs="1" maxOccurs="1" name="cotizacionOficial" type="s:double"/>
        </s:sequence>
      </s:complexType>
      <s:complexType name="Respuesta">
        <s:sequence>
          <s:element minOccurs="1" maxOccurs="1" name="exito" type="s:boolean"/>
          <s:element minOccurs="0" maxOccurs="1" name="mensaje" type="s:string"/>
          <s:element minOccurs="0" maxOccurs="1" name="cotizacion" type="tns:Cotizacion"/>
        </s:sequence>
      </s:complexType>
      <s:element name="obtenerCotizacion"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="fecha" type="s:string"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="obtenerCotizacionResponse"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="obtenerCotizacionResult" type="tns:Respuesta"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="registrarCotizacion"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="fecha" type="s:string"/>
        <s:element minOccurs="0" maxOccurs="1" name="monto" type="s:string"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="registrarCotizacionResponse"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="registrarCotizacionResult" type="tns:Respuesta"/>
      </s:sequence></s:complexType></s:element>
    </s:schema>
  </wsdl:types>
  <wsdl:message name="obtenerCotizacionSoapIn"><wsdl:part name="parameters" element="tns:obtenerCotizacion"/></wsdl:message>
  <wsdl:message name="obtenerCotizacionSoapOut"><wsdl:part name="parameters" element="tns:obtenerCotizacionResponse"/></wsdl:message>
  <wsdl:message name="registrarCotizacionSoapIn"><wsdl:part name="parameters" element="tns:registrarCotizacion"/></wsdl:message>
  <wsdl:message name="registrarCotizacionSoapOut"><wsdl:part name="parameters" element="tns:registrarCotizacionResponse"/></wsdl:message>
  <wsdl:portType name="CotizacionesSoap">
    <wsdl:operation name="obtenerCotizacion"><wsdl:input message="tns:obtenerCotizacionSoapIn"/><wsdl:output message="tns:obtenerCotizacionSoapOut"/></wsdl:operation>
    <wsdl:operation name="registrarCotizacion"><wsdl:input message="tns:registrarCotizacionSoapIn"/><wsdl:output message="tns:registrarCotizacionSoapOut"/></wsdl:operation>
  </wsdl:portType>
  <wsdl:binding name="CotizacionesSoap" type="tns:CotizacionesSoap">
    <soap:binding transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="obtenerCotizacion"><soap:operation soapAction="http://tempuri.org/obtenerCotizacion" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
    <wsdl:operation name="registrarCotizacion"><soap:operation soapAction="http://tempuri.org/registrarCotizacion" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
  </wsdl:binding>
  <wsdl:service name="Cotizaciones">
    <wsdl:port name="CotizacionesSoap" binding="tns:CotizacionesSoap">
      <soap:address location="URL"/>
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>
""".replace("URL", url);
    }
}
```

Probar con curl:
```bat
curl -X POST http://localhost:8086/cotizaciones -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"http://tempuri.org/obtenerCotizacion\"" -d "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><obtenerCotizacion xmlns=\"http://tempuri.org/\"><fecha>2025-04-04</fecha></obtenerCotizacion></soap:Body></soap:Envelope>"
```
Desde C#: referencia de servicio a `?wsdl` → `new srCotizaciones.CotizacionesSoapClient().obtenerCotizacion("2025-04-04")`.

---

## Pruebas obligatorias del enunciado (resultado real)
| Prueba | GraphQL (Laravel) | REST (Node) | SOAP (Java) |
|---|---|---|---|
| Consultar 04/04/2025 | `exito: true`, 7.02 / 6.97 | 200, 7.02 / 6.97 | `exito: true`, 7.02 / 6.97 |
| Consultar 12/04/2025 | 9.2 | 200, 9.2 | 9.2 |
| Fecha que no existe | `exito: false` "No se encontro…" | **404** | `exito: false` "No se encontro…" |
| Registrar nueva | "Cotizacion registrada" | **201** | "Cotizacion registrada" |
| Registrar fecha repetida | "Ya existe…" | **409** | "Ya existe…" |
| Consultar después de registrar | la encuentra (aunque se haya registrado por otro servicio) | igual | igual |
| Datos inválidos (`2025-13-01`, monto `x` o negativo) | "Fecha invalida…" / "El monto debe ser mayor a 0" | **400** | mismos mensajes |
| API REST apagada | — | — | **SOAP Fault** "La API REST no responde" |

## Comparación SOAP, REST y GraphQL (entregable de la práctica)
| | SOAP (Java) | REST (Node) | GraphQL (Laravel) |
|---|---|---|---|
| Cómo se pide | `POST /cotizaciones` con un sobre XML y `SOAPAction` | URL por recurso + verbo: `GET /api/cotizaciones/2025-04-04` | `POST /api/graphql` con `{"query": ...}` |
| Cómo se envían datos | Parámetros dentro del XML (`<fecha>`, `<monto>`) | JSON en el cuerpo del POST | Argumentos de la mutation |
| Respuesta | XML con tipos del WSDL | JSON, el código HTTP dice el resultado | JSON con la forma exacta de la consulta |
| Errores | SOAP Fault | 400 / 404 / 409 | Campo `exito` o `errors`, siempre HTTP 200 |
| Contrato | WSDL (obligatorio) | Opcional | Esquema (obligatorio) |

## Si tienes `mysql-connector-j` (SOAP con JDBC)
Agrega el jar al proyecto y en `operar` reemplaza la llamada REST por SQL:
```java
Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bcb_cotizaciones", "root", "");
PreparedStatement ps = con.prepareStatement("SELECT fecha, cotizacion, cotizacion_oficial FROM cotizaciones WHERE fecha = ?");
ps.setString(1, fecha);
ResultSet rs = ps.executeQuery();
```

## Cómo defenderlo
- "Los tres servicios usan **la misma base y las mismas reglas**: lo que registro por uno lo encuentro por los otros."
- "REST usa los **códigos HTTP** (201, 404, 409, 400); GraphQL y SOAP devuelven `exito` y `mensaje` dentro de la respuesta."
- "La fecha es única en la tabla (`unique`) y además lo valido antes de insertar."
- "El SOAP es un **adaptador**: traduce XML a una llamada REST y la respuesta JSON otra vez a XML."
