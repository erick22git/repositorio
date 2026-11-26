# Sin internet: todo lo que hace falta ya viene en el repositorio

[← Preparación](../README.md) · [Índice](../../README.md) · **[EXTRA]**

En el examen solo hay internet para **clonar**. Después `composer`, `npm install` y Maven **no descargan nada**.
Por eso aquí están los proyectos base **con sus dependencias ya descargadas**:

| Archivo | Qué trae | Para |
|---|---|---|
| `base-laravel.zip` (38 MB) | Laravel **12.69** con `vendor/` completo: Sanctum (para `install:api`), **firebase/php-jwt** (JWT del docente), **webonyx/graphql-php** (GraphQL en Laravel) y Faker (factories). `.env` ya apunta a MySQL de XAMPP (`root`, sin clave) y trae `JWT_SECRET` | REST, JWT, GraphQL en Laravel |
| `base-node.zip` (7 MB) | `node_modules/` con **express 4**, **express-graphql 0.12**, **graphql 15**, **mysql2**, **sequelize**, **cors** y **nodemon**. `package.json` con `npm run dev` | GraphQL en Node y REST en Node |
| `libs/jgroups-5.5.6.Final.jar` | JGroups 5.5.6.Final (misma versión de la práctica 3) | JGroups |

Requisitos de la PC: **PHP 8.2 o superior** (XAMPP 8.2+), Node 18+, JDK 17+.


## ¿Qué camino uso? (decidirlo al empezar el examen)

| Situación | Camino |
|---|---|
| El docente **deja usar internet unos minutos** (o activar una caché) para instalar | **Camino A**: instalar normal, **una sola vez**, y después copiar esa carpeta para cada proyecto |
| Solo hay internet para clonar | **Camino B**: usar los zips de esta carpeta (abajo, "Paso a paso en el examen") |
| No sé / se cortó el internet a la mitad | Camino B. Los zips funcionan siempre |

> Las **migraciones, seeders, `make:model`, `serve`** y todo `php artisan` **no necesitan internet** en ningún camino. Lo único que descarga es `composer create-project`, `composer require`, `composer install/update`, `laravel new` y `npm install`.

### Camino A · con internet permitido (lo que haría el docente)
Hazlo **al inicio**, antes de que corten el internet, y para **todos** los proyectos que vas a necesitar de una vez.

**Paso A1.** Laravel base (unos 2 a 5 minutos):
```bat
mkdir C:\examen\bases
cd C:\examen\bases
composer create-project laravel/laravel:^12.0 base-laravel
cd base-laravel
php artisan install:api
composer require firebase/php-jwt webonyx/graphql-php
```
Si `install:api` pregunta por migrar, responde **no** (todavía no creaste la base).

**Paso A2.** Node base:
```bat
mkdir C:\examen\bases\base-node
cd C:\examen\bases\base-node
npm init -y
npm install express@4 express-graphql@0.12.0 graphql@15 mysql2 sequelize cors
npm install -D nodemon
```

**Paso A3.** Ya sin internet, **cada proyecto nuevo es una copia** de la base (no vuelvas a correr `composer create-project` por cada uno):
```bat
xcopy /E /I /Q C:\examen\bases\base-laravel C:\examen\central-rest
xcopy /E /I /Q C:\examen\bases\base-node C:\examen\central-graphql
```
Luego, en cada Laravel copiado: `.env` → `DB_DATABASE=...`, crear la base en phpMyAdmin y seguir con `make:model`, migraciones, etc.

**Paso A4 (si cortan el internet antes de terminar).** npm guarda lo que bajó en su caché. Para otro proyecto Node:
```bat
npm install --offline express@4 express-graphql@0.12.0 graphql@15 mysql2 sequelize cors
```
Composer no tiene un modo así de seguro: si a Laravel le falta algo, pasa al **Camino B** (`nuevo-laravel.bat`).

**Qué decirle al docente:** "¿Podemos usar internet 5 minutos solo para `composer create-project` y `npm install`? Las migraciones y el código los hacemos sin internet." Si dice que no → Camino B.

## Paso a paso en el examen (Camino B · sin internet)

**Paso 1.** Clonar el repositorio (lo único que necesita internet):
```bat
cd C:\
git clone https://github.com/erick22git/repositorio C:\guia
```

**Paso 2.** Crear un proyecto Laravel (no hace falta `laravel new` ni `composer create-project`):
```bat
C:\guia\00-preparacion\sin-internet\nuevo-laravel.bat central-rest
cd C:\examen\central-rest
```
A mano es lo mismo: clic derecho en `base-laravel.zip` → *Extraer todo* → en `C:\examen` → renombrar la carpeta.

> Extrae **en `C:\examen`** y no dentro de la carpeta del repositorio: Windows falla con rutas de más de 260 letras y `vendor/` tiene rutas largas.

**Paso 3.** En `.env` cambia solo `DB_DATABASE=bd_examen` por el nombre de tu base y créala en phpMyAdmin.

**Paso 4.** Sigue la plantilla o el ejercicio **desde `php artisan install:api`**. Funciona sin internet porque Sanctum ya está instalado (si al final muestra un error de migración, es porque aún no existe la base: créala y corre `php artisan migrate`).
- **No** ejecutes `composer require firebase/php-jwt`: ya está.
- **No** ejecutes `composer install` ni `composer update`: intentan descargar.

**Paso 5.** Node (GraphQL o REST):
```bat
C:\guia\00-preparacion\sin-internet\nuevo-node.bat central-graphql
cd C:\examen\central-graphql
REM pegar server.js, schema.js, database.js
npm run dev
```
**No** ejecutes `npm install`: `node_modules` ya está.

**Paso 6.** JGroups:
```bat
javac -cp C:\guia\00-preparacion\sin-internet\libs\jgroups-5.5.6.Final.jar -d out *.java
java -Djava.net.preferIPv4Stack=true -cp "out;C:\guia\00-preparacion\sin-internet\libs\jgroups-5.5.6.Final.jar" NodoPuerta puerta1 50
```
En NetBeans: *Libraries → Add JAR/Folder* → elegir ese jar. Con Maven offline el jar no se descarga: usa NetBeans con Ant o la consola.

## Lo que NO se puede sin internet (y qué hacer)

| Quería… | No funciona porque… | Haz esto |
|---|---|---|
| `composer require` de otra librería | Necesita Packagist | Usa lo que ya trae la base (JWT, GraphQL, Http) |
| Lighthouse (`@all`, `schema.graphql`) | No viene en la base | GraphQL en Laravel con **webonyx** → [plantilla 14](../../plantillas/14-graphql-laravel/README.md) |
| SOAP en Java con JAX-WS (`@WebService`) | Desde Java 11 no viene en el JDK y sus jar no están | SOAP en Java **sin librerías** → [plantilla 15](../../plantillas/15-soap-java/README.md) |
| Java contra MySQL (JDBC) | Falta el jar `mysql-connector-j` | Que Java llame a la API REST (como `ServidorPagoMultas`) o datos en memoria |
| Maven `mvn compile` en un proyecto nuevo | Descarga plugins | NetBeans *Java with Ant* o `javac` en consola |
| NuGet en C# (.NET moderno) | Descarga paquetes | **.NET Framework 4.7.2**: `WebClient`, `JavaScriptSerializer` y referencias de servicio no necesitan NuGet |

## Cómo se armaron (por si hay que rehacerlos con internet)
```bat
REM Laravel (con internet)
composer create-project laravel/laravel:^12.0 base-laravel
cd base-laravel
composer require firebase/php-jwt webonyx/graphql-php laravel/sanctum
REM Node (con internet)
npm init -y
npm install express@4 express-graphql@0.12.0 graphql@15 mysql2 sequelize cors
npm install -D nodemon
```
`graphql@15` y `express@4` van **con versión**: `express-graphql` no funciona con `graphql` 16 (npm da `ERESOLVE`).
