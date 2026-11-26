<?php

use App\Http\Controllers\GraphQLController;
use Illuminate\Support\Facades\Route;

Route::post('/graphql', GraphQLController::class);
