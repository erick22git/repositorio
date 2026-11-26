<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Cotizacion extends Model
{
    protected $table = 'cotizaciones';
    protected $fillable = ['fecha', 'cotizacion', 'cotizacion_oficial'];

    protected function casts(): array
    {
        return ['cotizacion' => 'float', 'cotizacion_oficial' => 'float'];
    }
}
