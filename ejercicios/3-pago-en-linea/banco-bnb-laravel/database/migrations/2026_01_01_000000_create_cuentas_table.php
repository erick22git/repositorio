<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('cuentas', function (Blueprint $table) {
            $table->id();
            $table->string('nrocuenta', 20)->unique();
            $table->string('nombre', 100);
            $table->string('moneda', 5);                    // Bs o USD
            $table->decimal('saldo', 12, 2)->default(0);
            $table->boolean('activo')->default(true);       // borrado logico
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('cuentas');
    }
};
