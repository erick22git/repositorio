<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('actas', function (Blueprint $table) {
            $table->id();
            $table->foreignId('mesa_id')->unique()->constrained('mesas');
            $table->string('cargo');
            $table->unsignedInteger('blancos')->default(0);
            $table->unsignedInteger('nulos')->default(0);
            $table->string('registrado_por');
            $table->timestamps();
        });

        Schema::create('votos', function (Blueprint $table) {
            $table->id();
            $table->foreignId('acta_id')->constrained('actas')->cascadeOnDelete();
            $table->foreignId('candidato_id')->constrained('candidatos');
            $table->unsignedInteger('cantidad');
            $table->timestamps();
            $table->unique(['acta_id', 'candidato_id']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('votos');
        Schema::dropIfExists('actas');
    }
};
