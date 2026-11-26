<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('multas', function (Blueprint $table) {
            $table->id();
            $table->string('codigo', 20)->unique();        // codigo unico de boleta
            $table->string('ci', 15);
            $table->string('nombre', 100);                 // conductor
            $table->date('fecha');
            $table->string('tipo', 60);
            $table->string('lugar', 100);
            $table->decimal('monto', 10, 2);
            $table->string('estado', 20)->default('pendiente');  // pendiente | pagada | anulada
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('multas');
    }
};
