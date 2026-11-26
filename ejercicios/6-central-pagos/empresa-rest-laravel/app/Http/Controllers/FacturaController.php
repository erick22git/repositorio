<?php

namespace App\Http\Controllers;

use App\Models\Factura;
use Illuminate\Http\Request;

class FacturaController extends Controller
{
    // GET /api/facturas?ci=11021654   -> solo las pendientes de ese CI
    public function index(Request $request)
    {
        $consulta = Factura::where('estado', 'pendiente');
        if ($request->filled('ci')) {
            $consulta->where('ci', $request->query('ci'));
        }
        return response()->json($consulta->get());
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'ci'      => 'required|digits_between:5,10',
            'periodo' => 'required|string',
            'monto'   => 'required|numeric|gt:0',
        ]);
        return response()->json(Factura::create($datos)->refresh(), 201);
    }

    public function show(Factura $factura)
    {
        return response()->json($factura);
    }

    // PATCH /api/facturas/{id}   { "estado": "pagada" }
    public function update(Request $request, Factura $factura)
    {
        $request->validate(['estado' => 'required|in:pagada']);

        if ($factura->estado === 'pagada') {
            return response()->json(['error' => 'La factura ya fue pagada'], 409);
        }

        $factura->update(['estado' => 'pagada']);
        return response()->json($factura);
    }

    public function destroy(Factura $factura)
    {
        $factura->delete();
        return response()->noContent();
    }
}
