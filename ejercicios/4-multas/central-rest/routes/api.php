<?php

use App\Http\Controllers\MultaController;
use Illuminate\Support\Facades\Route;

// parameters() evita que Laravel genere {multum}
Route::apiResource('multas', MultaController::class)
    ->parameters(['multas' => 'multa']);
