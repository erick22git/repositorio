<?php

namespace App\GraphQL;

use App\Models\Cotizacion;
use GraphQL\Type\Definition\ObjectType;
use GraphQL\Type\Definition\Type;
use GraphQL\Type\Schema;

class Esquema
{
    const OFICIAL = 6.97;

    // fecha AAAA-MM-DD que exista en el calendario
    public static function fechaValida($fecha): bool
    {
        if (!is_string($fecha) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $fecha)) return false;
        [$a, $m, $d] = explode('-', $fecha);
        return checkdate((int) $m, (int) $d, (int) $a);
    }

    public static function crear(): Schema
    {
        $cotizacionType = new ObjectType([
            'name' => 'Cotizacion',
            'fields' => [
                'fecha'              => ['type' => Type::string(), 'resolve' => fn ($c) => substr((string) $c->fecha, 0, 10)],
                'cotizacion'         => Type::float(),
                'cotizacion_oficial' => Type::float(),
            ],
        ]);

        // misma respuesta para consultar y registrar
        $respuestaType = new ObjectType([
            'name' => 'RespuestaCotizacion',
            'fields' => [
                'exito'      => Type::nonNull(Type::boolean()),
                'mensaje'    => Type::nonNull(Type::string()),
                'cotizacion' => $cotizacionType,
            ],
        ]);

        $query = new ObjectType([
            'name' => 'Query',
            'fields' => [
                'obtenerCotizacion' => [
                    'type' => $respuestaType,
                    'args' => ['fecha' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        if (!self::fechaValida($args['fecha'])) {
                            return ['exito' => false, 'mensaje' => 'Fecha invalida, use AAAA-MM-DD', 'cotizacion' => null];
                        }
                        $c = Cotizacion::where('fecha', $args['fecha'])->first();
                        if (!$c) {
                            return ['exito' => false, 'mensaje' => 'No se encontro la cotizacion para ' . $args['fecha'], 'cotizacion' => null];
                        }
                        return ['exito' => true, 'mensaje' => 'Cotizacion encontrada', 'cotizacion' => $c];
                    },
                ],
                'cotizaciones' => [
                    'type' => Type::listOf($cotizacionType),
                    'resolve' => fn () => Cotizacion::orderBy('fecha')->get(),
                ],
            ],
        ]);

        $mutation = new ObjectType([
            'name' => 'Mutation',
            'fields' => [
                'registrarCotizacion' => [
                    'type' => $respuestaType,
                    'args' => [
                        'fecha' => Type::nonNull(Type::string()),
                        'monto' => Type::nonNull(Type::float()),
                    ],
                    'resolve' => function ($root, $args) {
                        if (!self::fechaValida($args['fecha'])) {
                            return ['exito' => false, 'mensaje' => 'Fecha invalida, use AAAA-MM-DD', 'cotizacion' => null];
                        }
                        if ($args['monto'] <= 0) {
                            return ['exito' => false, 'mensaje' => 'El monto debe ser mayor a 0', 'cotizacion' => null];
                        }
                        if (Cotizacion::where('fecha', $args['fecha'])->exists()) {
                            return ['exito' => false, 'mensaje' => 'Ya existe una cotizacion para ' . $args['fecha'], 'cotizacion' => null];
                        }
                        $c = Cotizacion::create([
                            'fecha' => $args['fecha'],
                            'cotizacion' => round($args['monto'], 2),
                            'cotizacion_oficial' => self::OFICIAL,
                        ]);
                        return ['exito' => true, 'mensaje' => 'Cotizacion registrada', 'cotizacion' => $c];
                    },
                ],
            ],
        ]);

        return new Schema(['query' => $query, 'mutation' => $mutation]);
    }
}
