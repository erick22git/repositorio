<?php
// 1) En routes/api.php (con o sin token, segun pidan):
Route::get('/disponibles', [ReservaController::class, 'disponibles']);

// 2) En ReservaController agregar: junta los dos hoteles y ordena por precio
public function disponibles()
{
    $a = Http::acceptJson()->timeout(5)->get(self::HOTEL_A)->json() ?? [];
    $b = Http::timeout(5)->post(self::HOTEL_B, [
        'query' => '{ habitaciones { codHabitacion tipo capacidad tarifa disponible } }',
    ])->json('data.habitaciones') ?? [];

    $todas = collect($a)->map(fn ($h) => $h + ['hotel' => 'A'])
        ->concat(collect($b)->map(fn ($h) => $h + ['hotel' => 'B']))
        ->where('disponible', true)
        ->sortBy('tarifa')
        ->values();

    return response()->json($todas);
}
