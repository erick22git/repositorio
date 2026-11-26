<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Acta extends Model
{
    protected $fillable = ['mesa_id', 'cargo', 'blancos', 'nulos', 'registrado_por'];

    public function mesa()
    {
        return $this->belongsTo(Mesa::class);
    }

    public function votos()
    {
        return $this->hasMany(Voto::class);
    }
}
