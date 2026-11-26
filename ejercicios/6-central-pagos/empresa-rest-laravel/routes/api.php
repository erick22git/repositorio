<?php

use App\Http\Controllers\FacturaController;
use Illuminate\Support\Facades\Route;

Route::apiResource('facturas', FacturaController::class)
    ->parameters(['facturas' => 'factura']);
