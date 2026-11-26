<?php
// Ejemplo: un recurso tiene muchos movimientos
// php artisan make:model Movimiento -msfc --api

// 1) Migracion de movimientos
$table->id();
$table->foreignId('recurso_id')->constrained('recursos')->cascadeOnDelete();
$table->decimal('monto', 10, 2);
$table->timestamps();

// 2) Modelo Movimiento
protected $table = 'movimientos';
protected $fillable = ['recurso_id', 'monto'];

// 3) Modelo Recurso
public function movimientos()
{
    return $this->hasMany(Movimiento::class);
}

// 4) En RecursoController@show : devolver el recurso CON sus movimientos
public function show(Recurso $recurso)
{
    return response()->json($recurso->load('movimientos'));
}
