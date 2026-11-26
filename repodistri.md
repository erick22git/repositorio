---
name: repodistri
description: Resumen del repositorio del curso SistemasDistribuidos2-26 (SIS-258). Usar al preparar o resolver ejercicios para seguir el estilo, las tecnologías y los patrones del curso.
---

# repodistri — el repositorio del curso, en una página

[← Volver al índice](README.md) · Fuente: <https://github.com/cmontellanob/SistemasDistribuidos2-26>

Resumen hecho sobre el estado del repositorio al preparar esta guía (puede haber cambiado después).

## Cómo usar este archivo
Súbelo o pégalo en tu asistente y pide, por ejemplo: *"Siguiendo repodistri.md, resuelve este enunciado"*. Reglas para quien lo use:

1. Usar solo tecnologías y comandos que aparecen aquí o en la hoja de comandos permitidos; si hace falta algo más, **decirlo**.
2. Imitar los nombres y la estructura del curso.
3. No copiar los errores listados al final.
4. El código lo debe entender y escribir cada estudiante.

## Mapa del repositorio

| Carpeta | Tema | Qué contiene |
|---|---|---|
| `repasojava/07-30` | Repaso Java | `HolaMundo`; `Figuras` (interfaz `IFigura` y clases); `Persona` / `universidad` (clases, enums, relaciones) |
| `tema3/08-10 sockes` | Sockets TCP | `holatcp` (servidor, puerto 5002) y `hola-cliente-tcp` |
| `tema3/08-17 tcp protocolos` | Protocolos sobre TCP | `ServidorWeb` (puerto 80, un hilo por cliente); `operaciones/1protocolo` y `2interactivo` |
| `tema3/08-20 soquets UDP` | UDP | `ServerUDP` y `ClientUDP`, puerto 6789 |
| `tema4/08-24/suma` | RMI | `InterfazSuma`, `ClaseSuma`, servidor y cliente, registro 1099 |
| `tema4/08-31 RMI TCP` | RMI + sockets | `Practica2`: sistema de consultas judiciales (`IJusticia`) |
| `09-28 resolucion Examen` | Examen resuelto | Turista → Operadora (RMI) → Migración (TCP), Banco (RMI) → Antifraude (UDP) |
| `tema7/09-17` | SOAP | `HolaMundo` ASMX (código en `App_Code`) |
| `tema7/10-05 SOAP` | SOAP y clientes | `SitioMatematicas` (`wsOperaciones`), `ClienteOperaciones` (Forms), `ClienteJava` (`wsimport`), `ConsumirImpuestos` (Forms contra un servicio externo) |
| `tema7/10-15 Grapql` | GraphQL | `graphql-mysql` (Express + Sequelize) y `bd_agenda_api.sql` |
| `11-16 resolucion 2p/transporte` | REST Laravel 12 | Salidas y asientos, JWT hecho a mano; su `README.md` es una chuleta de Artisan |

## Estilo del código

| Tema | Costumbre |
|---|---|
| Puertos | TCP **5002**, UDP **6789**, RMI **1099**, GraphQL **4000**, Laravel 8000 |
| TCP | Lectura con `BufferedReader`, escritura con `PrintStream`; variables `fromClient`, `toClient`, `fromServer`, `toServer` |
| Lógica del servidor | En un método estático `procesarSolicitud(String)` |
| Protocolo | Texto `clave:valor` separado con `split(":")` |
| RMI | `Registry reg`, interfaces `I...` o `Interfaz...`, `rebind` / `lookup` |
| SOAP | Clases `ws...`; referencias de servicio `sr...` (`srOperaciones.wsOperacionesSoapClient`) |
| Controles WinForms | Prefijos `txt`, `cb`, `lbl` |
| GraphQL | Tres archivos: `database.js`, `schema.js` (`RootQueryType`, `Mutation`), `server.js` |
| Proyectos Java | Maven (`com.com350...`, `com.sis258...`) y Ant (NetBeans) |

## Patrones mínimos

### RMI
```java
public interface InterfazSuma extends Remote {
    int sumar(int a, int b) throws RemoteException;
}
// servidor
Registry reg = LocateRegistry.createRegistry(1099);
reg.rebind("Suma", new ClaseSuma());
// cliente
InterfazSuma s = (InterfazSuma) LocateRegistry.getRegistry(1099).lookup("Suma");
```

### SOAP
```csharp
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class wsOperaciones : System.Web.Services.WebService
{
    [WebMethod]
    public int Sumar(int a, int b) { return a + b; }
}
```

### GraphQL
```js
const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    personas: { type: new GraphQLList(PersonaType), resolve() { return Persona.findAll(); } }
  }
});
```
El ejemplo del curso expone `personas`, `persona(id)`, `addPersona` y `updatePersona`, y **no** usa `sync()` porque la tabla ya existe.

### JWT a mano (Laravel)
```php
$b64 = fn (string $t) => rtrim(strtr(base64_encode($t), '+/', '-_'), '=');
$h = $b64(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
$p = $b64(json_encode(['email' => $email, 'iat' => time(), 'exp' => time() + 3600]));
$f = $b64(hash_hmac('sha256', "$h.$p", $secreto, true));
$token = "$h.$p.$f";
```

### Comandos Artisan de la chuleta del curso
```bash
composer install
copy .env.example .env
php artisan key:generate
php artisan install:api
php artisan make:model Salida -mfsc --api
php artisan make:controller SalidaController --api --model=Salida
php artisan migrate
php artisan migrate:fresh --seed
php artisan migrate:rollback
php artisan migrate:status
php artisan db:seed --class=SalidaSeeder
php artisan route:list --path=api
php artisan tinker
php artisan config:clear
php artisan serve
```

## Cómo arma el docente un ejercicio
Observado en el repositorio, las prácticas y los parciales:

- Un **contexto institucional** (justicia, turismo, multas, hoteles, pagos).
- **Varios sistemas** con tecnologías distintas que deben integrarse.
- Un componente que es **servidor y cliente a la vez** (orquestador).
- **Contratos dados**: nombres de métodos, formatos de mensaje, datos de ejemplo. Respétalos exactamente.
- Validaciones en cadena (primero verificar, después registrar o cobrar).
- Un cliente final: consola, escritorio o web.

## Errores del repositorio que NO conviene copiar
| Dónde | Detalle | Corrección |
|---|---|---|
| `operaciones/1protocolo` y `2interactivo` | `new ServerSocket` **dentro** del `while` | Crearlo una vez, antes |
| `operaciones/2interactivo` | `"restar"` suma en vez de restar; solo hay suma y resta | Revisar cada `case` |
| `SoquetsUDP/ServerUDP` | `new String(peticion.getData())` sin longitud: llega basura | `new String(datos, 0, peticion.getLength())` |
| `SoquetsUDP/ServerUDP` | El largo de la respuesta se toma de la cadena, no de los bytes | `mensaje.length` |
| `Practica2/IJusticia` | Un método remoto sin `throws RemoteException` | Todos deben tenerlo |
| `09-28 resolucion Examen` | Nombre y puerto del registro no coinciden entre servidor y cliente | Mismo puerto y mismo nombre, con mayúsculas iguales |
| `09-28 resolucion Examen/Operadora` | Pregunta `respuesta[1].equals("Valido")`, pero Migración contesta `valido:Bolivia`: el estado está en `respuesta[0]` y en minúscula | Comparar la posición y el texto correctos |
| `09-28 resolucion Examen/Banco` y `ServidorAntifraude` | El banco envía un nombre escrito por teclado y el antifraude espera el formato `x:y-monto` | Acordar un solo formato en los dos lados |
| `09-28 resolucion Examen/ServerMigracion` | `new ServerSocket` dentro del `while` | Crearlo una vez, antes |
| `SitioMatematicas` | `Dividir` no controla cero | Validar y lanzar `SoapException` |
| `transporte` | Controladores vacíos; el modelo `Salida` declara `hora` en `$fillable` pero la migración no crea esa columna | Mantener migración y modelo iguales |

> Nunca subas claves o *tokens* reales a un repositorio público.
