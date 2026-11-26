<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Cuenta extends Model
{
    use HasFactory;

    protected $table = 'cuentas';
    protected $fillable = ['nrocuenta', 'nombre', 'moneda', 'saldo', 'activo'];

    // /api/cuentas/12345 busca por nrocuenta y no por id
    public function getRouteKeyName(): string
    {
        return 'nrocuenta';
    }
}
