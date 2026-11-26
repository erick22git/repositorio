<?php

namespace Database\Seeders;

use App\Models\Paciente;
use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::create(['name' => 'Recepcion', 'email' => 'admin@test.com', 'password' => '123456']);

        Paciente::create(['ci' => '11021654', 'nombres' => 'Juan', 'apellidos' => 'Perez Segovia', 'telefono' => '70011223', 'seguro' => true]);
        Paciente::create(['ci' => '5551234', 'nombres' => 'Ana', 'apellidos' => 'Rojas Flores', 'telefono' => '71122334', 'seguro' => false]);

        for ($i = 0; $i < 5; $i++) {
            Paciente::create([
                'ci'        => (string) fake()->unique()->numberBetween(1000000, 9999999),
                'nombres'   => fake()->firstName(),
                'apellidos' => fake()->lastName() . ' ' . fake()->lastName(),
                'telefono'  => '7' . fake()->numerify('#######'),
                'seguro'    => fake()->boolean(),
            ]);
        }
    }
}
