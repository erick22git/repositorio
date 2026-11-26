<?php

namespace App\Http\Controllers;

use App\Models\Candidato;
use Illuminate\Http\Request;

class CandidatoController extends Controller
{
    public function index(Request $request)
    {
        $consulta = Candidato::query();
        if ($request->filled('cargo')) {
            $consulta->where('cargo', $request->cargo);
        }
        return response()->json($consulta->get(), 200);
    }

    public function store(Request $request)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $datos = $request->validate([
            'nombres' => 'required|string',
            'apellidos' => 'required|string',
            'cargo' => 'required|in:Rector,ViceRector,Decano,Director',
            'ambito' => 'required|string',
        ]);
        $candidato = Candidato::create($datos);
        return response()->json($candidato, 201);
    }

    public function show(Candidato $candidato)
    {
        return response()->json($candidato, 200);
    }

    public function update(Request $request, Candidato $candidato)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $candidato->update($request->only(['nombres', 'apellidos', 'cargo', 'ambito']));
        return response()->json($candidato, 200);
    }

    public function destroy(Request $request, Candidato $candidato)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $candidato->delete();
        return response()->json($candidato, 204);
    }
}
