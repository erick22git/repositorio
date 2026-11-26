<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('asientos', function (Blueprint $table) {
            $table->id();
            $table->foreignId('salida_id')->constrained('salidas')->cascadeOnDelete();
            $table->unsignedInteger('numero_asiento');
            $table->string('ci_pasajero', 20);
            $table->timestamps();
            $table->unique(['salida_id', 'numero_asiento']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('asientos');
    }
};
