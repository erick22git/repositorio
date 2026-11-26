<?php

use App\Http\Controllers\GraphQLController;
use Illuminate\Support\Facades\Route;

// http://localhost:8000/api/graphql
Route::post('/graphql', GraphQLController::class);
