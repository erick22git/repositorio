<?php
// 1) En routes/api.php, DENTRO del grupo con JwtMiddleware:
Route::delete('/reserva/{reserva}', [ReservaController::class, 'destroy']);

// 2) En ReservaController agregar:
public function destroy(Reserva $reserva)
{
    // Liberar la habitacion en el hotel y recien borrar la reserva
    $cod = $reserva->codHabitacion;
    try {
        if ($reserva->hotelDestino === 'A') {
            $ok = Http::acceptJson()->timeout(5)
                ->patch(self::HOTEL_A . '/' . $cod, ['disponible' => true])->successful();
        } else {
            $r = Http::timeout(5)->post(self::HOTEL_B, [
                'query'     => 'mutation($c: String!) { actualizarDisponibilidad(codHabitacion: $c, disponible: true) { disponible } }',
                'variables' => ['c' => $cod],
            ]);
            $ok = $r->successful() && $r->json('errors') === null;
        }
    } catch (ConnectionException $e) {
        return response()->json(['error' => 'El hotel no responde'], 503);
    }

    if (!$ok) {
        return response()->json(['error' => 'El hotel no acepto el cambio'], 502);
    }

    $reserva->delete();
    return response()->noContent();
}
