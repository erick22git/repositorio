# Ejercicio 6 · Central de pagos de servicios

[← Ejercicios](../README.md) · [Índice](../../README.md)

| Versión | Me piden… |
|---|---|
| [1](#versión-1--consultar-y-pagar) | Consultar deudas y pagar una |
| [2](#versión-2--pagar-todo) | Pagar todas |
| [3](#versión-3--otra-empresa-y-total) | Una empresa más y total |
| [4](#versión-4--venta-de-pasajes-u-otro-tema) | Otro tema (pasajes) |

## Enunciado resumido
El cliente de escritorio habla **solo SOAP** con una central. La central verifica el CI en el **SEGIP (GraphQL)** y consulta/paga facturas en dos empresas **REST**.

```
 Forms ──SOAP──> Central de Pagos ──GraphQL──> SEGIP (Node :4000)
                                  ├─REST─────> CESSA  (Laravel :8001)
                                  └─REST─────> ELAPAS (Laravel :8002)
```

| Carpeta | Tecnología | Puerto | Base |
|---|---|---|---|
| `segip-graphql` | Node, **sin** base | 4000 | — |
| `empresa-rest-laravel` (se usa **dos veces**) | Laravel | 8001 y 8002 | `bd_cessa`, `bd_elapas` |
| `servidor-pagos-soap` | C# ASMX | lo da Visual Studio | — |
| `cliente-forms` | C# Forms | — | — |

## Orden de arranque
SEGIP → CESSA → ELAPAS → SOAP → Forms.

---

## Versión 1 · Consultar y pagar

### Paso 1 · SEGIP (GraphQL)
Copia tu base Node, pega y `npm run dev`.
#### schema.js
```javascript
const { GraphQLObjectType, GraphQLString, GraphQLSchema, GraphQLNonNull } = require('graphql');

// SIN base de datos: los datos viven en un arreglo
const personas = [
  { ci: '11021654', nombre: 'Juan Perez' },
  { ci: '5551234',  nombre: 'Ana Rojas' }
];

const PersonaType = new GraphQLObjectType({
  name: 'Persona',
  fields: {
    ci:     { type: GraphQLString },
    nombre: { type: GraphQLString }
  }
});

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    persona: {
      type: PersonaType,
      args: { ci: { type: new GraphQLNonNull(GraphQLString) } },
      resolve: (parent, { ci }) => personas.find(p => p.ci === ci) || null
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

const app = express();
app.use('/graphql', graphqlHTTP({ schema, graphiql: true }));
app.listen(4000, () => console.log('SEGIP (GraphQL) en http://localhost:4000/graphql'));
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

### Paso 2 · Empresa CESSA
Proyecto Laravel `cessa`, `.env`: `DB_DATABASE=bd_cessa`.
```bash
php artisan install:api
php artisan make:model Factura -msfc --api
```
Pega los archivos y:
```bash
php artisan migrate:fresh --seed
php artisan serve --port=8001
```
#### Migración (database/migrations/…_create_facturas_table.php)
```php
<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('facturas', function (Blueprint $table) {
            $table->id();
            $table->string('ci', 15);
            $table->string('periodo', 7);                        // 2026-10
            $table->decimal('monto', 10, 2);
            $table->string('estado', 20)->default('pendiente');  // pendiente | pagada
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('facturas');
    }
};
```
#### app/Models/Factura.php
```php
<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Factura extends Model
{
    use HasFactory;

    protected $table = 'facturas';
    protected $fillable = ['ci', 'periodo', 'monto', 'estado'];
    protected $casts = ['monto' => 'float'];
}
```
#### app/Http/Controllers/FacturaController.php
```php
<?php

namespace App\Http\Controllers;

use App\Models\Factura;
use Illuminate\Http\Request;

class FacturaController extends Controller
{
    // GET /api/facturas?ci=11021654   -> solo las pendientes de ese CI
    public function index(Request $request)
    {
        $consulta = Factura::where('estado', 'pendiente');
        if ($request->filled('ci')) {
            $consulta->where('ci', $request->query('ci'));
        }
        return response()->json($consulta->get());
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'ci'      => 'required|digits_between:5,10',
            'periodo' => 'required|string',
            'monto'   => 'required|numeric|gt:0',
        ]);
        return response()->json(Factura::create($datos)->refresh(), 201);
    }

    public function show(Factura $factura)
    {
        return response()->json($factura);
    }

    // PATCH /api/facturas/{id}   { "estado": "pagada" }
    public function update(Request $request, Factura $factura)
    {
        $request->validate(['estado' => 'required|in:pagada']);

        if ($factura->estado === 'pagada') {
            return response()->json(['error' => 'La factura ya fue pagada'], 409);
        }

        $factura->update(['estado' => 'pagada']);
        return response()->json($factura);
    }

    public function destroy(Factura $factura)
    {
        $factura->delete();
        return response()->noContent();
    }
}
```
#### routes/api.php
```php
<?php

use App\Http\Controllers\FacturaController;
use Illuminate\Support\Facades\Route;

Route::apiResource('facturas', FacturaController::class)
    ->parameters(['facturas' => 'factura']);
```
#### database/seeders/DatabaseSeeder.php
```php
<?php

namespace Database\Seeders;

use App\Models\Factura;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Factura::create(['ci' => '11021654', 'periodo' => '2026-09', 'monto' => 85.5]);
        Factura::create(['ci' => '11021654', 'periodo' => '2026-10', 'monto' => 92]);
        Factura::create(['ci' => '5551234', 'periodo' => '2026-10', 'monto' => 40]);
    }
}
```

### Paso 3 · Empresa ELAPAS (copia)
1. Copia toda la carpeta `cessa` como `elapas`.
2. En `elapas/.env`: `DB_DATABASE=bd_elapas`.
3.
```bash
php artisan migrate:fresh --seed
php artisan serve --port=8002
```

### Paso 4 · Central de Pagos (SOAP)
[Crear como la plantilla 05](../../plantillas/05-soap-csharp/README.md). Si no reconoce `JavaScriptSerializer`: clic derecho en el sitio → *Agregar referencia* → `System.Web.Extensions`.
#### Servicio.asmx
```aspx
<%@ WebService Language="C#" CodeBehind="~/App_Code/Servicio.cs" Class="Servicio" %>
```
#### App_Code/Servicio.cs
```csharp
using System;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Web.Services;
using System.Web.Services.Protocols;

// CENTRAL DE PAGOS: hacia el cliente es SOAP; por detras consume GraphQL (SEGIP) y REST (empresas)
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class Servicio : System.Web.Services.WebService
{
    private const string SEGIP = "http://localhost:4000/graphql";

    private static readonly Dictionary<string, string> EMPRESAS = new Dictionary<string, string>
    {
        { "CESSA",  "http://localhost:8001/api/facturas" },
        { "ELAPAS", "http://localhost:8002/api/facturas" }
    };

    [WebMethod]
    public Deuda[] consultarDeudas(string ci)
    {
        VerificarPersona(ci);

        var deudas = new List<Deuda>();
        foreach (var empresa in EMPRESAS)
        {
            string json = Llamar(empresa.Value + "?ci=" + Uri.EscapeDataString(ci), "GET", null);
            foreach (var f in new JavaScriptSerializer().Deserialize<List<Factura>>(json))
            {
                deudas.Add(new Deuda { empresa = empresa.Key, idFactura = f.id, periodo = f.periodo, monto = f.monto });
            }
        }
        return deudas.ToArray();
    }

    [WebMethod]
    public string pagar(string empresa, int idFactura)
    {
        string url;
        if (!EMPRESAS.TryGetValue(empresa ?? "", out url))
        {
            throw new SoapException("Empresa desconocida", SoapException.ClientFaultCode);
        }
        Llamar(url + "/" + idFactura, "PATCH", "{\"estado\":\"pagada\"}");
        return "Pago registrado en " + empresa;
    }

    // Consulta GraphQL: si el CI no existe, se corta aqui
    private void VerificarPersona(string ci)
    {
        var consulta = new
        {
            query = "query($ci: String!) { persona(ci: $ci) { nombre } }",
            variables = new { ci = ci }
        };
        string json = Llamar(SEGIP, "POST", new JavaScriptSerializer().Serialize(consulta));
        if (json.Contains("\"persona\":null"))
        {
            throw new SoapException("SEGIP: el CI no existe", SoapException.ClientFaultCode);
        }
    }

    // Una sola funcion para todas las llamadas HTTP; traduce errores a SoapException
    private string Llamar(string url, string metodo, string cuerpo)
    {
        try
        {
            using (var web = new WebClient { Encoding = Encoding.UTF8 })
            {
                web.Headers["Accept"] = "application/json";
                web.Headers["Content-Type"] = "application/json";
                return metodo == "GET" ? web.DownloadString(url) : web.UploadString(url, metodo, cuerpo);
            }
        }
        catch (WebException ex)
        {
            var r = ex.Response as HttpWebResponse;
            if (r == null) throw new SoapException("No responde: " + url, SoapException.ServerFaultCode);
            throw new SoapException("Error HTTP " + (int)r.StatusCode, SoapException.ClientFaultCode);
        }
    }
}

public class Factura
{
    public int id { get; set; }
    public string ci { get; set; }
    public string periodo { get; set; }
    public double monto { get; set; }
}

public class Deuda
{
    public string empresa { get; set; }
    public int idFactura { get; set; }
    public string periodo { get; set; }
    public double monto { get; set; }
}
```

### Paso 5 · Cliente (Forms)
Referencia de servicio **`srPagos`**.
```csharp
// Cliente de la Central de Pagos (solo habla SOAP)
// ANTES DE PEGAR: agregar referencia de servicio con el nombre  srPagos
using System;
using System.Drawing;
using System.Windows.Forms;

namespace WindowsFormsApp1   // <-- dejar el nombre que tenga TU proyecto
{
    public partial class Form1 : Form
    {
        TextBox txtCi;
        DataGridView tabla;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Central de pagos";
            ClientSize = new Size(500, 320);

            txtCi = new TextBox { Text = "11021654", Location = new Point(10, 10), Width = 150 };

            var consultar = new Button { Text = "Consultar deudas", Location = new Point(170, 8), Width = 130 };
            consultar.Click += (s, e) => Ejecutar(Consultar);

            var pagar = new Button { Text = "Pagar seleccionada", Location = new Point(310, 8), Width = 140 };
            pagar.Click += (s, e) => Ejecutar(Pagar);

            tabla = new DataGridView
            {
                Location = new Point(10, 45), Size = new Size(480, 265), ReadOnly = true,
                AllowUserToAddRows = false, SelectionMode = DataGridViewSelectionMode.FullRowSelect
            };

            Controls.AddRange(new Control[] { txtCi, consultar, pagar, tabla });
        }

        // Si el servidor lanza SoapException, aqui se muestra el mensaje
        void Ejecutar(Action accion)
        {
            try { accion(); }
            catch (Exception ex) { MessageBox.Show(ex.Message); }
        }

        void Consultar()
        {
            var central = new srPagos.ServicioSoapClient();
            tabla.DataSource = central.consultarDeudas(txtCi.Text.Trim());
        }

        void Pagar()
        {
            if (tabla.CurrentRow == null) return;
            var deuda = (srPagos.Deuda)tabla.CurrentRow.DataBoundItem;

            var central = new srPagos.ServicioSoapClient();
            MessageBox.Show(central.pagar(deuda.empresa, deuda.idFactura));
            Consultar();   // refrescar la lista
        }
    }
}
```

Prueba: CI `11021654` → 4 deudas (2 por empresa). Selecciona una → *Pagar* → desaparece. CI `999999` → "SEGIP: el CI no existe".

### Cómo defenderlo
- "El cliente solo conoce **un contrato SOAP**; por detrás hay GraphQL y REST: la central es una **fachada**."
- "Agregar una empresa es **una línea** en el diccionario: **escalabilidad**."
- "Los errores HTTP los traduzco a `SoapException`: `Client` si el pedido estaba mal, `Server` si el otro servicio no responde."
- "Las empresas solo devuelven pendientes y rechazan con 409 un doble pago."

---

## Versión 2 · Pagar todo
```csharp
// En App_Code/Servicio.cs, dentro de la clase Servicio:
[WebMethod]
public double pagarTodo(string ci)
{
    double total = 0;
    foreach (Deuda d in consultarDeudas(ci))     // reutiliza el metodo anterior
    {
        pagar(d.empresa, d.idFactura);
        total += d.monto;
    }
    return total;
}
```

**Cómo defenderlo:** "Reutilizo las operaciones que ya existen. Si una empresa falla a la mitad, las anteriores quedan pagadas: no es una transacción atómica."

---

## Versión 3 · Otra empresa y total
```csharp
// A) Una empresa mas: en el diccionario EMPRESAS agregar una linea
//    { "ENTEL", "http://localhost:8003/api/facturas" }
//    y levantar otra copia del proyecto Laravel en el puerto 8003 con su propia base.

// B) Total de deuda por CI (dentro de la clase Servicio):
[WebMethod]
public double totalDeuda(string ci)
{
    double total = 0;
    foreach (Deuda d in consultarDeudas(ci)) total += d.monto;
    return total;
}
```

> Después de agregar un `[WebMethod]`: compila el sitio y en el cliente clic derecho en la referencia → **Actualizar referencia de servicio**.

**Cómo defenderlo:** "La central compone la información de varias fuentes."

---

## Versión 4 · Venta de pasajes u otro tema

| Central de pagos | Pasajes interdepartamentales |
|---|---|
| SEGIP: persona por CI | SEGIP: pasajero por CI |
| CESSA / ELAPAS (REST) | Dos empresas de buses (REST) |
| factura (periodo, monto) | asiento (ruta, fecha, precio) |
| `GET ?ci=` deudas | `GET ?destino=` asientos libres |
| PATCH `estado: pagada` | PATCH `estado: vendido` |
| `consultarDeudas`, `pagar` | `consultarViajes`, `comprar` |

Cambia migración y controlador en las empresas, y las clases y métodos del SOAP.

El repositorio del curso trae una resolución de examen de **transporte** en Laravel (salidas y asientos): ver [repodistri.md](../../repodistri.md) y [relación uno a muchos](../../logica/README.md#laravel-relación-uno-a-muchos).
