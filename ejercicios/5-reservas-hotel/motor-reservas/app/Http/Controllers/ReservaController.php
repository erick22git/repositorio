<?php

namespace App\Http\Controllers;

use App\Models\Reserva;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Http;

// INTERMEDIARIO: un hotel habla REST y el otro GraphQL. El cliente no se entera.
class ReservaController extends Controller
{
    private const HOTEL_A = 'http://localhost:8001/api/habitaciones';   // REST
    private const HOTEL_B = 'http://localhost:4000/graphql';            // GraphQL

    // GET /api/reserva
    public function index()
    {
        return response()->json(Reserva::all());
    }

    // POST /api/reserva
    public function store(Request $request)
    {
        $datos = $request->validate([
            'fecha'         => 'required|date',
            'idHuesped'     => 'required|string',
            'codHabitacion' => 'required|alpha_num',
            'hotelDestino'  => 'required|in:A,B',
            'noches'        => 'required|integer|min:1',
        ]);

        try {
            // 1) Preguntar al hotel
            $habitacion = $this->consultar($datos['hotelDestino'], $datos['codHabitacion']);
            if (!$habitacion) {
                return response()->json(['confirmada' => false, 'error' => 'La habitacion no existe'], 404);
            }
            // 2) Verificar disponibilidad y tarifa
            if (!$habitacion['disponible']) {
                return response()->json(['confirmada' => false, 'error' => 'No esta disponible'], 409);
            }
            // 3) Marcarla como ocupada en el hotel
            if (!$this->ocupar($datos['hotelDestino'], $datos['codHabitacion'])) {
                return response()->json(['confirmada' => false, 'error' => 'El hotel no acepto el cambio'], 502);
            }
        } catch (ConnectionException $e) {
            return response()->json(['confirmada' => false, 'error' => 'El hotel no responde'], 503);
        }

        // 4) Guardar la reserva y devolver el resultado
        $datos['total'] = $habitacion['tarifa'] * $datos['noches'];
        $reserva = Reserva::create($datos);

        return response()->json(['confirmada' => true, 'tarifa' => $habitacion['tarifa'], 'reserva' => $reserva], 201);
    }

    private function consultar(string $hotel, string $cod): ?array
    {
        if ($hotel === 'A') {
            $r = Http::acceptJson()->timeout(5)->get(self::HOTEL_A . '/' . $cod);
            return $r->successful() ? $r->json() : null;
        }

        $r = Http::timeout(5)->post(self::HOTEL_B, [
            'query'     => 'query($c: String!) { habitacion(codHabitacion: $c) { tarifa disponible } }',
            'variables' => ['c' => $cod],
        ]);
        return $r->json('data.habitacion');      // null si no existe
    }

    private function ocupar(string $hotel, string $cod): bool
    {
        if ($hotel === 'A') {
            return Http::acceptJson()->timeout(5)
                ->patch(self::HOTEL_A . '/' . $cod, ['disponible' => false])
                ->successful();
        }

        $r = Http::timeout(5)->post(self::HOTEL_B, [
            'query'     => 'mutation($c: String!) { actualizarDisponibilidad(codHabitacion: $c, disponible: false) { disponible } }',
            'variables' => ['c' => $cod],
        ]);
        // GraphQL responde 200 aunque falle: hay que mirar "errors"
        return $r->successful() && $r->json('errors') === null;
    }
}
