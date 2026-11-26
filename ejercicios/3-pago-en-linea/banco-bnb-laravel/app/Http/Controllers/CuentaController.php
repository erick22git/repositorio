<?php

namespace App\Http\Controllers;

use App\Models\Cuenta;
use Illuminate\Http\Request;

class CuentaController extends Controller
{
    public function index()
    {
        return response()->json(Cuenta::where('activo', true)->get());
    }

    public function store(Request $request)
    {
        $datos = $request->validate([
            'nrocuenta' => 'required|string',
            'nombre'    => 'required|string',
            'moneda'    => 'required|in:Bs,USD',
        ]);

        if (Cuenta::where('nrocuenta', $datos['nrocuenta'])->exists()) {
            return response()->json(['error' => 'La cuenta ya existe'], 409);
        }

        return response()->json(Cuenta::create($datos)->refresh(), 201);
    }

    public function show(Cuenta $cuenta)
    {
        if (!$cuenta->activo) {
            return response()->json(['error' => 'Cuenta no existe'], 404);
        }
        return response()->json($cuenta);
    }

    // PATCH /api/cuentas/{nro}   { "operacion": "debito" | "abono", "monto": 10 }
    public function update(Request $request, Cuenta $cuenta)
    {
        $datos = $request->validate([
            'operacion' => 'required|in:debito,abono',
            'monto'     => 'required|numeric|gt:0',
        ]);

        if (!$cuenta->activo) {
            return response()->json(['error' => 'Cuenta no existe'], 404);
        }

        if ($datos['operacion'] === 'debito') {
            if ($cuenta->saldo < $datos['monto']) {
                return response()->json(['error' => 'Saldo insuficiente'], 409);
            }
            $cuenta->saldo -= $datos['monto'];
        } else {
            $cuenta->saldo += $datos['monto'];
        }
        $cuenta->save();

        return response()->json($cuenta);
    }

    public function destroy(Cuenta $cuenta)
    {
        $cuenta->update(['activo' => false]);
        return response()->noContent();
    }
}
