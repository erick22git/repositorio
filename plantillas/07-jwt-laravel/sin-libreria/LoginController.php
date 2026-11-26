<?php

// Version SIN libreria. Reemplaza a app/Http/Controllers/LoginController.php
namespace App\Http\Controllers;

use App\Models\User;
use App\Support\JwtSimple;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;

class LoginController extends Controller
{
    public function login(Request $request)
    {
        $request->validate(['email' => 'required', 'password' => 'required']);

        $user = User::where('email', $request->email)->first();
        if (!$user || !Hash::check($request->password, $user->password)) {
            return response()->json(['error' => 'Credenciales invalidas'], 401);
        }

        $token = JwtSimple::crear(['id' => $user->id, 'email' => $user->email], env('JWT_SECRET'));
        return response()->json(['token' => $token, 'type' => 'bearer']);
    }
}
