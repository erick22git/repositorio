<?php

namespace Database\Seeders;

use App\Models\Asiento;
use App\Models\Salida;
use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::create(['name' => 'Boletero', 'email' => 'admin@test.com', 'password' => '123456']);

        Salida::factory()->count(5)->create();
        Asiento::create(['salida_id' => 1, 'numero_asiento' => 1, 'ci_pasajero' => '11021654']);
    }
}
