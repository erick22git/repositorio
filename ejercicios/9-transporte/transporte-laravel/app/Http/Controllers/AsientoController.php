<?php

namespace App\Http\Controllers;

use App\Models\Asiento;
use App\Models\Salida;
use Illuminate\Http\Request;

class AsientoController extends Controller
{
    public function index(Request $request)
    {
        $consulta = Asiento::query()->orderBy('numero_asiento');
        if ($request->filled('salida_id')) {
            $consulta->where('salida_id', $request->salida_id);
        }
        return response()->json($consulta->get(), 200);
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'salida_id'      => 'required|integer|exists:salidas,id',
            'numero_asiento' => 'required|integer|min:1',
            'ci_pasajero'    => 'required|string|max:20',
        ]);

        $salida = Salida::find($datos['salida_id']);
        if ($datos['numero_asiento'] > $salida->capacidad) {
            return response()->json(['mensaje' => "El bus solo tiene {$salida->capacidad} asientos"], 422);
        }
        if ($salida->asientos()->count() >= $salida->capacidad) {
            return response()->json(['mensaje' => 'La salida ya esta llena'], 422);
        }
        $ocupado = Asiento::where('salida_id', $salida->id)->where('numero_asiento', $datos['numero_asiento'])->exists();
        if ($ocupado) {
            return response()->json(['mensaje' => 'El asiento ya esta vendido'], 409);
        }

        return response()->json(Asiento::create($datos), 201);
    }

    public function show($id)
    {
        $asiento = Asiento::with('salida')->find($id);
        if (!$asiento) {
            return response()->json(['mensaje' => 'Asiento no encontrado'], 404);
        }
        return response()->json($asiento, 200);
    }

    public function update(Request $request, $id)
    {
        $asiento = Asiento::find($id);
        if (!$asiento) {
            return response()->json(['mensaje' => 'Asiento no encontrado'], 404);
        }
        $datos = $request->validate(['ci_pasajero' => 'required|string|max:20']);
        $asiento->update($datos);
        return response()->json($asiento, 200);
    }

    public function destroy($id)
    {
        $asiento = Asiento::find($id);
        if (!$asiento) {
            return response()->json(['mensaje' => 'Asiento no encontrado'], 404);
        }
        $asiento->delete();
        return response()->json(['mensaje' => 'Venta anulada'], 200);
    }
}
