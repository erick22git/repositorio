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
            $decoded = JWT::decode((string) $request->bearerToken(), new Key(env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256')));
        } catch (\Exception $e) {
            return response()->json(['mensaje' => 'Token invalido o ausente'], 401);
        }

        $request->attributes->add(['usuario' => $decoded->data]);
        return $next($request);
    }
}
