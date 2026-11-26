# Ejercicio 4 · Multas de tránsito (parcial anterior)

[← Ejercicios](../README.md) · [Índice](../../README.md)

| Versión | Me piden… |
|---|---|
| [1](#versión-1--sistema-completo) | Registrar, consultar y pagar |
| [2](#versión-2--además-anular-y-ver-estado-por-socket) | Además anular y ver estado |
| [3](#versión-3--total-adeudado) | Total que debe un conductor |
| [4](#versión-4--mismo-sistema-otro-tema) | Mismo sistema, otro tema |

## Enunciado resumido
La **Policía** (escritorio) registra multas, pero antes verifica el CI en el **SEGIP (SOAP)**. La **Central** guarda las multas y las expone por **REST**, **GraphQL** y un **socket TCP** de pagos. La **Entidad Recaudadora** consulta por GraphQL y paga por socket. El **ciudadano** consulta en una página web.

```
 Policía (Forms) ──SOAP verificarCI──> SEGIP
        └──────── POST /api/multas ──> Central REST (Laravel :8000) ──┐
 Web ciudadano ── GET ?ci= ──────────>        ▲                        ├─ MySQL bd_multas
 Recaudadora ── GraphQL ─────────────> Central GraphQL (:4000) ───────┘
        └────── "PAGAR:codigo:ci" ───> Central TCP (:5002) ──REST──> Central REST
```

## Proyectos

| Carpeta | Tecnología | Puerto |
|---|---|---|
| `central-rest` | Laravel + MySQL | 8000 |
| `central-graphql` | Node | 4000 |
| `central-tcp` | Java | 5002 |
| `segip-soap` | C# ASMX | lo da Visual Studio |
| `recaudadora-java` | Java (cliente) | — |
| `cliente-web` | HTML + JS | — |
| `policia-forms` | C# Windows Forms | — |

## Orden de arranque
1. `central-rest` (migrar y `serve`) → 2. `central-graphql` → 3. `central-tcp` → 4. `segip-soap` → 5. los clientes.

## Datos de prueba
| Boleta | CI | Monto | Estado inicial |
|---|---|---|---|
| B-001 | 11021654 | 200 | pendiente |
| B-002 | 5551234 | 50 | anulada |

CI que existen en SEGIP: `11021654`, `5551234`, `7778899`.

---

## Versión 1 · Sistema completo

### Paso 1 · Central REST
1. Crea la base `bd_multas` y [un proyecto Laravel](../../00-preparacion/README.md#d2-proyecto-laravel-nuevo) con `DB_DATABASE=bd_multas`.
2.
```bash
php artisan install:api
php artisan make:model Multa -msfc --api
```
3. Pega los archivos.
4.
```bash
php artisan migrate:fresh --seed
php artisan serve
```
5. Comprueba: `http://localhost:8000/api/multas`

#### Migración (database/migrations/…_create_multas_table.php)
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('multas', function (Blueprint $table) {
            $table->id();
            $table->string('codigo', 20)->unique();        // codigo unico de boleta
            $table->string('ci', 15);
            $table->string('nombre', 100);                 // conductor
            $table->date('fecha');
            $table->string('tipo', 60);
            $table->string('lugar', 100);
            $table->decimal('monto', 10, 2);
            $table->string('estado', 20)->default('pendiente');  // pendiente | pagada | anulada
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('multas');
    }
};
```
#### app/Models/Multa.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Multa extends Model
{
    use HasFactory;

    protected $table = 'multas';
    protected $fillable = ['codigo', 'ci', 'nombre', 'fecha', 'tipo', 'lugar', 'monto', 'estado'];

    // /api/multas/B-001 busca por codigo de boleta y no por id
    public function getRouteKeyName(): string
    {
        return 'codigo';
    }
}
```
#### app/Http/Controllers/MultaController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Multa;
use Illuminate\Http\Request;

class MultaController extends Controller
{
    // GET /api/multas        GET /api/multas?ci=11021654
    public function index(Request $request)
    {
        $consulta = Multa::query();
        if ($request->filled('ci')) {
            $consulta->where('ci', $request->query('ci'));
        }
        return response()->json($consulta->get());
    }

    // POST /api/multas   (lo usa la Policia)
    public function store(Request $request)
    {
        $datos = $request->validate([
            'codigo' => 'required|string',
            'ci'     => 'required|digits_between:5,10',
            'nombre' => 'required|string',
            'fecha'  => 'required|date',
            'tipo'   => 'required|string',
            'lugar'  => 'required|string',
            'monto'  => 'required|numeric|gt:0',
        ]);

        if (Multa::where('codigo', $datos['codigo'])->exists()) {
            return response()->json(['error' => 'Ya existe una boleta con ese codigo'], 409);
        }

        // Toda multa nace pendiente (no se acepta el estado desde afuera)
        $multa = Multa::create($datos);
        return response()->json($multa->refresh(), 201);
    }

    // GET /api/multas/{codigo}
    public function show(Multa $multa)
    {
        return response()->json($multa);
    }

    // PATCH /api/multas/{codigo}   { "estado": "pagada" | "anulada" }
    public function update(Request $request, Multa $multa)
    {
        $datos = $request->validate(['estado' => 'required|in:pagada,anulada']);

        // Solo una multa pendiente puede cambiar
        if ($multa->estado !== 'pendiente') {
            return response()->json(['error' => 'La multa ya esta ' . $multa->estado], 409);
        }

        $multa->update($datos);
        return response()->json($multa);
    }

    // DELETE = anular (borrado logico)
    public function destroy(Multa $multa)
    {
        if ($multa->estado !== 'pendiente') {
            return response()->json(['error' => 'La multa ya esta ' . $multa->estado], 409);
        }
        $multa->update(['estado' => 'anulada']);
        return response()->noContent();
    }
}
```
#### routes/api.php
```php
<?php

use App\Http\Controllers\MultaController;
use Illuminate\Support\Facades\Route;

// parameters() evita que Laravel genere {multum}
Route::apiResource('multas', MultaController::class)
    ->parameters(['multas' => 'multa']);
```
#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Multa;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Multa::create(['codigo' => 'B-001', 'ci' => '11021654', 'nombre' => 'Juan Perez', 'fecha' => '2026-06-01',
            'tipo' => 'Exceso de velocidad', 'lugar' => 'Av. Principal', 'monto' => 200]);
        Multa::create(['codigo' => 'B-002', 'ci' => '5551234', 'nombre' => 'Ana Rojas', 'fecha' => '2026-06-02',
            'tipo' => 'Mal estacionado', 'lugar' => 'Plaza central', 'monto' => 50, 'estado' => 'anulada']);
    }
}
```

### Paso 2 · Central GraphQL
Copia tu [base de Node](../../00-preparacion/README.md#d3-proyecto-node-graphql-nuevo), pega estos archivos y `npm run dev`.

#### database.js
```javascript
const { Sequelize } = require('sequelize');

// MISMA base de datos que Laravel: asi REST, GraphQL y TCP ven lo mismo
const sequelize = new Sequelize('bd_multas', 'root', '', {
  host: 'localhost',
  dialect: 'mysql',
  logging: false
});

const Multa = sequelize.define('Multa', {
  codigo: { type: Sequelize.STRING },
  ci:     { type: Sequelize.STRING },
  nombre: { type: Sequelize.STRING },
  fecha:  { type: Sequelize.DATEONLY },
  tipo:   { type: Sequelize.STRING },
  lugar:  { type: Sequelize.STRING },
  monto:  { type: Sequelize.FLOAT },
  estado: { type: Sequelize.STRING }
}, {
  tableName: 'multas',     // la tabla que creo la migracion de Laravel
  timestamps: false
});

module.exports = { sequelize, Multa };
```
#### schema.js
```javascript
const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Multa } = require('./database');

const MultaType = new GraphQLObjectType({
  name: 'Multa',
  fields: {
    codigo: { type: GraphQLString },
    ci:     { type: GraphQLString },
    nombre: { type: GraphQLString },
    fecha:  { type: GraphQLString },
    tipo:   { type: GraphQLString },
    lugar:  { type: GraphQLString },
    monto:  { type: GraphQLFloat },
    estado: { type: GraphQLString }
  }
});

// Respuesta de la verificacion que pide la Entidad Recaudadora
const VerificacionType = new GraphQLObjectType({
  name: 'Verificacion',
  fields: {
    sePuedePagar: { type: GraphQLBoolean },
    mensaje:      { type: GraphQLString },
    multa:        { type: MultaType }
  }
});

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    multas: {
      type: new GraphQLList(MultaType),
      args: { ci: { type: GraphQLString } },           // opcional
      resolve(parent, args) {
        return Multa.findAll(args.ci ? { where: { ci: args.ci } } : {});
      }
    },
    verificarMulta: {
      type: VerificacionType,
      args: {
        codigo: { type: new GraphQLNonNull(GraphQLString) },
        ci:     { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, { codigo, ci }) {
        const multa = await Multa.findOne({ where: { codigo } });
        // 1) existe  2) es de ese conductor  3) no esta pagada ni anulada
        if (!multa) return { sePuedePagar: false, mensaje: 'La multa no existe' };
        if (multa.ci !== ci) return { sePuedePagar: false, mensaje: 'No fue emitida a ese conductor' };
        if (multa.estado !== 'pendiente') {
          return { sePuedePagar: false, mensaje: 'La multa esta ' + multa.estado, multa };
        }
        return { sePuedePagar: true, mensaje: 'Pendiente de pago', multa };
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery });
```
#### server.js
```javascript
const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const schema = require('./schema');
const { sequelize } = require('./database');

const app = express();

app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

app.use('/graphql', graphqlHTTP({ schema, graphiql: true }));

// Sin sync(): la tabla la crea Laravel con "php artisan migrate:fresh --seed"
sequelize.authenticate()
  .then(() => app.listen(4000, () => console.log('GraphQL en http://localhost:4000/graphql')))
  .catch(err => console.error('No pude conectar a la base de datos:', err.message));
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

Prueba en `http://localhost:4000/graphql`:
```graphql
{ verificarMulta(codigo: "B-001", ci: "11021654") { sePuedePagar mensaje multa { monto estado } } }
```

### Paso 3 · Central TCP (pagos)
Proyecto Java → ejecuta `ServidorPagoMultas`.
#### ServidorPagoMultas.java
```java
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.regex.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorPagoMultas {

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

    static final String REST = "http://localhost:8000/api/multas/";

    // PAGAR:codigo:ci   ->  OK:multa pagada  |  ERROR:motivo
    // synchronized: la misma multa no se puede cobrar dos veces a la vez
    static synchronized String procesarSolicitud(String mensaje) {
        try {
            String[] p = mensaje.split(":");
            if (p.length != 3 || !p[0].equals("PAGAR")) return "ERROR:use PAGAR:codigo:ci";

            HttpResponse<String> r = pedir("GET", REST + URLEncoder.encode(p[1], "UTF-8"), null);
            if (r.statusCode() == 404) return "ERROR:la multa no existe";
            if (r.statusCode() != 200) return "ERROR:la central respondio " + r.statusCode();

            if (!p[2].equals(campo(r.body(), "ci"))) return "ERROR:la multa no es de ese conductor";

            String estado = campo(r.body(), "estado");
            if (!"pendiente".equals(estado)) return "ERROR:la multa esta " + estado;

            r = pedir("PATCH", REST + URLEncoder.encode(p[1], "UTF-8"), "{\"estado\":\"pagada\"}");
            return r.statusCode() == 200 ? "OK:multa pagada" : "ERROR:no se pudo registrar el pago";

        } catch (Exception e) {
            return "ERROR:la central REST no responde";
        }
    }

    static HttpResponse<String> pedir(String metodo, String url, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                                             : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
```

### Paso 4 · SEGIP (SOAP)
Crea el servicio como en la [plantilla 05](../../plantillas/05-soap-csharp/README.md), pega el código y `Ctrl + F5`.
#### Servicio.asmx
```aspx
<%@ WebService Language="C#" CodeBehind="~/App_Code/Servicio.cs" Class="Servicio" %>
```
#### App_Code/Servicio.cs
```csharp
using System.Collections.Generic;
using System.Web.Services;

// SEGIP: responde si un CI existe.
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class Servicio : System.Web.Services.WebService
{
    private static readonly List<string> carnets = new List<string> { "11021654", "5551234", "7778899" };

    [WebMethod]
    public bool verificarCI(string ci)
    {
        return ci != null && carnets.Contains(ci.Trim());
    }
}
```

### Paso 5 · Recaudadora (cliente Java)
Dos clases: `Recaudadora` y `ClienteTCP`.
#### Recaudadora.java
```java
import java.net.URI;
import java.net.http.*;
import java.util.Scanner;

// Entidad Recaudadora: 1) consulta por GraphQL   2) si se puede, paga por socket TCP
public class Recaudadora {

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Codigo de boleta: ");
        String codigo = sc.nextLine().trim().replaceAll("[\"\\\\]", "");
        System.out.print("CI del conductor: ");
        String ci = sc.nextLine().trim().replaceAll("[\"\\\\]", "");

        // La consulta GraphQL viaja dentro de un JSON: {"query":"..."}
        String json = "{\"query\":\"{ verificarMulta(codigo:\\\"" + codigo + "\\\", ci:\\\"" + ci
                + "\\\") { sePuedePagar mensaje multa { monto estado } } }\"}";

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:4000/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        String respuesta = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString()).body();
        System.out.println("Consulta: " + respuesta);

        if (!respuesta.contains("\"sePuedePagar\":true")) {
            System.out.println("No se puede pagar.");
            return;
        }

        System.out.print("Pagar? (s/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println(ClienteTCP.enviar("localhost", 5002, "PAGAR:" + codigo + ":" + ci));
        }
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

Prueba: boleta `B-001`, CI `11021654`, pagar `s` → `OK:multa pagada`. Repite: ahora dice que ya está pagada.

### Paso 6 · Cliente web (ciudadano)
Guarda como `index.html` y ábrelo con doble clic.
```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Consulta de multas</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 700px; margin: 20px auto; }
    input, button { padding: 6px; margin: 4px 0; }
    table { border-collapse: collapse; width: 100%; }
    td, th { border: 1px solid #999; padding: 6px; }
  </style>
</head>
<body>
  <h2>Consulta de multas</h2>
  <input id="ci" placeholder="CI del conductor" value="11021654">
  <button onclick="consultar()">Consultar</button>
  <p id="mensaje"></p>
  <table>
    <thead><tr><th>Boleta</th><th>Fecha</th><th>Tipo</th><th>Monto</th><th>Estado</th></tr></thead>
    <tbody id="filas"></tbody>
  </table>

  <script>
    const API = 'http://localhost:8000/api/multas';

    async function consultar() {
      const ci = document.getElementById('ci').value.trim();
      const mensaje = document.getElementById('mensaje');
      const filas = document.getElementById('filas');
      filas.innerHTML = '';
      if (!ci) { mensaje.textContent = 'Escribe un CI'; return; }

      try {
        const r = await fetch(API + '?ci=' + encodeURIComponent(ci), { headers: { Accept: 'application/json' } });
        if (!r.ok) { mensaje.textContent = 'Error HTTP ' + r.status; return; }
        const multas = await r.json();
        mensaje.textContent = multas.length ? multas.length + ' multa(s)' : 'No tiene multas registradas';
        for (const m of multas) {
          const tr = filas.insertRow();
          // textContent: los datos se muestran como texto, nunca como HTML
          for (const valor of [m.codigo, m.fecha, m.tipo, m.monto, m.estado]) {
            tr.insertCell().textContent = valor;
          }
        }
      } catch (e) {
        mensaje.textContent = 'No se pudo conectar con la Central: ' + e.message;
      }
    }
  </script>
</body>
</html>
```

### Paso 7 · Policía (Windows Forms)
1. [Crea el proyecto](../../plantillas/11-cliente-forms-csharp/README.md) (.NET Framework) y agrega la referencia `System.Web.Extensions`.
2. Con el SEGIP encendido: *Agregar → Referencia de servicio* → `http://localhost:PUERTO/Servicio.asmx?WSDL` → nombre **`srSegip`**.
3. *Ver código* de `Form1.cs` y pega.

```csharp
// POLICIA: 1) verifica el CI en SEGIP (SOAP)   2) registra la boleta en la Central (REST)
// ANTES DE PEGAR:
//   - Agregar referencia de servicio al SEGIP con el nombre  srSegip
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
        const string REST = "http://localhost:8000/api/multas";

        readonly Dictionary<string, TextBox> cajas = new Dictionary<string, TextBox>();
        TextBox salida;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Policia - Registro de multas";
            ClientSize = new Size(460, 430);

            string[] campos = { "codigo", "ci", "nombre", "fecha", "tipo", "lugar", "monto" };
            int y = 10;
            foreach (string c in campos)
            {
                Controls.Add(new Label { Text = c, Location = new Point(10, y + 3), AutoSize = true });
                cajas[c] = new TextBox { Location = new Point(90, y), Width = 250 };
                Controls.Add(cajas[c]);
                y += 28;
            }
            cajas["fecha"].Text = DateTime.Today.ToString("yyyy-MM-dd");

            var boton = new Button { Text = "Registrar multa", Location = new Point(90, y), Width = 150 };
            boton.Click += Registrar;
            Controls.Add(boton);

            salida = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical,
                                   Location = new Point(10, y + 40), Size = new Size(440, 170) };
            Controls.Add(salida);
        }

        void Registrar(object sender, EventArgs e)
        {
            try
            {
                // 1) SOAP: el CI existe?
                var segip = new srSegip.ServicioSoapClient();
                if (!segip.verificarCI(cajas["ci"].Text.Trim()))
                {
                    salida.Text = "SEGIP: el CI no existe. No se registra.";
                    return;
                }

                // 2) REST: registrar la boleta
                var datos = new Dictionary<string, string>();
                foreach (var par in cajas) datos[par.Key] = par.Value.Text.Trim();

                var web = new WebClient { Encoding = Encoding.UTF8 };
                web.Headers["Accept"] = "application/json";
                web.Headers["Content-Type"] = "application/json";
                salida.Text = "Registrada:\r\n" +
                    web.UploadString(REST, "POST", new JavaScriptSerializer().Serialize(datos));
            }
            catch (WebException ex)
            {
                // 409 = codigo repetido, 422 = datos mal llenados
                var r = ex.Response as HttpWebResponse;
                salida.Text = r == null ? "La Central no responde: " + ex.Message
                    : "Error " + (int)r.StatusCode + "\r\n" + new StreamReader(r.GetResponseStream()).ReadToEnd();
            }
            catch (Exception ex)
            {
                salida.Text = "Error: " + ex.Message;
            }
        }
    }
}
```

Prueba: código `B-003`, CI `11021654`, completa los campos → "Registrada". Con CI `999999` → "el CI no existe". Repite `B-003` → error 409.

### Cómo defenderlo
- "Cada tecnología está donde el enunciado la pide: **SOAP** para el SEGIP (contrato formal), **REST** para registrar y listar, **GraphQL** para que la recaudadora pida justo los campos que necesita, **TCP** para el pago."
- "REST y GraphQL leen **la misma base de datos**, así no hay datos desincronizados. La tabla la crea Laravel; por eso en Node **no** uso `sync()`."
- "El servidor TCP **no toca la base**: pasa por la API REST, donde están las reglas."
- "Las reglas: código único (409), solo una multa **pendiente** puede pagarse o anularse, y se verifica que sea de ese conductor."
- "`DELETE` es **borrado lógico**: la multa queda como anulada para auditoría."
- "El método del socket es `synchronized` para que la misma multa no se cobre dos veces."

---

## Versión 2 · Además anular y ver estado por socket
Reemplaza un método en `ServidorPagoMultas.java`:
```java
// En ServidorPagoMultas.java REEMPLAZAR el metodo procesarSolicitud por este.
// PAGAR:codigo:ci   ANULAR:codigo:ci   ESTADO:codigo:ci
static synchronized String procesarSolicitud(String mensaje) {
    try {
        String[] p = mensaje.split(":");
        if (p.length != 3) return "ERROR:use OPERACION:codigo:ci";
        String url = REST + URLEncoder.encode(p[1], "UTF-8");

        HttpResponse<String> r = pedir("GET", url, null);
        if (r.statusCode() != 200) return "ERROR:la multa no existe";
        if (!p[2].equals(campo(r.body(), "ci"))) return "ERROR:la multa no es de ese conductor";
        String estado = campo(r.body(), "estado");

        if (p[0].equals("ESTADO")) return "OK:" + estado;

        String nuevo = p[0].equals("PAGAR") ? "pagada" : p[0].equals("ANULAR") ? "anulada" : null;
        if (nuevo == null) return "ERROR:operacion desconocida";
        if (!"pendiente".equals(estado)) return "ERROR:la multa esta " + estado;

        r = pedir("PATCH", url, "{\"estado\":\"" + nuevo + "\"}");
        return r.statusCode() == 200 ? "OK:multa " + nuevo : "ERROR:no se pudo registrar";

    } catch (Exception e) {
        return "ERROR:la central REST no responde";
    }
}
```

Prueba con el cliente TCP de la plantilla: `ESTADO:B-001:11021654`, `ANULAR:B-001:11021654`. (Si ya la pagaste, corre `php artisan migrate:fresh --seed`.)

**Cómo defenderlo:** "Extendí el protocolo con la primera palabra como operación; las validaciones son comunes y solo cambia el estado destino."

---

## Versión 3 · Total adeudado
```javascript
// En schema.js, dentro de "fields" de RootQuery, agregar (despues de verificarMulta, con una coma):
totalPendiente: {
  type: GraphQLFloat,
  args: { ci: { type: new GraphQLNonNull(GraphQLString) } },
  async resolve(parent, { ci }) {
    const total = await Multa.sum('monto', { where: { ci, estado: 'pendiente' } });
    return total || 0;
  }
},
```

```graphql
{ totalPendiente(ci: "11021654") }
```
**Cómo defenderlo:** "Es una query nueva; la suma la hace la base de datos con `SUM`, no traigo todas las filas."

---

## Versión 4 · Mismo sistema, otro tema

| Multas | Biblioteca | Universidad | Servicios básicos |
|---|---|---|---|
| multa | préstamo | matrícula | factura |
| CI conductor | CI lector | CI estudiante | CI cliente |
| pendiente/pagada/anulada | prestado/devuelto/perdido | pendiente/pagada/anulada | pendiente/pagada |
| Policía | Bibliotecario | Kardex | Empresa |

Cambia en orden: **migración → modelo → controlador → schema → mensajes del socket → pantallas**.
