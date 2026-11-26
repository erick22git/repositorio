<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Materia extends Model
{
    protected $table = 'materias';
    protected $fillable = ['sigla', 'nombre', 'docente', 'horario', 'cupo'];

    protected function casts(): array
    {
        return ['cupo' => 'integer'];
    }

    public function inscripciones(): HasMany
    {
        return $this->hasMany(Inscripcion::class, 'materia_id');
    }
}
