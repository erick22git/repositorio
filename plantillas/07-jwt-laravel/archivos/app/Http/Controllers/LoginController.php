<?php

namespace App\Http\Controllers;

use App\Models\User;
use Firebase\JWT\JWT;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class LoginController extends Controller
{
    // POST /api/login   { "email": "...", "password": "..." }
    public function login(Request $request)
    {
        $request->validate(['email' => 'required', 'password' => 'required']);

        $user = User::where('email', $request->email)->first();
        if (!$user || !Hash::check($request->password, $user->password)) {
            return response()->json(['error' => 'Credenciales invalidas'], 401);
        }

        $payload = [
            'iat'  => time(),            // cuando se creo
            'exp'  => time() + 3600,     // vence en 1 hora
            'data' => ['id' => $user->id, 'email' => $user->email],
        ];

        $token = JWT::encode($payload, env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256'));

        return response()->json(['token' => $token, 'type' => 'bearer']);
    }
}
