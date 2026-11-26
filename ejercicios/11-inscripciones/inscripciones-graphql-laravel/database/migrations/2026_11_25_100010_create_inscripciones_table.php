<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('inscripciones', function (Blueprint $table) {
            $table->id();
            $table->string('ru', 10);
            $table->string('estudiante', 120);
            $table->foreignId('materia_id')->constrained('materias')->cascadeOnDelete();
            $table->string('gestion', 10);
            $table->timestamps();
            $table->unique(['ru', 'materia_id', 'gestion']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('inscripciones');
    }
};
