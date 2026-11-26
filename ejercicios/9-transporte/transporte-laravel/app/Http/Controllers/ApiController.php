<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;


class ApiController extends Controller
{
    private static $Secreto = 'clave_terminal_2026';

    private static function base64url($texto)
    {
        return rtrim(strtr(base64_encode($texto), '+/', '-_'), '=');
    }

    private static function creartoken($email)
    {
        $h = self::base64url(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
        $p = self::base64url(json_encode(['email' => $email, 'iat' => time(), 'exp' => time() + 3600]));
        $f = self::base64url(hash_hmac('sha256', "$h.$p", self::$Secreto, true));

        return "$h.$p.$f";
    }


    public static function verificartoken($token)
    {
        $partes = explode('.', (string) $token);
        if (count($partes) !== 3) return null;
        [$h, $p, $f] = $partes;
        $esperada = self::base64url(hash_hmac('sha256', "$h.$p", self::$Secreto, true));
        if (!hash_equals($esperada, $f)) return null;
        $payload = json_decode(base64_decode(strtr($p, '-_', '+/')), true);
        if (!$payload || ($payload['exp'] ?? 0) < time()) return null;
        return $payload;
    }


    public function login(Request $request)
    {
        $request->validate(['email' => 'required|email', 'password' => 'required']);
        $usuario = User::where('email', $request->email)->first();
        if (!$usuario || !Hash::check($request->password, $usuario->password)) {
            return response()->json(['mensaje' => 'Credenciales invalidas'], 401);
        }
        return response()->json(['token' => self::creartoken($usuario->email), 'type' => 'bearer'], 200);
    }
}
