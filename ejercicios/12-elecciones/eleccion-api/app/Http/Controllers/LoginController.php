<?php

namespace App\Http\Controllers;

use App\Models\User;
use Firebase\JWT\JWT;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class LoginController extends Controller
{
    public function login(Request $request)
    {
        $usuario = User::where('email', $request->email)->first();
        if ($usuario == null) {
            return response()->json(['mensaje' => 'usuario invalido'], 400);
        }
        if (!Hash::check($request->password, $usuario->password)) {
            return response()->json(['mensaje' => 'Contraseña invalida'], 400);
        }

        $key = env('JWT_SECRET');
        $algoritmo = env('JWT_ALGORITHM');
        $time = time();
        $payload = [
            'iat' => $time,
            'exp' => $time + (60 * 60),
            'data' => [
                'user_id' => $usuario->id,
                'email' => $usuario->email,
                'name' => $usuario->name,
                'role' => $usuario->role
            ],
        ];
        $jwt = JWT::encode($payload, $key, $algoritmo);

        return response()->json([
            'mensaje' => 'Se logró autenticar al usuario',
            'token' => $jwt,
            'type' => 'bearer',
            'expires' => $time + (60 * 60),
            'usuario' => $usuario
        ], 200);
    }
}
