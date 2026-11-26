<?php
// Fragmentos para pegar DENTRO de un controlador de Laravel (no es un archivo completo)
// Arriba del controlador:  use Illuminate\Support\Facades\Http;

// REST
$r = Http::acceptJson()->timeout(5)->get('http://localhost:8001/api/recursos');
$r = Http::acceptJson()->post('http://localhost:8001/api/recursos', ['codigo' => 'X1', 'nombre' => 'n', 'monto' => 5]);
$r = Http::withToken($token)->acceptJson()->get('http://localhost:8001/api/recursos');
if ($r->failed()) {
    return response()->json(['error' => 'El otro servicio respondio ' . $r->status()], 502);
}
$datos = $r->json();

// GraphQL
$r = Http::post('http://localhost:4000/graphql', [
    'query'     => 'query($c: String!) { recurso(codigo: $c) { nombre } }',
    'variables' => ['c' => 'A1'],
]);
$nombre = $r->json('data.recurso.nombre');

// SOAP  (activar extension=soap en php.ini)
$soap = new \SoapClient('http://localhost:51305/Servicio.asmx?WSDL');
$existe = $soap->__soapCall('Verificar', [['codigo' => 'A1']])->VerificarResult;

// Socket TCP (para hablar con un servidor Java)
$s = fsockopen('localhost', 5002, $errno, $errstr, 5);
fwrite($s, "hola\n");
$respuesta = trim(fgets($s));
fclose($s);
