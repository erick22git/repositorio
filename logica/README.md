# Lógica extra (para poner ENCIMA de las plantillas)

[← Volver al índice](../README.md)

Las [plantillas](../plantillas/README.md) son para copiar tal cual. Aquí están los **pedazos** que se agregan según lo que pidan.

## Índice
| Me piden… | Ir a |
|---|---|
| Llamar a SOAP / REST / GraphQL / socket desde otro lenguaje | [Consumir servicios](#consumir-servicios-desde-cualquier-lenguaje) |
| No borrar de verdad | [Borrado lógico](#laravel-borrado-lógico) |
| Buscar por código | [Buscar por código](#laravel-buscar-por-código) |
| Filtrar, ordenar, sumar | [Filtros y totales](#laravel-filtros-y-totales) |
| Débito / abono / transferencia | [Débito y abono](#laravel-débito-y-abono) |
| Dos tablas relacionadas | [Uno a muchos](#laravel-relación-uno-a-muchos) |
| Filtrar o borrar en GraphQL | [GraphQL](#graphql-filtrar-y-borrar) |
| Registrar, eliminar, sumar en SOAP | [SOAP](#soap-más-operaciones) |
| Guardar sin base de datos | [Archivo](#java-guardar-en-archivo) |

Los fragmentos usan los nombres genéricos de las plantillas (`Recurso`, `Dato`): cámbialos por los tuyos.

---

## Consumir servicios desde cualquier lenguaje

| Desde \ Hacia | REST | GraphQL | SOAP | Socket TCP |
|---|---|---|---|---|
| Java | [Plantilla 12](../plantillas/12-cliente-java/README.md) | [GraphQLDesdeJava](#graphqldesdejavajava) | [SoapDesdeJava](#soapdesdejavajava) | [Plantilla 01](../plantillas/01-socket-tcp-java/README.md) |
| C# | [DesdeCSharp](#desdecsharpcs) | [DesdeCSharp](#desdecsharpcs) | Referencia de servicio ([plantilla 11](../plantillas/11-cliente-forms-csharp/README.md)) | [DesdeCSharp](#desdecsharpcs) |
| Laravel | [DesdeLaravel](#desdelaravelphp) | [DesdeLaravel](#desdelaravelphp) | [DesdeLaravel](#desdelaravelphp) | [DesdeLaravel](#desdelaravelphp) |
| PHP puro | [consumir.php](#consumirphp) | — | [consumir.php](#consumirphp) | [DesdeLaravel](#desdelaravelphp) |
| JavaScript | [consumir.js](#consumirjs) | [consumir.js](#consumirjs) | [consumir.js](#consumirjs) | No se puede desde el navegador |

El puerto `51305` de los ejemplos SOAP es de muestra: pon el que abrió Visual Studio.

### SoapDesdeJava.java
**[EXTRA]**. La hoja del curso usa `wsimport` para Java, pero **el JDK 11 o superior ya no lo trae**; esta clase evita el problema.
```java
import java.net.URI;
import java.net.http.*;
import java.util.regex.*;

// [EXTRA] Consumir un servicio SOAP desde Java SIN wsimport ni librerias.
public class SoapDesdeJava {

    // CAMBIAR el puerto por el que abrio Visual Studio
    static final String URL = "http://localhost:51305/Servicio.asmx";

    public static void main(String[] args) {
        System.out.println(llamar("Verificar", "<codigo>A1</codigo>"));
    }

    // operacion = nombre del [WebMethod];  parametros = <nombre>valor</nombre> (igual que en el WSDL)
    static String llamar(String operacion, String parametros) {
        String sobre = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                + "<" + operacion + " xmlns=\"http://tempuri.org/\">" + parametros + "</" + operacion + ">"
                + "</soap:Body></soap:Envelope>";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "\"http://tempuri.org/" + operacion + "\"")
                    .POST(HttpRequest.BodyPublishers.ofString(sobre))
                    .build();
            String xml = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString()).body();

            Matcher ok = Pattern.compile("<" + operacion + "Result>(.*)</" + operacion + "Result>",
                    Pattern.DOTALL).matcher(xml);
            if (ok.find()) return ok.group(1);

            Matcher error = Pattern.compile("<faultstring>(.*?)</faultstring>", Pattern.DOTALL).matcher(xml);
            return error.find() ? "ERROR:" + error.group(1) : "ERROR:respuesta inesperada " + xml;

        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
```

Con `wsimport` instalado **[INGE]** se generan clases desde `?WSDL` y se usa como en el repositorio:
```java
WsOperaciones service = new WsOperaciones();
WsOperacionesSoap port = service.getWsOperacionesSoap12();
int result = port.sumar(10, 20);
```

### GraphQLDesdeJava.java
```java
import java.net.URI;
import java.net.http.*;

// Consumir GraphQL desde Java: es un POST con {"query":"..."}
public class GraphQLDesdeJava {

    public static void main(String[] args) throws Exception {
        System.out.println(consultar("{ recursos { codigo nombre estado } }"));
        // Con comillas dentro de la consulta:
        System.out.println(consultar("{ recurso(codigo: \"A1\") { nombre } }"));
    }

    static String consultar(String query) throws Exception {
        String json = "{\"query\":\"" + query.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:4000/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
```

### DesdeCSharp.cs
```csharp
// Fragmentos para C# (.NET Framework). Pegar dentro de un metodo de tu clase.
// using System.Net; using System.Net.Sockets; using System.IO; using System.Text;

// ---------- REST ----------
var web = new WebClient { Encoding = Encoding.UTF8 };
web.Headers["Accept"] = "application/json";
string lista = web.DownloadString("http://localhost:8000/api/recursos");

web.Headers["Content-Type"] = "application/json";
string creado = web.UploadString("http://localhost:8000/api/recursos", "POST",
    "{\"codigo\":\"X1\",\"nombre\":\"n\",\"monto\":5}");

// ---------- GraphQL ----------
web.Headers["Content-Type"] = "application/json";
string gql = web.UploadString("http://localhost:4000/graphql", "POST",
    "{\"query\":\"{ recursos { codigo nombre } }\"}");

// ---------- Socket TCP (cliente de un servidor Java) ----------
using (var cliente = new TcpClient("localhost", 5002))
using (var red = cliente.GetStream())
{
    var escribir = new StreamWriter(red) { AutoFlush = true, NewLine = "\n" };
    var leer = new StreamReader(red);
    escribir.WriteLine("hola");
    string respuesta = leer.ReadLine();
}
```

### DesdeLaravel.php
```php
<?php
// Fragmentos para pegar DENTRO de un controlador de Laravel (no es un archivo completo)
// Arriba del controlador:  use Illuminate\Support\Facades\Http;

// REST
$r = Http::acceptJson()->timeout(5)->get('http://localhost:8001/api/recursos');
$r = Http::acceptJson()->post('http://localhost:8001/api/recursos', ['codigo' => 'X1', 'nombre' => 'n', 'monto' => 5]);
$r = Http::withToken($token)->acceptJson()->get('http://localhost:8001/api/recursos');
if ($r->failed()) {
    return response()->json(['error' => 'El otro servicio respondio ' . $r->status()], 502);
}
$datos = $r->json();

// GraphQL
$r = Http::post('http://localhost:4000/graphql', [
    'query'     => 'query($c: String!) { recurso(codigo: $c) { nombre } }',
    'variables' => ['c' => 'A1'],
]);
$nombre = $r->json('data.recurso.nombre');

// SOAP  (activar extension=soap en php.ini)
$soap = new \SoapClient('http://localhost:51305/Servicio.asmx?WSDL');
$existe = $soap->__soapCall('Verificar', [['codigo' => 'A1']])->VerificarResult;

// Socket TCP (para hablar con un servidor Java)
$s = fsockopen('localhost', 5002, $errno, $errstr, 5);
fwrite($s, "hola\n");
$respuesta = trim(fgets($s));
fclose($s);
```

### consumir.php
Se ejecuta con `php consumir.php`.
```php
<?php
// Desde PHP puro:  php consumir.php
// SOAP necesita activar  extension=soap  en C:\xampp\php\php.ini

// ---------- SOAP ----------
try {
    $cliente = new SoapClient('http://localhost:51305/Servicio.asmx?WSDL',
        ['features' => SOAP_SINGLE_ELEMENT_ARRAYS]);

    $r = $cliente->__soapCall('Verificar', [['codigo' => 'A1']]);
    var_dump($r->VerificarResult);

    $lista = $cliente->__soapCall('Listar', [[]]);
    foreach ($lista->ListarResult->Dato as $d) {
        echo $d->Codigo, ' ', $d->Monto, PHP_EOL;
    }
} catch (SoapFault $e) {
    echo 'Error SOAP: ', $e->getMessage(), PHP_EOL;
}

// ---------- REST con cURL ----------
$ch = curl_init('http://localhost:8000/api/recursos');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER     => ['Accept: application/json'],
]);
$respuesta = curl_exec($ch);
echo curl_getinfo($ch, CURLINFO_HTTP_CODE), ' ', $respuesta, PHP_EOL;
```

### consumir.js
Se ejecuta con `node consumir.js` o en la consola del navegador. En el navegador, el SOAP de Visual Studio puede quedar bloqueado por CORS.
```javascript
// Funciona en el navegador y en Node 18 o superior

// REST
async function rest() {
  const r = await fetch('http://localhost:8000/api/recursos', {
    headers: { Accept: 'application/json' }       // , Authorization: 'Bearer ' + token
  });
  console.log(r.status, await r.json());
}

// GraphQL
async function graphql() {
  const r = await fetch('http://localhost:4000/graphql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: 'query($c: String!) { recurso(codigo: $c) { nombre estado } }',
      variables: { c: 'A1' }
    })
  });
  const { data, errors } = await r.json();        // GraphQL: mirar errors
  console.log(errors ? errors[0].message : data);
}

// SOAP
async function soap() {
  const sobre = `<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
    <Verificar xmlns="http://tempuri.org/"><codigo>A1</codigo></Verificar>
  </soap:Body></soap:Envelope>`;
  const r = await fetch('http://localhost:51305/Servicio.asmx', {
    method: 'POST',
    headers: { 'Content-Type': 'text/xml; charset=utf-8', SOAPAction: '"http://tempuri.org/Verificar"' },
    body: sobre
  });
  console.log(await r.text());
}

rest().catch(e => console.log('REST no responde:', e.message));
graphql().catch(e => console.log('GraphQL no responde:', e.message));
soap().catch(e => console.log('SOAP no responde:', e.message));
```

---

## Laravel: borrado lógico
```php
<?php
// Opcion A (a mano). En la migracion:
$table->boolean('activo')->default(true);
// En $fillable agregar 'activo'. En el controlador:
public function index()
{
    return response()->json(Recurso::where('activo', true)->get());
}

public function destroy(Recurso $recurso)
{
    $recurso->update(['activo' => false]);
    return response()->noContent();
}
```
**Defensa:** "No pierdo la información; solo la marco como inactiva para poder auditarla."

## Laravel: buscar por código
```php
<?php
// Para que /api/recursos/A1 funcione (en vez de /api/recursos/1), en el MODELO:
public function getRouteKeyName(): string
{
    return 'codigo';
}
```
**Defensa:** "El *route model binding* busca por esa columna y responde 404 si no existe."

## Laravel: filtros y totales
```php
<?php
// GET /api/recursos?estado=activo&min=50
public function index(Request $request)
{
    $q = Recurso::query();
    if ($request->filled('estado')) $q->where('estado', $request->query('estado'));
    if ($request->filled('min'))    $q->where('monto', '>=', $request->query('min'));
    return response()->json($q->orderBy('monto')->get());
}

// Ruta aparte (ponerla ANTES del apiResource):  Route::get('/recursos-total', [RecursoController::class, 'total']);
public function total()
{
    return response()->json([
        'cantidad' => Recurso::count(),
        'suma'     => Recurso::sum('monto'),
    ]);
}
```
**Defensa:** "Los filtros van en la *query string*; la consulta se arma solo con los que llegan."

## Laravel: débito y abono
```php
<?php
// PATCH /api/recursos/{recurso}   { "operacion": "debito", "monto": 10 }
// Arriba:  use Illuminate\Support\Facades\DB;
public function update(Request $request, Recurso $recurso)
{
    $datos = $request->validate([
        'operacion' => 'required|in:debito,abono',
        'monto'     => 'required|numeric|gt:0',
    ]);

    // Transaccion + bloqueo: dos peticiones no pueden gastar el mismo saldo
    return DB::transaction(function () use ($recurso, $datos) {
        $r = Recurso::lockForUpdate()->find($recurso->id);

        if ($datos['operacion'] === 'debito' && $r->monto < $datos['monto']) {
            return response()->json(['error' => 'Saldo insuficiente'], 409);
        }

        $r->monto += $datos['operacion'] === 'debito' ? -$datos['monto'] : $datos['monto'];
        $r->save();

        return response()->json($r);
    });
}
```
**Defensa:** "La **transacción** hace que sea todo o nada y `lockForUpdate` bloquea la fila para que dos pagos no gasten el mismo saldo."

## Laravel: relación uno a muchos
```php
<?php
// Ejemplo: un recurso tiene muchos movimientos
// php artisan make:model Movimiento -msfc --api

// 1) Migracion de movimientos
$table->id();
$table->foreignId('recurso_id')->constrained('recursos')->cascadeOnDelete();
$table->decimal('monto', 10, 2);
$table->timestamps();

// 2) Modelo Movimiento
protected $table = 'movimientos';
protected $fillable = ['recurso_id', 'monto'];

// 3) Modelo Recurso
public function movimientos()
{
    return $this->hasMany(Movimiento::class);
}

// 4) En RecursoController@show : devolver el recurso CON sus movimientos
public function show(Recurso $recurso)
{
    return response()->json($recurso->load('movimientos'));
}
```
**Defensa:** "`hasMany` / `belongsTo` representan la clave foránea; `load` trae los hijos en la misma respuesta."

## GraphQL: filtrar y borrar
```javascript
// Pegar dentro de "fields" de RootQuery en schema.js
// (arriba: const { Op } = require('sequelize');)
recursosPorEstado: {
  type: new GraphQLList(RecursoType),
  args: { estado: { type: GraphQLString }, min: { type: GraphQLFloat } },
  resolve(parent, { estado, min }) {
    const where = {};
    if (estado) where.estado = estado;
    if (min) where.monto = { [Op.gte]: min };
    return Recurso.findAll({ where });
  }
},

// Pegar dentro de "fields" de Mutation
eliminarRecurso: {
  type: GraphQLBoolean,
  args: { codigo: { type: new GraphQLNonNull(GraphQLString) } },
  async resolve(parent, { codigo }) {
    const borrados = await Recurso.destroy({ where: { codigo } });
    if (!borrados) throw new Error('No existe ' + codigo);
    return true;
  }
},
```

## SOAP: más operaciones
```csharp
// Pegar DENTRO de la clase Servicio (App_Code/Servicio.cs)

[WebMethod]
public Dato Registrar(string codigo, string nombre, double monto)
{
    if (string.IsNullOrWhiteSpace(codigo) || monto <= 0)
        throw new SoapException("Datos invalidos", SoapException.ClientFaultCode);
    if (datos.Exists(d => d.Codigo == codigo))
        throw new SoapException("Ya existe " + codigo, SoapException.ClientFaultCode);

    var nuevo = new Dato { Codigo = codigo, Nombre = nombre, Monto = monto };
    datos.Add(nuevo);
    return nuevo;
}

[WebMethod]
public bool Eliminar(string codigo)
{
    return datos.RemoveAll(d => d.Codigo == codigo) > 0;
}

[WebMethod]
public double Total()
{
    double suma = 0;
    foreach (var d in datos) suma += d.Monto;
    return suma;
}
```

## Java: guardar en archivo
```java
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

// [EXTRA] Persistencia sin base de datos: una linea por registro.
public class GuardarEnArchivo {

    static final Path ARCHIVO = Path.of("datos.txt");

    static synchronized void agregar(String linea) throws IOException {
        Files.writeString(ARCHIVO, linea + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    static synchronized List<String> leer() throws IOException {
        return Files.exists(ARCHIVO) ? Files.readAllLines(ARCHIVO) : List.of();
    }

    public static void main(String[] args) throws IOException {
        agregar("A1;Ejemplo;100");
        for (String l : leer()) System.out.println(l.split(";")[1]);
    }
}
```
