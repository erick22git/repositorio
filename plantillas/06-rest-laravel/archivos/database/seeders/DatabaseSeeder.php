<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // Usuario para probar el login (la clave se guarda cifrada sola)
        User::create(['name' => 'Admin', 'email' => 'admin@test.com', 'password' => '123456']);

        // Registrar aqui cada seeder, para que --seed lo ejecute
        $this->call([
            RecursoSeeder::class,
        ]);
    }
}
