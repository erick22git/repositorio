<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Recurso extends Model
{
    use HasFactory;

    protected $table = 'recursos';   // nombre EXACTO de la tabla en la migracion

    // Campos que se pueden guardar desde la API (los mismos de la migracion)
    protected $fillable = ['codigo', 'nombre', 'monto', 'estado'];
}
