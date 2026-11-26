<?php

namespace App\Http\Middleware;

use App\Http\Controllers\ApiController;
use Closure;
use Illuminate\Http\Request;

class TokenMiddleware
{
    public function handle(Request $request, Closure $next)
    {
        $payload = ApiController::verificartoken($request->bearerToken());
        if ($payload === null) {
            return response()->json(['mensaje' => 'Token invalido o ausente'], 401);
        }
        $request->attributes->add(['usuario' => $payload['email']]);
        return $next($request);
    }
}
