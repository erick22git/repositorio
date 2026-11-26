<?php

namespace App\Http\Controllers;

use App\Models\Recurso;
use Illuminate\Http\Request;

class RecursoController extends Controller
{
    // GET /api/recursos
    public function index()
    {
        // AQUI LOGICA (filtros)
        return response()->json(Recurso::all());
    }

    // POST /api/recursos
    public function store(Request $request)
    {
        // Si algo no cumple, responde 422 solo
        $datos = $request->validate([
            'codigo' => 'required|string',
            'nombre' => 'required|string',
            'monto'  => 'required|numeric',
        ]);

        if (Recurso::where('codigo', $datos['codigo'])->exists()) {
            return response()->json(['error' => 'El codigo ya existe'], 409);
        }

        // AQUI LOGICA (reglas antes de guardar, llamar a otros servicios)

        $recurso = Recurso::create($datos);
        return response()->json($recurso->refresh(), 201);
    }

    // GET /api/recursos/{recurso}   (si no existe responde 404 solo)
    public function show(Recurso $recurso)
    {
        // AQUI LOGICA
        return response()->json($recurso);
    }

    // PUT o PATCH /api/recursos/{recurso}
    public function update(Request $request, Recurso $recurso)
    {
        $datos = $request->validate([
            'nombre' => 'sometimes|string',
            'monto'  => 'sometimes|numeric',
            'estado' => 'sometimes|string',
        ]);

        // AQUI LOGICA

        $recurso->update($datos);
        return response()->json($recurso);
    }

    // DELETE /api/recursos/{recurso}
    public function destroy(Recurso $recurso)
    {
        // AQUI LOGICA (si piden borrado logico, ver la carpeta logica/)
        $recurso->delete();
        return response()->noContent();   // 204
    }
}
