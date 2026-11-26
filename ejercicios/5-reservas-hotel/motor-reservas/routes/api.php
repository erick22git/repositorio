<?php

use App\Http\Controllers\LoginController;
use App\Http\Controllers\ReservaController;
use App\Http\Middleware\JwtMiddleware;
use Illuminate\Support\Facades\Route;

Route::post('/login', [LoginController::class, 'login']);      // libre

Route::middleware(JwtMiddleware::class)->group(function () {   // con token
    Route::get('/reserva', [ReservaController::class, 'index']);
    Route::post('/reserva', [ReservaController::class, 'store']);
});
