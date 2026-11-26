<?php

namespace Database\Seeders;

use App\Models\Candidato;
use App\Models\Mesa;
use App\Models\User;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        User::factory()->create(['name' => 'Administrador', 'email' => 'test@example.com', 'role' => 'admin']);
        User::factory()->create(['name' => 'Jurado Mesa', 'email' => 'jurado@example.com', 'role' => 'jurado']);
        User::factory()->create(['name' => 'Test User 2', 'email' => 'test2@example.com', 'role' => 'user']);

        Candidato::create(['nombres' => 'Mario', 'apellidos' => 'Quiroga Leon', 'cargo' => 'Rector', 'ambito' => 'Universidad']);
        Candidato::create(['nombres' => 'Sonia', 'apellidos' => 'Paredes Vaca', 'cargo' => 'Rector', 'ambito' => 'Universidad']);
        Candidato::create(['nombres' => 'Hugo', 'apellidos' => 'Cardozo Rios', 'cargo' => 'Rector', 'ambito' => 'Universidad']);
        Candidato::factory()->count(6)->create(['cargo' => 'Decano', 'ambito' => 'Facultad']);

        for ($i = 1; $i <= 5; $i++) {
            Mesa::create(['numero' => $i, 'recinto' => $i <= 3 ? 'Facultad de Tecnologia' : 'Edificio Central', 'habilitados' => 200]);
        }
    }
}
