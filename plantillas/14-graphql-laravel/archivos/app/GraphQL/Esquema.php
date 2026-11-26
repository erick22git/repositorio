<?php

namespace App\GraphQL;

use App\Models\Recurso;
use GraphQL\Error\UserError;
use GraphQL\Type\Definition\ObjectType;
use GraphQL\Type\Definition\Type;
use GraphQL\Type\Schema;

// Mismo esquema que la plantilla 08 de Node, escrito en PHP
class Esquema
{
    public static function crear(): Schema
    {
        // 1) TIPO: que campos se pueden pedir
        $recursoType = new ObjectType([
            'name' => 'Recurso',
            'fields' => [
                'id'     => Type::string(),
                'codigo' => Type::string(),
                'nombre' => Type::string(),
                'monto'  => Type::float(),
                'estado' => Type::string(),
            ],
        ]);

        // 2) QUERIES: leer
        $query = new ObjectType([
            'name' => 'RootQueryType',
            'fields' => [
                'recursos' => [
                    'type' => Type::listOf($recursoType),
                    'args' => ['estado' => Type::string()],
                    'resolve' => function ($root, $args) {
                        $q = Recurso::query();
                        if (!empty($args['estado'])) $q->where('estado', $args['estado']);
                        return $q->get();
                    },
                ],
                'recurso' => [
                    'type' => $recursoType,
                    'args' => ['codigo' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        $r = Recurso::where('codigo', $args['codigo'])->first();
                        if (!$r) throw new UserError('No existe ' . $args['codigo']);
                        // AQUI LOGICA
                        return $r;
                    },
                ],
            ],
        ]);

        // 3) MUTATIONS: crear / cambiar
        $mutation = new ObjectType([
            'name' => 'Mutation',
            'fields' => [
                'crearRecurso' => [
                    'type' => $recursoType,
                    'args' => [
                        'codigo' => Type::nonNull(Type::string()),
                        'nombre' => Type::nonNull(Type::string()),
                        'monto'  => Type::float(),
                    ],
                    'resolve' => function ($root, $args) {
                        if (Recurso::where('codigo', $args['codigo'])->exists()) {
                            throw new UserError('El codigo ya existe');
                        }
                        // AQUI LOGICA (validaciones antes de guardar)
                        return Recurso::create($args)->refresh();
                    },
                ],
                'cambiarEstado' => [
                    'type' => $recursoType,
                    'args' => [
                        'codigo' => Type::nonNull(Type::string()),
                        'estado' => Type::nonNull(Type::string()),
                    ],
                    'resolve' => function ($root, $args) {
                        $r = Recurso::where('codigo', $args['codigo'])->first();
                        if (!$r) throw new UserError('No existe ' . $args['codigo']);
                        $r->update(['estado' => $args['estado']]);
                        return $r;
                    },
                ],
            ],
        ]);

        return new Schema(['query' => $query, 'mutation' => $mutation]);
    }
}
