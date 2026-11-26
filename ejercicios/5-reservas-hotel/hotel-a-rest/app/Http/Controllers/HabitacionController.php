<?php

namespace App\Http\Controllers;

use App\Models\Habitacion;
use Illuminate\Http\Request;

class HabitacionController extends Controller
{
    public function index()
    {
        return response()->json(Habitacion::all());
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'codHabitacion' => 'required|string',
            'tipo'          => 'required|string',
            'capacidad'     => 'required|integer|min:1',
            'tarifa'        => 'required|numeric|gt:0',
        ]);

        if (Habitacion::where('codHabitacion', $datos['codHabitacion'])->exists()) {
            return response()->json(['error' => 'La habitacion ya existe'], 409);
        }

        return response()->json(Habitacion::create($datos)->refresh(), 201);
    }

    public function show(Habitacion $habitacion)
    {
        return response()->json($habitacion);
    }

    // PATCH /api/habitaciones/{cod}   { "disponible": false }
    public function update(Request $request, Habitacion $habitacion)
    {
        $datos = $request->validate([
            'tipo'       => 'sometimes|string',
            'capacidad'  => 'sometimes|integer|min:1',
            'tarifa'     => 'sometimes|numeric|gt:0',
            'disponible' => 'sometimes|boolean',
        ]);

        $habitacion->update($datos);
        return response()->json($habitacion);
    }

    public function destroy(Habitacion $habitacion)
    {
        $habitacion->delete();
        return response()->noContent();
    }
}
