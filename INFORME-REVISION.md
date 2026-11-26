# Informe de revisión del repositorio

[← Índice](README.md)

Revisión completa de la guía (commit `e403223`, 189 archivos): se leyó cada archivo y se ejecutaron los ejercicios que se podían correr en Linux. Nada de lo que ya estaba bien se reescribió; solo se corrigió lo que fallaba y se **agregaron** carpetas nuevas.

## 1. Qué hay y cómo está

La guía está **muy bien armada**: índice "me piden → voy a", conceptos, defensa, errores, `logica/`, 12 plantillas y 7 ejercicios con versiones. Los ejercicios 3, 4 y 5 se corrieron de punta a punta y funcionan.

Problemas encontrados:

| # | Problema | Consecuencia |
|---|---|---|
| 1 | El repo trae **solo código para copiar**: ni `vendor`, ni `node_modules`, ni el jar de JGroups | Por eso se clona en segundos. En el examen (internet solo para clonar) `composer`, `npm` y Maven **no descargan**: Laravel y Node no arrancan si la PC no tiene ya un proyecto base preparado |
| 2 | `preparar-con-internet.bat` y `00-preparacion/README.md` instalan `graphql` **sin versión** | npm instala `graphql` 16 y `express-graphql` 0.12 falla con `ERESOLVE` |
| 3 | El ejercicio 6 (central de pagos) no sigue el **contrato oficial** del enunciado: `verDeuda(CI, primerApellido, segundoApellido, nombres)`, `PagarDeuda(Deuda c)` y SEGIP con `CI, Nombres, PrimerApellido, SegundoApellido` | Si piden exactamente ese enunciado, hay que adaptar a mano |
| 4 | Faltan combinaciones que el docente dijo que pueden caer: REST en Node, GraphQL en Laravel, SOAP en Java, C# consumiendo GraphQL, un servicio que llama a otro con token | Hay que improvisar en el examen |
| 5 | Faltan ejercicios reales: Práctica 5 (cotizaciones), control de aforo del 1er parcial, `transporte` del repo del docente | — |
| 6 | No hay proyectos de Visual Studio listos (`.sln` / `.csproj`) | Hay que crear el proyecto y agregar referencias a mano |
| 7 | En Windows, `git status` muestra todos los archivos modificados | Solo son finales de línea (CRLF); `git diff --ignore-cr-at-eol` no muestra cambios. No afecta |

## 2. Qué se corrigió (archivos que ya existían)

| Archivo | Cambio |
|---|---|
| `00-preparacion/preparar-con-internet.bat` | npm con versiones fijas: `express@4 express-graphql@0.12.0 graphql@15 mysql2 sequelize cors`; composer agrega `webonyx/graphql-php` |
| `00-preparacion/README.md` | Enlace a `sin-internet`, aviso de versiones, atajos `nuevo-laravel.bat` / `nuevo-node.bat` en D.2 y D.3, fila del jar de JGroups |
| `README.md` | Filas nuevas en "Me piden…", ejercicios 8–10, puertos 3000 y 8086, qué se probó en esta revisión |
| `plantillas/README.md`, `ejercicios/README.md` | Tablas con las plantillas 13–16 y ejercicios 8–10 |
| `ejercicios/6-central-pagos/README.md` | Sección "Versión 0 · contrato oficial" al inicio de la tabla |
| `ejercicios/7-jgroups/README.md` | Sección "Versión 4 · Control de aforo" |
| `errores.md` | `ERESOLVE`, versión de PHP, `GraphQL\GraphQL not found`, `exists` 422, `fetch is not defined`, errores de GraphQL con 200 |

## 3. Qué se agregó

| Carpeta | Qué es | Probado |
|---|---|---|
| `00-preparacion/sin-internet/` | **Camino A** (si el docente deja usar internet un rato: instalar una vez y copiar la carpeta) y **Camino B**: `base-laravel.zip` (Laravel 12 + Sanctum + firebase/php-jwt + webonyx, con `vendor`, 38 MB), `base-node.zip` (express 4, express-graphql 0.12, graphql 15, mysql2, sequelize, cors, nodemon, 7 MB), `libs/jgroups-5.5.6.Final.jar`, `nuevo-laravel.bat`, `nuevo-node.bat`, README paso a paso | Sí (bases armadas y usadas en todos los ejercicios nuevos) |
| `plantillas/13-rest-node` | CRUD REST con Express + Sequelize, códigos 201/400/404/409/422 | Sí |
| `plantillas/14-graphql-laravel` | GraphQL en Laravel con webonyx (`POST /api/graphql`) y `graphiql.html` offline | Sí |
| `plantillas/15-soap-java` | Servidor SOAP 1.1 en Java **sin librerías** (HttpServer + WSDL escrito a mano, estilo ASMX) | Sí, con un cliente SOAP real (zeep) y con cliente Java |
| `plantillas/16-proyectos-visual-studio` | `SitioServicio.sln` (ASMX) y `ClienteForms.sln` (Forms 4.7.2 con System.Web.Extensions y System.ServiceModel) | **No compilado** (sin C# en Linux) |
| `ejercicios/6-central-pagos/v0-oficial` | Contrato exacto del enunciado: SEGIP GraphQL, SOAP C# `verDeuda`/`PagarDeuda(Deuda c)`, cliente Forms | SEGIP sí; C# no compilado |
| `ejercicios/7-jgroups/v4-aforo` | Control de aforo del 1er parcial (JGroups) | Sí, dos nodos con `tcp.xml` |
| `ejercicios/8-cotizaciones` | Práctica 5: GraphQL Laravel + REST Node + SOAP Java sobre la misma base | Sí, todas las pruebas obligatorias |
| `ejercicios/9-transporte` | `transporte` del docente resuelto: salidas y asientos, JWT **a mano** como en su resolución, 422/409/401/404, cliente Forms | Laravel sí (12 pruebas); C# no compilado |
| `ejercicios/11-inscripciones` | GraphQL en **Laravel** que llama a REST en **Node** (`Http::get`): cupo, deuda, habilitado, máximo 4 materias; cliente web | Sí (Node, Laravel y la página en navegador real) |
| `ejercicios/12-elecciones` | El `elecicion-api` del docente completo: roles admin/jurado/user (403), actas por mesa (409/422), GraphQL Node que lee la misma base y calcula resultados y segunda vuelta; cliente web | Sí (Laravel, Node y la página en navegador real) |
| `COMO-RESOLVER.md` | Receta paso a paso para cualquier enunciado: primeros 5 minutos, dibujo, orden de construcción, cómo llamar a otro servicio, qué código HTTP devolver | — |
| `ejercicios/10-clinica` | Variante tipo examen: REST pacientes (Laravel + firebase JWT), GraphQL citas (Node) que **llama al REST con token**, cliente C# que consume **REST y GraphQL** | Laravel y Node sí (15 pruebas, incluida "Laravel apagado"); C# no compilado |

Cada carpeta nueva tiene su README con: enunciado, dibujo, orden de arranque, comandos paso a paso, **todo el código** dentro del README (para copiar desde GitHub), tabla de pruebas con el resultado real y variantes.

## 4. Pendiente / a revisar en Windows

- Abrir y compilar en Visual Studio los `.sln` de la plantilla 16 y los `proyecto-vs` de los ejercicios 9 y 10.
- Probar `nuevo-laravel.bat` / `nuevo-node.bat` en la PC del laboratorio (usan `tar`, que viene en Windows 10/11).
- La base de Laravel pide **PHP 8.2+**: confirmar la versión de XAMPP del laboratorio.
- Los zips pesan ~45 MB en total: el clon ya no es instantáneo, pero sigue siendo rápido y **funciona sin internet**.
- `AGENTS.md` pide que ninguna IA edite: estos cambios los revisa y sube una persona (commit manual).
