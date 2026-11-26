<?php

namespace Database\Seeders;

use App\Models\Cuenta;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Cuenta::create(['nrocuenta' => '12345', 'nombre' => 'Cliente Tres', 'moneda' => 'Bs', 'saldo' => 455]);
        Cuenta::create(['nrocuenta' => '12455', 'nombre' => 'Cliente Cuatro', 'moneda' => 'USD', 'saldo' => 300]);
    }
}
