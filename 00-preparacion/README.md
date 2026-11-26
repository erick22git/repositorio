# 00 · Preparación (con internet y sin internet)

[← Volver al índice](../README.md)

## Índice
- [Sin internet: bases listas en el repositorio](sin-internet/README.md)
- [A. Qué instalar](#a-qué-instalar)
- [B. Antes del examen, con internet](#b-antes-del-examen-con-internet)
- [C. Comprobar que todo está](#c-comprobar-que-todo-está)
- [D. Durante el examen, sin internet](#d-durante-el-examen-sin-internet)
- [E. Dos computadoras en red](#e-dos-computadoras-en-red)

## A. Qué instalar

| Programa | Para qué | Ojo |
|---|---|---|
| JDK 21 y NetBeans (o VS Code con "Extension Pack for Java") | Sockets, RMI, JGroups, REST Java | |
| Visual Studio Community | SOAP y Windows Forms | En Visual Studio Installer marca **Desarrollo de ASP.NET y web** y **Desarrollo de escritorio de .NET**. Si no aparecen las plantillas "(.NET Framework)", marca en componentes las de .NET Framework |
| XAMPP | MySQL y PHP | Encender **MySQL** en el panel |
| Composer | Laravel | Al instalar, apuntar al PHP de XAMPP |
| Node.js | GraphQL | Trae `npm` |
| `jgroups-5.x.Final.jar` | JGroups | Ya viene en `00-preparacion/sin-internet/libs/`. O guardarlo en `C:\libs\jgroups\` |
| Postman / Insomnia / SoapUI | Probar servicios | Opcional |

En `C:\xampp\php\php.ini` quita el `;` de estas líneas: `extension=zip`, `extension=fileinfo`, `extension=pdo_mysql`, `extension=soap`.

> Laravel 12 pide PHP 8.2 o superior. Comprueba con `php -v` que sea el PHP que esperas.

## B. Antes del examen, con internet

> **Si el docente deja usar internet un rato en el examen**, sigue el [Camino A](sin-internet/README.md#camino-a--con-internet-permitido-lo-que-haría-el-docente): instalar una vez y copiar la carpeta para cada proyecto.
>
> **Ya no es obligatorio.** La carpeta [`sin-internet`](sin-internet/README.md) trae `base-laravel.zip` y `base-node.zip` con todas las dependencias descargadas y el jar de JGroups. Basta con clonar el repositorio. **[EXTRA]**
>
> Ojo si lo haces a mano: `graphql` y `express` van **con versión** (`graphql@15`, `express@4`). Sin versión npm instala `graphql` 16 y `express-graphql` falla con `ERESOLVE`.

La forma corta: doble clic en [`preparar-con-internet.bat`](preparar-con-internet.bat) **[EXTRA]**.

A mano:

```bat
REM 1) Proyecto Laravel base
composer create-project laravel/laravel:^12.0 C:\examen\bases\base-laravel
cd C:\examen\bases\base-laravel
php artisan install:api
composer require firebase/php-jwt

REM 2) Proyecto Node base   (comandos [INGE])
mkdir C:\examen\bases\base-graphql
cd C:\examen\bases\base-graphql
npm init -y
npm install express@4 express-graphql@0.12.0 graphql@15 mysql2 sequelize cors
npm install -D nodemon

REM 3) Instalador de Laravel de la hoja del docente [INGE] (para crear proyectos igual necesita internet)
composer global require laravel/installer
```

También descarga con internet:
- **JGroups**: el `.jar` desde Maven Central (buscar `org.jgroups jgroups`) → `C:\libs\jgroups\`.
- **Esta guía**: botón *Code → Download ZIP* y descomprimir en `C:\examen\guia`.

## C. Comprobar que todo está

```bat
java -version
javac -version
php -v
composer -V
node -v
npm -v
```

Ensayo obligatorio **con el WiFi apagado**: copia `base-laravel` y ejecuta `php artisan serve`; copia `base-graphql`, pega la [plantilla GraphQL](../plantillas/08-graphql-node/README.md) y ejecuta `npm run dev`; crea un sitio ASMX en Visual Studio y ejecútalo.

## D. Durante el examen, sin internet

### D.1 Base de datos (una por cada proyecto)
- **Paso 1.** XAMPP → **Start** en MySQL (y en Apache para usar phpMyAdmin).
- **Paso 2.** Navegador → `http://localhost/phpmyadmin` → **Nueva** → escribir el nombre (ej. `bd_examen`) → **Crear**.

> **Regla de oro: cada proyecto Laravel usa SU PROPIA base de datos.** `migrate:fresh` borra **todas** las tablas de la base.

### D.2 Proyecto Laravel nuevo
> **Atajo si no preparaste nada:** `C:\guia\00-preparacion\sin-internet\nuevo-laravel.bat mi-proyecto` deja en `C:\examen\mi-proyecto` un Laravel 12 con `vendor`, Sanctum, `firebase/php-jwt` y `webonyx/graphql-php` ya instalados y el `.env` listo. Ver [sin-internet](sin-internet/README.md).

- **Paso 1.** Copiar la carpeta `C:\examen\bases\base-laravel` completa (con `vendor`) y renombrarla (ej. `central-rest`).
- **Paso 2.** Abrirla en VS Code.
- **Paso 3.** Abrir `.env`, quitar los `#` y dejar así:

```ini
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=bd_examen
DB_USERNAME=root
DB_PASSWORD=

JWT_SECRET=clave_secreta_del_examen_2026_de_32_letras_o_mas
JWT_ALGORITHM=HS256
```

- **Paso 4.** Si no existe `.env`: copiar `.env.example` como `.env` y ejecutar `php artisan key:generate` **[INGE]**.
- **Paso 5.** Seguir la [plantilla REST](../plantillas/06-rest-laravel/README.md).

Con internet sería simplemente `laravel new <nombre>` **[INGE]**.

#### Si no preparaste la base
`laravel new` y `composer create-project` solo funcionan sin internet si Composer ya tenía todo en su caché, y no hay que contar con eso. Busca en la PC un proyecto Laravel viejo **que tenga la carpeta `vendor`** y copia ese.

#### Si `install:api` falla
Sin internet puede mostrar un error al intentar descargar Sanctum. Revisa si igual creó `routes/api.php`. Si no:
- **Paso 1.** Crear el archivo `routes/api.php`.
- **Paso 2.** En `bootstrap/app.php`, dentro de `->withRouting(`, agregar:

```php
        api: __DIR__.'/../routes/api.php',
```

- **Paso 3.** `php artisan route:list --path=api` debe mostrar tus rutas.

### D.3 Proyecto Node (GraphQL) nuevo
> **Atajo:** `C:\guia\00-preparacion\sin-internet\nuevo-node.bat mi-graphql` deja un proyecto con `express@4`, `express-graphql@0.12.0`, `graphql@15`, `mysql2`, `sequelize`, `cors` y `nodemon` en `C:\examen\mi-graphql`.

- **Paso 1.** Copiar `C:\examen\bases\base-graphql` y renombrarla.
- **Paso 2.** Pegar `server.js`, `schema.js`, `database.js`.
- **Paso 3.** En `package.json`, dentro de `"scripts"`: `"dev": "nodemon server.js"`.
- **Paso 4.** `npm run dev` → `http://localhost:4000/graphql`.

### D.4 Proyecto Java
**NetBeans**
- **Paso 1.** *File → New Project → Java with Ant → Java Application* (con Ant no pide internet). Desmarcar "Create Main Class".
- **Paso 2.** Clic derecho en *Source Packages → New → Java Class* con el **mismo nombre** que el archivo.
- **Paso 3.** Pegar todo el contenido. Si NetBeans puso una línea `package`, **déjala** arriba.
- **Paso 4.** Ejecutar cada clase con `Shift + F6`. **Servidor primero**, cliente después.
- Atajos: formatear `Alt + Shift + F`, arreglar imports `Ctrl + Shift + I`.

**VS Code:** abrir una carpeta vacía, crear los `.java` y pulsar *Run* encima de `main`.

**Consola [EXTRA]** (dentro de la carpeta de los `.java`):
```bat
javac -d out *.java
java -cp out ServidorTCP
REM en OTRA ventana:
java -cp out ClienteTCP
```

Para detener un servidor: botón rojo (o `Ctrl + C` en consola). Si no lo detienes, el siguiente dirá *Address already in use*.

### D.5 Agregar el jar de JGroups
- NetBeans: clic derecho en *Libraries → Add JAR/Folder* → elegir el jar.
- VS Code: panel *Java Projects → Referenced Libraries → +*.
- Consola: `javac -cp C:\libs\jgroups\* -d out *.java` y `java -cp "out;C:\libs\jgroups\*" Nodo ana`.

### D.6 SOAP y Windows Forms
Pasos en [plantilla 05](../plantillas/05-soap-csharp/README.md) y [plantilla 11](../plantillas/11-cliente-forms-csharp/README.md). No necesitan internet si la carga de trabajo ya está instalada.

## E. Dos computadoras en red

**[PRÁCTICA] / [EXTRA]**
1. `ipconfig` → anotar la **Dirección IPv4** de cada PC.
2. En el cliente cambiar `localhost` por la IP del servidor.
3. Abrir puertos en el firewall (símbolo del sistema **como administrador**):

```bat
netsh advfirewall firewall add rule name="Examen TCP" dir=in action=allow protocol=TCP localport=1099,1100,4000,5002,8000-8003,8083,8085
netsh advfirewall firewall add rule name="Examen UDP" dir=in action=allow protocol=UDP localport=6789
```

4. Laravel visible para otras PCs: `php artisan serve --host=0.0.0.0 --port=8000`.
5. RMI: en el servidor, activar la línea `java.rmi.server.hostname` con su IP. RMI usa además un puerto al azar: permite `java.exe` en el firewall.
