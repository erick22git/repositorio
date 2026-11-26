<?php

namespace Database\Seeders;

use App\Models\Multa;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Multa::create(['codigo' => 'B-001', 'ci' => '11021654', 'nombre' => 'Juan Perez', 'fecha' => '2026-06-01',
            'tipo' => 'Exceso de velocidad', 'lugar' => 'Av. Principal', 'monto' => 200]);
        Multa::create(['codigo' => 'B-002', 'ci' => '5551234', 'nombre' => 'Ana Rojas', 'fecha' => '2026-06-02',
            'tipo' => 'Mal estacionado', 'lugar' => 'Plaza central', 'monto' => 50, 'estado' => 'anulada']);
    }
}
