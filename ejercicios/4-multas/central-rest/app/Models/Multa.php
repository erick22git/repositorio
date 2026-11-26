<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Multa extends Model
{
    use HasFactory;

    protected $table = 'multas';
    protected $fillable = ['codigo', 'ci', 'nombre', 'fecha', 'tipo', 'lugar', 'monto', 'estado'];

    // /api/multas/B-001 busca por codigo de boleta y no por id
    public function getRouteKeyName(): string
    {
        return 'codigo';
    }
}
