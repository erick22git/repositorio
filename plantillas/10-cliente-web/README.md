# 10 · Cliente web (HTML + JavaScript)

[← Plantillas](../README.md) · [Índice](../../README.md)

**Qué es:** una página que hace login, consulta y registra contra tu API REST.

## Pasos
1. Guarda el código como `index.html`.
2. Cambia `API` y `RECURSO` si hace falta.
3. Ten el servidor encendido y abre el archivo con **doble clic**.
4. `F12` → pestañas **Console** y **Network** para ver errores.

Si tu API no usa JWT, no pulses "Entrar".

## index.html
```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Cliente web</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 640px; margin: 20px auto; }
    fieldset { margin-bottom: 12px; }
    input, button { margin: 4px 0; padding: 6px; }
    pre { background: #f2f2f2; padding: 10px; white-space: pre-wrap; }
  </style>
</head>
<body>
  <h2>Cliente web</h2>

  <fieldset>
    <legend>1. Iniciar sesion (solo si hay JWT)</legend>
    <input id="email" placeholder="correo" value="admin@test.com">
    <input id="password" type="password" placeholder="clave" value="123456">
    <button onclick="iniciarSesion()">Entrar</button>
  </fieldset>

  <fieldset>
    <legend>2. Consultar</legend>
    <input id="buscar" placeholder="id (vacio = todos)">
    <button onclick="consultar()">Consultar</button>
  </fieldset>

  <fieldset>
    <legend>3. Registrar</legend>
    <input id="codigo" placeholder="codigo">
    <input id="nombre" placeholder="nombre">
    <input id="monto" type="number" placeholder="monto">
    <button onclick="registrar()">Registrar</button>
  </fieldset>

  <pre id="salida">Aqui sale la respuesta</pre>

  <script>
    // CAMBIAR por la direccion de tu servicio
    const API = 'http://localhost:8000/api';
    const RECURSO = 'recursos';
    let token = '';

    const valor = id => document.getElementById(id).value.trim();
    const mostrar = dato => document.getElementById('salida').textContent =
      typeof dato === 'string' ? dato : JSON.stringify(dato, null, 2);

    // Una sola funcion para todas las llamadas REST
    async function llamar(metodo, ruta, cuerpo) {
      const headers = { 'Accept': 'application/json', 'Content-Type': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;
      try {
        const r = await fetch(API + ruta, { method: metodo, headers, body: cuerpo && JSON.stringify(cuerpo) });
        const texto = await r.text();
        const datos = texto ? JSON.parse(texto) : {};
        mostrar({ codigoHTTP: r.status, ok: r.ok, datos });
        return r.ok ? datos : null;
      } catch (e) {
        mostrar('No se pudo conectar: ' + e.message);
        return null;
      }
    }

    async function iniciarSesion() {
      const d = await llamar('POST', '/login', { email: valor('email'), password: valor('password') });
      if (d) token = d.token;
    }

    function consultar() {
      const id = valor('buscar');
      llamar('GET', '/' + RECURSO + (id ? '/' + encodeURIComponent(id) : ''));
    }

    function registrar() {
      // AQUI LOGICA (validar campos, llamar a otro servicio antes, etc.)
      llamar('POST', '/' + RECURSO, {
        codigo: valor('codigo'),
        nombre: valor('nombre'),
        monto: Number(valor('monto'))
      });
    }
  </script>
</body>
</html>
```

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Mostrar en tabla | [Cliente web del ejercicio 4](../../ejercicios/4-multas/README.md) |
| Formulario con lista desplegable | [Cliente web del ejercicio 5](../../ejercicios/5-reservas-hotel/README.md) |
| Consumir GraphQL o SOAP | [logica/consumir.js](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |

## Cómo defenderlo
- "`fetch` hace la petición HTTP; es asíncrono, por eso `async/await`."
- "Mando `Accept` y `Content-Type: application/json`, y el token en `Authorization`."
- "Reviso `r.ok` porque `fetch` no lanza error con un 404."
- "**CORS**: el navegador solo permite llamar a otro puerto si el servidor lo autoriza."
