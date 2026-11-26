<?php

namespace Database\Seeders;

use App\Models\Factura;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        Factura::create(['ci' => '11021654', 'periodo' => '2026-09', 'monto' => 85.5]);
        Factura::create(['ci' => '11021654', 'periodo' => '2026-10', 'monto' => 92]);
        Factura::create(['ci' => '5551234', 'periodo' => '2026-10', 'monto' => 40]);
    }
}
