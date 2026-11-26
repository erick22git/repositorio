<?php

use App\Http\Controllers\CuentaController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::apiResource('cuentas', CuentaController::class)
    ->parameters(['cuentas' => 'cuenta'])
    ->middleware(JwtMiddleware::class);
