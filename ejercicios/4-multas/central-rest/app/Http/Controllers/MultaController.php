<?php

namespace App\Http\Controllers;

use App\Models\Multa;
use Illuminate\Http\Request;

class MultaController extends Controller
{
    // GET /api/multas        GET /api/multas?ci=11021654
    public function index(Request $request)
    {
        $consulta = Multa::query();
        if ($request->filled('ci')) {
            $consulta->where('ci', $request->query('ci'));
        }
        return response()->json($consulta->get());
    }

    // POST /api/multas   (lo usa la Policia)
    public function store(Request $request)
    {
        $datos = $request->validate([
            'codigo' => 'required|string',
            'ci'     => 'required|digits_between:5,10',
            'nombre' => 'required|string',
            'fecha'  => 'required|date',
            'tipo'   => 'required|string',
            'lugar'  => 'required|string',
            'monto'  => 'required|numeric|gt:0',
        ]);

        if (Multa::where('codigo', $datos['codigo'])->exists()) {
            return response()->json(['error' => 'Ya existe una boleta con ese codigo'], 409);
        }

        // Toda multa nace pendiente (no se acepta el estado desde afuera)
        $multa = Multa::create($datos);
        return response()->json($multa->refresh(), 201);
    }

    // GET /api/multas/{codigo}
    public function show(Multa $multa)
    {
        return response()->json($multa);
    }

    // PATCH /api/multas/{codigo}   { "estado": "pagada" | "anulada" }
    public function update(Request $request, Multa $multa)
    {
        $datos = $request->validate(['estado' => 'required|in:pagada,anulada']);

        // Solo una multa pendiente puede cambiar
        if ($multa->estado !== 'pendiente') {
            return response()->json(['error' => 'La multa ya esta ' . $multa->estado], 409);
        }

        $multa->update($datos);
        return response()->json($multa);
    }

    // DELETE = anular (borrado logico)
    public function destroy(Multa $multa)
    {
        if ($multa->estado !== 'pendiente') {
            return response()->json(['error' => 'La multa ya esta ' . $multa->estado], 409);
        }
        $multa->update(['estado' => 'anulada']);
        return response()->noContent();
    }
}
