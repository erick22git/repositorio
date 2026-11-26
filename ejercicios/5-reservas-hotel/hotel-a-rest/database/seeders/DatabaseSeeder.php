<?php

namespace Database\Seeders;

use App\Models\Habitacion;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Habitacion::create(['codHabitacion' => 'A101', 'tipo' => 'Simple', 'capacidad' => 1, 'tarifa' => 150]);
        Habitacion::create(['codHabitacion' => 'A201', 'tipo' => 'Doble', 'capacidad' => 2, 'tarifa' => 250]);
    }
}
