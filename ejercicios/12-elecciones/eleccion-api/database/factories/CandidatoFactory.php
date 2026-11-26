<?php

namespace Database\Factories;

use Illuminate\Database\Eloquent\Factories\Factory;

class CandidatoFactory extends Factory
{
    public function definition(): array
    {
        $ambitos = ['Rector' => 'Universidad', 'ViceRector' => 'Universidad', 'Decano' => 'Facultad', 'Director' => 'Carrera'];
        $cargo = $this->faker->randomElement(['Rector', 'ViceRector', 'Decano', 'Director']);
        $ambito = $ambitos[$cargo];
        return [
            'nombres' => $this->faker->firstName(),
            'apellidos' => $this->faker->lastName(),
            'cargo' => $cargo,
            'ambito' => $ambito,
        ];
    }
}
