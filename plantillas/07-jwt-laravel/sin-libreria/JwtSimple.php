<?php

// JWT a mano (HS256), por si NO se puede instalar firebase/php-jwt.
// Copiar en app/Support/JwtSimple.php
namespace App\Support;

class JwtSimple
{
    private static function b64(string $texto): string
    {
        return rtrim(strtr(base64_encode($texto), '+/', '-_'), '=');
    }

    // header.payload.firma
    public static function crear(array $data, string $secreto): string
    {
        $h = self::b64(json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
        $p = self::b64(json_encode(['iat' => time(), 'exp' => time() + 3600, 'data' => $data]));
        $f = self::b64(hash_hmac('sha256', "$h.$p", $secreto, true));
        return "$h.$p.$f";
    }

    // Devuelve el payload si el token es valido, o null si no
    public static function verificar(string $token, string $secreto): ?array
    {
        $partes = explode('.', $token);
        if (count($partes) !== 3) return null;

        [$h, $p, $f] = $partes;
        $esperada = self::b64(hash_hmac('sha256', "$h.$p", $secreto, true));
        if (!hash_equals($esperada, $f)) return null;

        $payload = json_decode(base64_decode(strtr($p, '-_', '+/')), true);
        if (!$payload || ($payload['exp'] ?? 0) < time()) return null;

        return $payload;
    }
}
