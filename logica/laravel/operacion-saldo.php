<?php
// PATCH /api/recursos/{recurso}   { "operacion": "debito", "monto": 10 }
// Arriba:  use Illuminate\Support\Facades\DB;
public function update(Request $request, Recurso $recurso)
{
    $datos = $request->validate([
        'operacion' => 'required|in:debito,abono',
        'monto'     => 'required|numeric|gt:0',
    ]);

    // Transaccion + bloqueo: dos peticiones no pueden gastar el mismo saldo
    return DB::transaction(function () use ($recurso, $datos) {
        $r = Recurso::lockForUpdate()->find($recurso->id);

        if ($datos['operacion'] === 'debito' && $r->monto < $datos['monto']) {
            return response()->json(['error' => 'Saldo insuficiente'], 409);
        }

        $r->monto += $datos['operacion'] === 'debito' ? -$datos['monto'] : $datos['monto'];
        $r->save();

        return response()->json($r);
    });
}
