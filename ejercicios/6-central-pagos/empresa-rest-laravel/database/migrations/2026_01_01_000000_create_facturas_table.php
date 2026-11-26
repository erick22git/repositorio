<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('facturas', function (Blueprint $table) {
            $table->id();
            $table->string('ci', 15);
            $table->string('periodo', 7);                        // 2026-10
            $table->decimal('monto', 10, 2);
            $table->string('estado', 20)->default('pendiente');  // pendiente | pagada
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('facturas');
    }
};
