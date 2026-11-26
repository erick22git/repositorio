<?php

use App\Http\Controllers\HabitacionController;
use Illuminate\Support\Facades\Route;

// parameters() es obligatorio: Laravel creeria que el singular es "habitacione"
Route::apiResource('habitaciones', HabitacionController::class)
    ->parameters(['habitaciones' => 'habitacion']);
