<?php

// Valida el token que emitio el servicio ATC (mismo JWT_SECRET)
namespace App\Http\Middleware;

use App\Support\JwtSimple;
use Closure;
use Illuminate\Http\Request;

class JwtMiddleware
{
    public function handle(Request $request, Closure $next)
    {
        $jwt = str_replace('Bearer ', '', $request->header('Authorization', ''));
        $payload = JwtSimple::verificar($jwt, env('JWT_SECRET'));

        if ($payload === null) {
            return response()->json(['error' => 'Token invalido o ausente'], 401);
        }

        $request->attributes->add(['usuario' => $payload['data']]);
        return $next($request);
    }
}
