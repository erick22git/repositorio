<?php

namespace Database\Seeders;

use App\Models\Cotizacion;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // datos del enunciado (la oficial es 6.97 en todas)
        $datos = [
            '2025-04-04' => 7.02, '2025-04-05' => 7.05, '2025-04-06' => 7.10,
            '2025-04-07' => 8.20, '2025-04-08' => 8.30, '2025-04-09' => 8.40,
            '2025-04-10' => 8.70, '2025-04-11' => 9.00, '2025-04-12' => 9.20,
        ];
        foreach ($datos as $fecha => $valor) {
            Cotizacion::create(['fecha' => $fecha, 'cotizacion' => $valor, 'cotizacion_oficial' => 6.97]);
        }
    }
}
