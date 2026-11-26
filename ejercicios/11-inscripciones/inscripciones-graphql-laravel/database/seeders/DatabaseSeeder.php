<?php

namespace Database\Seeders;

use App\Models\Materia;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Materia::create(['sigla' => 'SIS258', 'nombre' => 'Sistemas Distribuidos', 'docente' => 'Ing. Docente', 'horario' => 'Lun-Mie 07:00', 'cupo' => 3]);
        Materia::create(['sigla' => 'SIS325', 'nombre' => 'Base de Datos II', 'docente' => fake()->name(), 'horario' => 'Mar-Jue 09:00', 'cupo' => 30]);
        Materia::create(['sigla' => 'COM350', 'nombre' => 'Arquitectura de Software', 'docente' => fake()->name(), 'horario' => 'Lun-Mie 11:00', 'cupo' => 25]);
        Materia::create(['sigla' => 'SIS427', 'nombre' => 'Tecnologias Emergentes', 'docente' => fake()->name(), 'horario' => 'Vie 14:00', 'cupo' => 20]);
        Materia::create(['sigla' => 'SIS421', 'nombre' => 'Inteligencia Artificial II', 'docente' => fake()->name(), 'horario' => 'Mar-Jue 16:00', 'cupo' => 20]);
        Materia::create(['sigla' => 'MAT207', 'nombre' => 'Investigacion Operativa', 'docente' => fake()->name(), 'horario' => 'Sab 08:00', 'cupo' => 40]);
    }
}
