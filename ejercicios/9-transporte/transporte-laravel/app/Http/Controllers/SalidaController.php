<?php

namespace App\Http\Controllers;

use App\Models\Salida;
use Illuminate\Http\Request;

class SalidaController extends Controller
{
    public function index(Request $request)
    {
        $consulta = Salida::withCount('asientos')->orderBy('fecha')->orderBy('hora');
        if ($request->filled('destino')) {
            $consulta->where('destino', $request->destino);
        }
        return response()->json($consulta->get(), 200);
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'destino'   => 'required|string|max:100',
            'fecha'     => 'required|date',
            'hora'      => 'required|date_format:H:i',
            'bus'       => 'required|string|max:20',
            'precio'    => 'required|numeric|min:0',
            'capacidad' => 'required|integer|min:1',
        ]);
        return response()->json(Salida::create($datos), 201);
    }

    public function show($id)
    {
        $salida = Salida::with('asientos')->find($id);
        if (!$salida) {
            return response()->json(['mensaje' => 'Salida no encontrada'], 404);
        }
        $salida->disponibles = $salida->capacidad - $salida->asientos->count();
        return response()->json($salida, 200);
    }

    public function update(Request $request, $id)
    {
        $salida = Salida::find($id);
        if (!$salida) {
            return response()->json(['mensaje' => 'Salida no encontrada'], 404);
        }
        $datos = $request->validate([
            'destino'   => 'sometimes|string|max:100',
            'fecha'     => 'sometimes|date',
            'hora'      => 'sometimes|date_format:H:i',
            'bus'       => 'sometimes|string|max:20',
            'precio'    => 'sometimes|numeric|min:0',
            'capacidad' => 'sometimes|integer|min:1',
        ]);
        if (isset($datos['capacidad']) && $datos['capacidad'] < $salida->asientos()->count()) {
            return response()->json(['mensaje' => 'Ya hay mas asientos vendidos que esa capacidad'], 422);
        }
        $salida->update($datos);
        return response()->json($salida, 200);
    }

    public function destroy($id)
    {
        $salida = Salida::find($id);
        if (!$salida) {
            return response()->json(['mensaje' => 'Salida no encontrada'], 404);
        }
        $salida->asientos()->delete();
        $salida->delete();
        return response()->json(['mensaje' => 'Salida eliminada'], 200);
    }
}
