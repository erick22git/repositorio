<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Habitacion extends Model
{
    use HasFactory;

    protected $table = 'habitaciones';   // sin esto Laravel buscaria "habitacions"
    protected $fillable = ['codHabitacion', 'tipo', 'capacidad', 'tarifa', 'disponible'];
    protected $casts = ['disponible' => 'boolean', 'tarifa' => 'float'];

    public function getRouteKeyName(): string
    {
        return 'codHabitacion';
    }
}
