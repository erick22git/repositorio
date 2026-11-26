<?php

namespace App\Http\Controllers;

use App\GraphQL\Esquema;
use GraphQL\GraphQL;
use Illuminate\Http\Request;

// Un solo endpoint:  POST /api/graphql   { "query": "...", "variables": {...} }
class GraphQLController extends Controller
{
    public function __invoke(Request $request)
    {
        $resultado = GraphQL::executeQuery(
            Esquema::crear(),
            $request->input('query'),
            null,
            null,
            $request->input('variables')
        );
        return response()->json($resultado->toArray());
    }
}
