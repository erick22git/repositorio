<?php
// Desde PHP puro:  php consumir.php
// SOAP necesita activar  extension=soap  en C:\xampp\php\php.ini

// ---------- SOAP ----------
try {
    $cliente = new SoapClient('http://localhost:51305/Servicio.asmx?WSDL',
        ['features' => SOAP_SINGLE_ELEMENT_ARRAYS]);

    $r = $cliente->__soapCall('Verificar', [['codigo' => 'A1']]);
    var_dump($r->VerificarResult);

    $lista = $cliente->__soapCall('Listar', [[]]);
    foreach ($lista->ListarResult->Dato as $d) {
        echo $d->Codigo, ' ', $d->Monto, PHP_EOL;
    }
} catch (SoapFault $e) {
    echo 'Error SOAP: ', $e->getMessage(), PHP_EOL;
}

// ---------- REST con cURL ----------
$ch = curl_init('http://localhost:8000/api/recursos');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER     => ['Accept: application/json'],
]);
$respuesta = curl_exec($ch);
echo curl_getinfo($ch, CURLINFO_HTTP_CODE), ' ', $respuesta, PHP_EOL;
