<?php

use App\Http\Controllers\LoginController;
use App\Http\Controllers\RecursoController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);   // libre

Route::apiResource('recursos', RecursoController::class)    // protegido
    ->parameters(['recursos' => 'recurso'])
    ->middleware(JwtMiddleware::class);
