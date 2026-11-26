<?php
// Para que /api/recursos/A1 funcione (en vez de /api/recursos/1), en el MODELO:
public function getRouteKeyName(): string
{
    return 'codigo';
}
