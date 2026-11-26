<?php
// Opcion A (a mano). En la migracion:
$table->boolean('activo')->default(true);
// En $fillable agregar 'activo'. En el controlador:
public function index()
{
    return response()->json(Recurso::where('activo', true)->get());
}

public function destroy(Recurso $recurso)
{
    $recurso->update(['activo' => false]);
    return response()->noContent();
}
