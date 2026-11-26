<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class Voto extends Model
{
    protected $fillable = ['acta_id', 'candidato_id', 'cantidad'];

    public function candidato()
    {
        return $this->belongsTo(Candidato::class);
    }
}
