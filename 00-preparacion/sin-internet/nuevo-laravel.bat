@echo off
REM uso:  nuevo-laravel.bat nombre-del-proyecto      (crea C:\examen\nombre-del-proyecto)
if "%~1"=="" (echo Uso: nuevo-laravel.bat nombre & exit /b 1)
if not exist C:\examen mkdir C:\examen
if exist "C:\examen\%~1" (echo Ya existe C:\examen\%~1 & exit /b 1)
cd /d C:\examen
tar -xf "%~dp0base-laravel.zip"
ren base-laravel "%~1"
echo.
echo Listo: C:\examen\%~1
echo Ahora: abre .env y cambia DB_DATABASE, crea esa base en phpMyAdmin y sigue la plantilla.
