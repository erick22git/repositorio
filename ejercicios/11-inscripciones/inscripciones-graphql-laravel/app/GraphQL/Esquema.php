<?php

namespace App\GraphQL;

use App\Models\Inscripcion;
use App\Models\Materia;
use GraphQL\Error\UserError;
use GraphQL\Type\Definition\ObjectType;
use GraphQL\Type\Definition\Type;
use GraphQL\Type\Schema;
use Illuminate\Support\Facades\Http;

class Esquema
{
    const KARDEX = 'http://127.0.0.1:3000/api/estudiantes/';
    const GESTION = '2/2026';
    const MAXIMO_MATERIAS = 4;

    private static function estudiante($ru)
    {
        try {
            $r = Http::timeout(5)->acceptJson()->get(self::KARDEX . urlencode($ru));
        } catch (\Exception $e) {
            throw new UserError('El servicio de kardex (Node) no responde');
        }
        if ($r->status() == 404) {
            throw new UserError('No existe el estudiante con RU ' . $ru);
        }
        if (!$r->successful()) {
            throw new UserError('Error ' . $r->status() . ' al consultar el kardex');
        }
        return $r->json();
    }

    public static function crear(): Schema
    {
        $materiaType = new ObjectType([
            'name' => 'Materia',
            'fields' => function () use (&$inscripcionType) {
                return [
                    'id'          => Type::int(),
                    'sigla'       => Type::string(),
                    'nombre'      => Type::string(),
                    'docente'     => Type::string(),
                    'horario'     => Type::string(),
                    'cupo'        => Type::int(),
                    'inscritos'   => [
                        'type' => Type::int(),
                        'resolve' => fn ($m) => $m->inscripciones()->where('gestion', self::GESTION)->count(),
                    ],
                    'disponibles' => [
                        'type' => Type::int(),
                        'resolve' => fn ($m) => $m->cupo - $m->inscripciones()->where('gestion', self::GESTION)->count(),
                    ],
                    'estudiantes' => [
                        'type' => Type::listOf($inscripcionType),
                        'resolve' => fn ($m) => $m->inscripciones()->where('gestion', self::GESTION)->orderBy('estudiante')->get(),
                    ],
                ];
            },
        ]);

        $inscripcionType = new ObjectType([
            'name' => 'Inscripcion',
            'fields' => function () use ($materiaType) {
                return [
                    'id'         => Type::int(),
                    'ru'         => Type::string(),
                    'estudiante' => Type::string(),
                    'gestion'    => Type::string(),
                    'materia'    => ['type' => $materiaType, 'resolve' => fn ($i) => $i->materia],
                ];
            },
        ]);

        $query = new ObjectType([
            'name' => 'RootQueryType',
            'fields' => [
                'materias' => [
                    'type' => Type::listOf($materiaType),
                    'resolve' => fn () => Materia::orderBy('sigla')->get(),
                ],
                'materia' => [
                    'type' => $materiaType,
                    'args' => ['sigla' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        $m = Materia::where('sigla', strtoupper($args['sigla']))->first();
                        if (!$m) throw new UserError('No existe la materia ' . $args['sigla']);
                        return $m;
                    },
                ],
                'inscripcionesDe' => [
                    'type' => Type::listOf($inscripcionType),
                    'args' => ['ru' => Type::nonNull(Type::string())],
                    'resolve' => function ($root, $args) {
                        return Inscripcion::where('ru', $args['ru'])->where('gestion', self::GESTION)->get();
                    },
                ],
            ],
        ]);

        $mutation = new ObjectType([
            'name' => 'Mutation',
            'fields' => [
                'inscribir' => [
                    'type' => $inscripcionType,
                    'args' => [
                        'ru'    => Type::nonNull(Type::string()),
                        'sigla' => Type::nonNull(Type::string()),
                    ],
                    'resolve' => function ($root, $args) {
                        $materia = Materia::where('sigla', strtoupper($args['sigla']))->first();
                        if (!$materia) throw new UserError('No existe la materia ' . $args['sigla']);

                        $est = self::estudiante($args['ru']);
                        if (!$est['habilitado']) {
                            throw new UserError('El estudiante no esta habilitado para inscribirse');
                        }
                        if ($est['deuda'] > 0) {
                            throw new UserError('El estudiante tiene una deuda pendiente de Bs ' . $est['deuda']);
                        }

                        $ya = Inscripcion::where('ru', $args['ru'])->where('materia_id', $materia->id)
                            ->where('gestion', self::GESTION)->exists();
                        if ($ya) throw new UserError('El estudiante ya esta inscrito en ' . $materia->sigla);

                        $cantidad = Inscripcion::where('ru', $args['ru'])->where('gestion', self::GESTION)->count();
                        if ($cantidad >= self::MAXIMO_MATERIAS) {
                            throw new UserError('No puede inscribirse a mas de ' . self::MAXIMO_MATERIAS . ' materias');
                        }

                        $ocupados = $materia->inscripciones()->where('gestion', self::GESTION)->count();
                        if ($ocupados >= $materia->cupo) {
                            throw new UserError('La materia ' . $materia->sigla . ' ya no tiene cupo');
                        }

                        return Inscripcion::create([
                            'ru'         => $args['ru'],
                            'estudiante' => $est['nombre'],
                            'materia_id' => $materia->id,
                            'gestion'    => self::GESTION,
                        ]);
                    },
                ],
                'retirar' => [
                    'type' => Type::string(),
                    'args' => [
                        'ru'    => Type::nonNull(Type::string()),
                        'sigla' => Type::nonNull(Type::string()),
                    ],
                    'resolve' => function ($root, $args) {
                        $materia = Materia::where('sigla', strtoupper($args['sigla']))->first();
                        if (!$materia) throw new UserError('No existe la materia ' . $args['sigla']);
                        $i = Inscripcion::where('ru', $args['ru'])->where('materia_id', $materia->id)
                            ->where('gestion', self::GESTION)->first();
                        if (!$i) throw new UserError('El estudiante no esta inscrito en ' . $materia->sigla);
                        $i->delete();
                        return 'Se retiro a ' . $i->estudiante . ' de ' . $materia->sigla;
                    },
                ],
                'crearMateria' => [
                    'type' => $materiaType,
                    'args' => [
                        'sigla'   => Type::nonNull(Type::string()),
                        'nombre'  => Type::nonNull(Type::string()),
                        'docente' => Type::nonNull(Type::string()),
                        'horario' => Type::nonNull(Type::string()),
                        'cupo'    => Type::nonNull(Type::int()),
                    ],
                    'resolve' => function ($root, $args) {
                        $args['sigla'] = strtoupper($args['sigla']);
                        if ($args['cupo'] < 1) throw new UserError('El cupo debe ser mayor a 0');
                        if (Materia::where('sigla', $args['sigla'])->exists()) {
                            throw new UserError('Ya existe la materia ' . $args['sigla']);
                        }
                        return Materia::create($args);
                    },
                ],
            ],
        ]);

        return new Schema(['query' => $query, 'mutation' => $mutation]);
    }
}
