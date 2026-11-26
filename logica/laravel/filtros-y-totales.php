<?php
// GET /api/recursos?estado=activo&min=50
public function index(Request $request)
{
    $q = Recurso::query();
    if ($request->filled('estado')) $q->where('estado', $request->query('estado'));
    if ($request->filled('min'))    $q->where('monto', '>=', $request->query('min'));
    return response()->json($q->orderBy('monto')->get());
}

// Ruta aparte (ponerla ANTES del apiResource):  Route::get('/recursos-total', [RecursoController::class, 'total']);
public function total()
{
    return response()->json([
        'cantidad' => Recurso::count(),
        'suma'     => Recurso::sum('monto'),
    ]);
}
