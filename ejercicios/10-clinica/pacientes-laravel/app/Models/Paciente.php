<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Paciente extends Model
{
    protected $table = 'pacientes';
    protected $fillable = ['ci', 'nombres', 'apellidos', 'telefono', 'seguro'];

    protected function casts(): array
    {
        return ['seguro' => 'boolean'];
    }
}
