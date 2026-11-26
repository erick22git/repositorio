<?php

namespace App\Http\Controllers;

use App\Models\Mesa;
use Illuminate\Http\Request;

class MesaController extends Controller
{
    public function index()
    {
        $mesas = Mesa::with('acta')->orderBy('numero')->get();
        return response()->json($mesas, 200);
    }

    public function store(Request $request)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $datos = $request->validate([
            'numero' => 'required|integer|min:1|unique:mesas,numero',
            'recinto' => 'required|string',
            'habilitados' => 'required|integer|min:1',
        ]);
        return response()->json(Mesa::create($datos), 201);
    }

    public function show(Mesa $mesa)
    {
        return response()->json($mesa->load('acta.votos.candidato'), 200);
    }
}
