<?php

namespace App\Http\Middleware;

use Closure;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Illuminate\Http\Request;

class JwtMiddleware
{
    public function handle(Request $request, Closure $next)
    {
        try {
            $autorizacion = $request->header('Authorization');
            $jwt = substr($autorizacion, 7);
            $key = env('JWT_SECRET');
            $algoritmo = env('JWT_ALGORITHM');
            $datos = JWT::decode($jwt, new Key($key, $algoritmo));
            $request->attributes->add(['user' => $datos->data]);
        } catch (\Exception $e) {
            return response()->json(['status' => 'No autorizado ' . $e->getMessage()], 401);
        }
        return $next($request);
    }
}
