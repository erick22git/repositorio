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
        $request->validate(['email' => 'required', 'password' => 'required']);

        $user = User::where('email', $request->email)->first();
        if (!$user || !Hash::check($request->password, $user->password)) {
            return response()->json(['mensaje' => 'Credenciales invalidas'], 401);
        }

        $payload = [
            'iat'  => time(),
            'exp'  => time() + 3600,
            'data' => ['id' => $user->id, 'email' => $user->email],
        ];

        return response()->json([
            'token' => JWT::encode($payload, env('JWT_SECRET'), env('JWT_ALGORITHM', 'HS256')),
            'type'  => 'bearer',
        ]);
    }
}
