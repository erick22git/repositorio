<?php

use App\Http\Controllers\RecursoController;
use Illuminate\Support\Facades\Route;

// Todas estas rutas empiezan con /api
// parameters(...) es OBLIGATORIO en espanol: sin eso Laravel puede inventar
// {multum} o {cuentum} y el controlador recibe un registro vacio.
Route::apiResource('recursos', RecursoController::class)
    ->parameters(['recursos' => 'recurso']);
