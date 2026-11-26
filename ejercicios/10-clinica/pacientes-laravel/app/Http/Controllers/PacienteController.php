<?php

namespace App\Http\Controllers;

use App\Models\Paciente;
use Illuminate\Http\Request;

class PacienteController extends Controller
{
    public function index()
    {
        return response()->json(Paciente::orderBy('apellidos')->get(), 200);
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'ci'        => 'required|string|max:20|unique:pacientes,ci',
            'nombres'   => 'required|string|max:80',
            'apellidos' => 'required|string|max:80',
            'telefono'  => 'nullable|string|max:20',
            'seguro'    => 'boolean',
        ]);
        return response()->json(Paciente::create($datos), 201);
    }

    public function show($ci)
    {
        $paciente = Paciente::where('ci', $ci)->first();
        if (!$paciente) {
            return response()->json(['mensaje' => 'Paciente no registrado'], 404);
        }
        return response()->json($paciente, 200);
    }

    public function update(Request $request, $ci)
    {
        $paciente = Paciente::where('ci', $ci)->first();
        if (!$paciente) {
            return response()->json(['mensaje' => 'Paciente no registrado'], 404);
        }
        $datos = $request->validate([
            'nombres'   => 'sometimes|string|max:80',
            'apellidos' => 'sometimes|string|max:80',
            'telefono'  => 'nullable|string|max:20',
            'seguro'    => 'sometimes|boolean',
        ]);
        $paciente->update($datos);
        return response()->json($paciente, 200);
    }

    public function destroy($ci)
    {
        $paciente = Paciente::where('ci', $ci)->first();
        if (!$paciente) {
            return response()->json(['mensaje' => 'Paciente no registrado'], 404);
        }
        $paciente->delete();
        return response()->json(['mensaje' => 'Paciente eliminado'], 200);
    }
}
