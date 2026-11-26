@echo off
REM  EJECUTAR UNA SOLA VEZ, CON INTERNET, ANTES DEL EXAMEN.  [EXTRA]
REM  Deja en C:\examen\bases un proyecto Laravel y uno de Node ya descargados.

mkdir C:\examen\bases
cd /d C:\examen\bases

REM --- Laravel 12 (la version del repo del curso) con rutas API y JWT
call composer create-project laravel/laravel:^12.0 base-laravel
cd base-laravel
call php artisan install:api --no-interaction
call composer require firebase/php-jwt
cd ..

REM --- Node con las dependencias de GraphQL (comandos de la hoja del docente)
mkdir base-graphql
cd base-graphql
call npm init -y
call npm install express express-graphql graphql mysql2 sequelize
call npm install -D nodemon
cd ..

echo.
echo LISTO. Revisa que no haya errores arriba.
pause
