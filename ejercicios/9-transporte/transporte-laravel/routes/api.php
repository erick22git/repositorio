<?php

use App\Http\Controllers\ApiController;
use App\Http\Controllers\AsientoController;
use App\Http\Controllers\SalidaController;
use App\Http\Middleware\TokenMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [ApiController::class, 'login']);

Route::get('/salidas', [SalidaController::class, 'index']);
Route::get('/salidas/{id}', [SalidaController::class, 'show']);

Route::middleware(TokenMiddleware::class)->group(function () {
    Route::post('/salidas', [SalidaController::class, 'store']);
    Route::put('/salidas/{id}', [SalidaController::class, 'update']);
    Route::delete('/salidas/{id}', [SalidaController::class, 'destroy']);
    Route::apiResource('asientos', AsientoController::class);
});
