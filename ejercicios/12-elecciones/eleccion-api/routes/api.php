<?php

use App\Http\Controllers\ActaController;
use App\Http\Controllers\CandidatoController;
use App\Http\Controllers\LoginController;
use App\Http\Controllers\MesaController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);

Route::apiResource('candidatos', CandidatoController::class)->middleware(JwtMiddleware::class);
Route::apiResource('mesas', MesaController::class)->only(['index', 'store', 'show'])->middleware(JwtMiddleware::class);
Route::apiResource('actas', ActaController::class)->except(['update'])->middleware(JwtMiddleware::class);
