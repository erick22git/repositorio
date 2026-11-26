<?php

namespace App\Http\Middleware;

use Closure;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Illuminate\Http\Request;

class JwtMiddleware
{
    // Portero: sin token valido no se pasa
    public function handle(Request $request, Closure $next)
    {
        $jwt = str_replace('Bearer ', '', $request->header('Authorization', ''));

        try {
            $decoded = JWT::decode($jwt, new Key(env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256')));
        } catch (\Exception $e) {
            return response()->json(['error' => 'Token invalido o ausente'], 401);
        }

        $request->attributes->add(['usuario' => $decoded->data]);
        return $next($request);
    }
}
