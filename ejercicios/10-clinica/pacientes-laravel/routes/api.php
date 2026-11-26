<?php

use App\Http\Controllers\LoginController;
use App\Http\Controllers\PacienteController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);

Route::apiResource('pacientes', PacienteController::class)
    ->parameters(['pacientes' => 'ci'])
    ->middleware(JwtMiddleware::class);
