<?php

namespace Database\Seeders;

use App\Models\Recurso;
use Illuminate\Database\Seeder;

class RecursoSeeder extends Seeder
{
    public function run(): void
    {
        // Datos de prueba. Si el examen da una tabla de datos, copiarla aqui.
        Recurso::create(['codigo' => 'A1', 'nombre' => 'Ejemplo uno', 'monto' => 100]);
        Recurso::create(['codigo' => 'A2', 'nombre' => 'Ejemplo dos', 'monto' => 200]);
    }
}
