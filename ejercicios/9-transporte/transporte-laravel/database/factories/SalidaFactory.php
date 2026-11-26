<?php

namespace Database\Factories;

use Illuminate\Database\Eloquent\Factories\Factory;

class SalidaFactory extends Factory
{
    public function definition(): array
    {
        return [
            'destino'   => fake()->randomElement(['La Paz', 'Cochabamba', 'Santa Cruz', 'Potosí', 'Oruro', 'Tarija']),
            'fecha'     => fake()->dateTimeBetween('now', '+30 days')->format('Y-m-d'),
            'hora'      => fake()->randomElement(['07:00', '10:30', '14:00', '19:00', '21:30']),
            'bus'       => strtoupper(fake()->bothify('####-???')),
            'precio'    => fake()->randomElement([50, 70, 90, 120, 150]),
            'capacidad' => fake()->randomElement([40, 44, 48]),
        ];
    }
}
