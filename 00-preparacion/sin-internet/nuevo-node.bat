@echo off
REM uso:  nuevo-node.bat nombre-del-proyecto      (crea C:\examen\nombre-del-proyecto)
if "%~1"=="" (echo Uso: nuevo-node.bat nombre & exit /b 1)
if not exist C:\examen mkdir C:\examen
if exist "C:\examen\%~1" (echo Ya existe C:\examen\%~1 & exit /b 1)
cd /d C:\examen
tar -xf "%~dp0base-node.zip"
ren base-node "%~1"
echo.
echo Listo: C:\examen\%~1
echo Ahora: pega server.js, schema.js, database.js y ejecuta  npm run dev
