# Cómo resolver el examen paso a paso (cualquier enunciado)

[← Índice](README.md)

Esta es la receta general. Los ejercicios resueltos son **ejemplos** de esta receta; si el enunciado no se parece a ninguno, sigue estos pasos igual.

## Paso 0 · Primeros 5 minutos (antes de escribir código)
1. **Clonar** la guía: `git clone https://github.com/erick22git/repositorio C:\guia`.
2. Preguntar al docente si se puede usar internet un rato para instalar → [Camino A o B](00-preparacion/sin-internet/README.md#qué-camino-uso-decidirlo-al-empezar-el-examen).
3. Encender **XAMPP → Apache y MySQL**.
4. Crear en `C:\examen` los proyectos base que vas a necesitar (`nuevo-laravel.bat`, `nuevo-node.bat`).

## Paso 1 · Leer el enunciado y dibujar
Subraya **cada programa** y **con qué tecnología habla**. Dibuja cajas y flechas en una hoja:

```
 [Cliente ¿web/C#/Java?] ──¿REST/GraphQL/SOAP?──> [Servicio A ¿Laravel/Node/C#/Java?] ──¿?──> [Servicio B] ──> [BD]
```

Por cada caja, responde:
| Pregunta | Ejemplo |
|---|---|
| ¿En qué lenguaje / framework? | "REST en Laravel" |
| ¿Qué datos guarda? (tabla y campos) | `pacientes(ci, nombres, apellidos, seguro)` |
| ¿Qué operaciones ofrece? | `GET /pacientes/{ci}`, `POST /pacientes` |
| ¿A quién llama? | "GraphQL llama al REST para validar el CI" |
| ¿Está protegido? (JWT, roles) | "solo admin crea → 403" |
| ¿Qué reglas de negocio? | "no repetir", "no pasar el cupo", "saldo suficiente" |

## Paso 2 · Buscar el ejercicio más parecido
| Lo que dibujaste se parece a… | Abre |
|---|---|
| Cliente → **un** REST con login | [9 transporte](ejercicios/9-transporte/README.md) (JWT a mano) · [12 elecciones](ejercicios/12-elecciones/README.md) (firebase + roles) |
| GraphQL **Node** → REST **Laravel** con token, cliente C# | [10 clínica](ejercicios/10-clinica/README.md) |
| GraphQL **Laravel** → REST **Node**, cliente web | [11 inscripciones](ejercicios/11-inscripciones/README.md) |
| Dos servicios leen **la misma base** (uno escribe, otro reporta) | [12 elecciones](ejercicios/12-elecciones/README.md), [8 cotizaciones](ejercicios/8-cotizaciones/README.md) |
| **SOAP** que consume REST y GraphQL | [6 central de pagos](ejercicios/6-central-pagos/README.md) (v0 = contrato oficial) |
| SOAP en **Java**, GraphQL en Laravel, REST en Node | [8 cotizaciones](ejercicios/8-cotizaciones/README.md) |
| SOAP + REST + GraphQL + web + Forms todo junto | [4 multas](ejercicios/4-multas/README.md), [5 hotel](ejercicios/5-reservas-hotel/README.md) |
| Sockets / RMI / JGroups | [1](ejercicios/1-sockets/README.md), [2](ejercicios/2-rmi-sockets/README.md), [3](ejercicios/3-pago-en-linea/README.md), [7](ejercicios/7-jgroups/README.md) |

Si no hay uno igual: arma cada caja con su **plantilla** (tabla del [índice](README.md#me-piden--voy-a)).

## Paso 3 · Construir de atrás hacia adelante
**El orden importa**: primero lo que **solo responde**, al final el cliente.

1. **Base de datos**: crear la base en phpMyAdmin (una por proyecto, o la misma si el enunciado dice "misma base").
2. **Servicio que no llama a nadie** (normalmente el REST con la tabla principal):
   - Laravel: `install:api` → `make:model X -m` (o `-mfsc --api`) → campos en la migración → `$fillable` → controlador → `routes/api.php` → seeder → `migrate:fresh --seed` → `serve`.
   - Node: `database.js` (modelo) → `server.js` o `schema.js` → `node server.js`.
3. **Probarlo solo** en Postman / GraphiQL: un caso bien y **cada** error (404, 409, 422, 401, 403).
4. **Servicio que llama al anterior**: copiar la función que llama (ver tabla de abajo), probar con el otro **encendido** y luego **apagado** (debe dar un mensaje claro, no caerse).
5. **Cliente** (web, Forms o Java): primero que muestre una lista; después los botones que crean / cambian.

### Cómo llamar a otro servicio (lo que más se repite)
| Desde | REST | GraphQL | Dónde está el código |
|---|---|---|---|
| Laravel | `Http::withToken($t)->get($url)` / `Http::post($url, [...])` | `Http::post($url, ['query' => '...', 'variables' => [...]])` | [11](ejercicios/11-inscripciones/README.md) `Esquema.php`, [logica/consumir](logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |
| Node | `fetch(url, { headers: { Authorization: 'Bearer ' + token } })` | `fetch(url, { method: 'POST', body: JSON.stringify({ query, variables }) })` | [10](ejercicios/10-clinica/README.md) `laravel.js` |
| C# (Forms / ASMX) | `WebClient.DownloadString` / `UploadString` + `JavaScriptSerializer` | `UploadString(url, "POST", Json(new { query, variables }))` | [9](ejercicios/9-transporte/README.md), [10](ejercicios/10-clinica/README.md) `Form1.cs` |
| Java | `HttpClient` del JDK | igual, POST con JSON | [plantilla 12](plantillas/12-cliente-java/README.md), [8](ejercicios/8-cotizaciones/README.md) `soap-java` |
| Web (JS) | `fetch` | `fetch` POST | [11](ejercicios/11-inscripciones/README.md), [12](ejercicios/12-elecciones/README.md) `.html` |

### Reglas de negocio típicas y cómo se responden
| Regla | REST (código HTTP) | GraphQL / SOAP |
|---|---|---|
| No existe | 404 | `throw new Error(...)` / `UserError` / `SoapException` |
| Ya existe / repetido / ya pagado / ya vendido | 409 | error con mensaje |
| Datos mal / pasa el cupo / no alcanza el saldo | 422 (o 400) | error con mensaje |
| Sin token o token falso | 401 | — (si piden, revisar `context`) |
| Con token pero sin permiso (rol) | 403 | — |
| El otro servicio no responde | 502/503 o mensaje | "El servicio X no responde" (`try/catch`) |

Ejemplos de cada uno ya resueltos: cupo → [11](ejercicios/11-inscripciones/README.md); asiento vendido → [9](ejercicios/9-transporte/README.md); horario ocupado → [10](ejercicios/10-clinica/README.md); roles → [12](ejercicios/12-elecciones/README.md); saldo → [3](ejercicios/3-pago-en-linea/README.md), [logica](logica/README.md#laravel-débito-y-abono).

## Paso 4 · Probar todo junto y preparar la defensa
1. Encender todo **en orden** (el README de cada ejercicio lo dice).
2. Hacer el flujo completo desde el cliente: caso bien + **un error de cada regla**.
3. Apagar un servicio y mostrar que el otro responde con mensaje claro.
4. Repasar [defensa.md](defensa.md). Preguntas casi seguras:
   - ¿Qué diferencia hay entre REST, GraphQL y SOAP? (recursos y verbos HTTP / una sola URL y pides los campos / XML con contrato WSDL)
   - ¿Para qué sirve el JWT y dónde se valida? (identidad sin sesión; en el middleware, con la clave secreta)
   - ¿Por qué 409 y no 400? (el dato está bien escrito, pero choca con el estado actual)
   - ¿Qué pasa si el otro servicio se cae? (mostrarlo)

## Si te trancas
| Problema | Ver |
|---|---|
| Un error que no entiendes | [errores.md](errores.md) |
| No arranca Laravel / Node sin internet | [sin-internet](00-preparacion/sin-internet/README.md) |
| No sé cómo agregar una regla | [logica](logica/README.md) |
| No sé cómo se llama algo | [01-conceptos](01-conceptos/README.md) |
