# Me sale un error

[← Volver al índice](README.md)

## Java
| Mensaje | Causa y solución |
|---|---|
| `Address already in use` | Ya hay un programa en ese puerto. Detén el anterior. Para encontrarlo: `netstat -ano \| findstr :5002` y `taskkill /PID numero /F` **[EXTRA]** |
| `Connection refused` | El servidor está apagado, o puerto/IP equivocados |
| `NotBoundException` | El nombre del `lookup` no es igual al del `rebind` |
| `NotSerializableException` | Falta `implements Serializable` |
| `ClassNotFoundException` / `ClassCastException` en RMI | La interfaz está en paquetes distintos |
| `package org.jgroups does not exist` | Falta agregar el jar |
| `class X is public, should be declared in a file named X.java` | El archivo se llama distinto que la clase |
| JGroups: cada nodo se ve solo | Multicast bloqueado → [usar tcp.xml](plantillas/04-jgroups-java/README.md#si-cada-nodo-se-ve-solo-red-de-la-facultad-o-wi-fi) |

## Laravel
Mira siempre las **primeras líneas** de `storage/logs/laravel.log`.

| Mensaje | Solución |
|---|---|
| `could not find driver` | Activar `extension=pdo_mysql` en `php.ini` |
| `Unknown database` | Crear la base en phpMyAdmin |
| `Connection refused` (SQLSTATE 2002) | Encender MySQL en XAMPP |
| `Table ... doesn't exist` | `php artisan migrate:fresh --seed` |
| `No application encryption key` | `php artisan key:generate` |
| 404 en `/api/...` | Falta `install:api`, o revisa `php artisan route:list --path=api` |
| Devuelve HTML en vez de JSON | Enviar `Accept: application/json` |
| `{multum}` en las rutas, registro vacío | Falta `->parameters([...])` |
| `Add [campo] to fillable property` | Agregarlo en `$fillable` |
| `Class "Firebase\JWT\JWT" not found` | No está instalada: [plan B sin librería](plantillas/07-jwt-laravel/README.md#plan-b-no-pude-instalar-la-librería-sin-internet) |
| `Provided key is too short` | `JWT_SECRET` de 32 caracteres o más |
| 401 siempre | Cabecera `Authorization: Bearer TOKEN`; mismo secreto en los dos lados; token vencido |
| Cambié `.env` y no se nota | `php artisan config:clear` y reiniciar `serve` |
| `composer`: `requires php >= ...` | La versión de PHP no coincide con la del proyecto: revisar `php -v`. La base de `sin-internet` pide **PHP 8.2 o más** (XAMPP 8.2 sirve) |
| `Class "GraphQL\GraphQL" not found` | Falta `webonyx/graphql-php`: usar la base de [sin-internet](00-preparacion/sin-internet/README.md) o `composer require webonyx/graphql-php` |
| `The selected salida id is invalid` (422) | La regla `exists:tabla,id` no encontró el registro padre: créalo primero |

## Node / GraphQL
| Mensaje | Solución |
|---|---|
| `Cannot find module 'express'` | Falta `node_modules`: copiar tu base preparada o usar `sin-internet\nuevo-node.bat` |
| `npm ERR! ERESOLVE ... peer graphql@"^14.7.0 \|\| ^15.3.0"` | `npm install graphql` trae la 16 y `express-graphql` solo acepta hasta la 15: `npm install graphql@15 express-graphql@0.12.0` |
| `fetch is not defined` | Node 16 o menor: actualizar a Node 18+ o usar el módulo `http`/`axios` |
| GraphQL responde 200 pero con `errors` | Es normal: GraphQL no usa códigos HTTP para errores. Leer `errors[0].message` |
| `EADDRINUSE` | El puerto 4000 está ocupado |
| `ER_BAD_DB_ERROR` | Crear la base en phpMyAdmin |
| `ECONNREFUSED ...3306` | Encender MySQL |
| `Cannot query field "x"` | El campo no existe en el tipo de `schema.js` |
| `npm run dev` no existe | Agregar el script en `package.json` |
| PowerShell: "la ejecución de scripts está deshabilitada" | Usar el **símbolo del sistema (cmd)** |

## C# / Visual Studio
| Mensaje | Solución |
|---|---|
| No aparecen plantillas ASP.NET o .NET Framework | Visual Studio Installer → Modificar → agregar la carga de trabajo y componentes |
| `srServicio` no existe | Falta la referencia de servicio, o tiene otro nombre |
| `JavaScriptSerializer` no existe | Agregar referencia `System.Web.Extensions` |
| `EndpointNotFoundException` | El servicio SOAP está apagado o cambió de puerto → configurar/actualizar la referencia |
| El método nuevo no aparece en el cliente | Compilar el servicio y **Actualizar referencia de servicio** |
| `The type or namespace 'Form1' ...` / controles duplicados | Dejaste otro `namespace`: usa el de tu proyecto |

## Navegador
| Síntoma | Solución |
|---|---|
| "blocked by CORS policy" | El servidor debe permitirlo. Laravel: `php artisan config:publish cors`. Node: la plantilla ya lo trae |
| `Failed to fetch` | Servidor apagado o URL mal escrita |

## Windows
| Problema | Solución |
|---|---|
| Puerto 80 ocupado | Usar otro puerto, o `net stop http` como administrador |
| Otra PC no se conecta | [Firewall e IP](00-preparacion/README.md#e-dos-computadoras-en-red) |
| `php`, `composer` o `java` "no se reconoce" | No está en el PATH: usar la ruta completa (ej. `C:\xampp\php\php.exe`) |
