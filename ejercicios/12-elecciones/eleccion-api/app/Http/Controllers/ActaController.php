<?php

namespace App\Http\Controllers;

use App\Models\Acta;
use App\Models\Candidato;
use App\Models\Mesa;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class ActaController extends Controller
{
    public function index()
    {
        return response()->json(Acta::with('mesa', 'votos.candidato')->get(), 200);
    }

    public function store(Request $request)
    {
        $usuario = $request->attributes->get('user');
        if (!in_array($usuario->role, ['admin', 'jurado'])) {
            return response()->json(['message' => 'Acceso denegado: solo jurados registran actas'], 403);
        }

        $datos = $request->validate([
            'mesa_id' => 'required|integer|exists:mesas,id',
            'cargo' => 'required|in:Rector,ViceRector,Decano,Director',
            'blancos' => 'required|integer|min:0',
            'nulos' => 'required|integer|min:0',
            'votos' => 'required|array|min:1',
            'votos.*.candidato_id' => 'required|integer|exists:candidatos,id',
            'votos.*.cantidad' => 'required|integer|min:0',
        ]);

        $mesa = Mesa::find($datos['mesa_id']);
        if ($mesa->acta) {
            return response()->json(['message' => 'La mesa ' . $mesa->numero . ' ya tiene acta registrada'], 409);
        }

        $ids = array_column($datos['votos'], 'candidato_id');
        if (count($ids) !== count(array_unique($ids))) {
            return response()->json(['message' => 'Un candidato esta repetido en el acta'], 422);
        }
        $deOtroCargo = Candidato::whereIn('id', $ids)->where('cargo', '!=', $datos['cargo'])->count();
        if ($deOtroCargo > 0) {
            return response()->json(['message' => 'Hay candidatos que no son de ' . $datos['cargo']], 422);
        }

        $total = array_sum(array_column($datos['votos'], 'cantidad')) + $datos['blancos'] + $datos['nulos'];
        if ($total > $mesa->habilitados) {
            return response()->json([
                'message' => "La mesa tiene {$mesa->habilitados} habilitados y el acta suma {$total} votos"
            ], 422);
        }

        $acta = DB::transaction(function () use ($datos, $usuario) {
            $acta = Acta::create([
                'mesa_id' => $datos['mesa_id'],
                'cargo' => $datos['cargo'],
                'blancos' => $datos['blancos'],
                'nulos' => $datos['nulos'],
                'registrado_por' => $usuario->email,
            ]);
            foreach ($datos['votos'] as $v) {
                $acta->votos()->create($v);
            }
            return $acta;
        });

        return response()->json($acta->load('votos.candidato'), 201);
    }

    public function show(Acta $acta)
    {
        return response()->json($acta->load('mesa', 'votos.candidato'), 200);
    }

    public function destroy(Request $request, Acta $acta)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $acta->delete();
        return response()->json(['message' => 'Acta anulada'], 200);
    }
}
