# 09 · API REST en Java, sin librerías

[← Plantillas](../README.md) · [Índice](../../README.md) · **[EXTRA]** (usa `HttpServer`, que viene en el JDK)

**Cuándo usarla:** si piden "un servicio REST en Java". Datos en memoria, no necesita internet.

## Pasos
1. Proyecto Java con `ServidorRest.java` → ejecutar.
2. Navegador: `http://localhost:8085/api/recursos`.

## ServidorRest.java
```java
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

// API REST en Java SIN librerias (viene incluido en el JDK). Recurso: /api/recursos
public class ServidorRest {

    static final int PUERTO = 8085;
    static final String BASE = "/api/recursos";

    // Datos en memoria:  codigo -> {codigo, nombre, monto, estado}
    static final Map<String, Map<String, String>> DATOS = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        guardar("A1", "Ejemplo uno", "100");
        guardar("A2", "Ejemplo dos", "200");
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        server.createContext(BASE, ServidorRest::atender);
        server.start();
        System.out.println("REST Java en http://localhost:" + PUERTO + BASE);
    }

    static synchronized void atender(HttpExchange ex) throws IOException {
        try {
            String metodo = ex.getRequestMethod();
            if (metodo.equals("OPTIONS")) { responder(ex, 204, ""); return; }

            // /api/recursos       -> id = ""
            // /api/recursos/A1    -> id = "A1"
            String id = ex.getRequestURI().getPath().substring(BASE.length()).replace("/", "");
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            // AQUI LOGICA (validar token, reglas del ejercicio)

            if (metodo.equals("GET") && id.isEmpty()) {
                List<String> lista = new ArrayList<>();
                for (Map<String, String> r : DATOS.values()) lista.add(json(r));
                responder(ex, 200, "[" + String.join(",", lista) + "]");
            } else if (metodo.equals("POST")) {
                String codigo = campo(cuerpo, "codigo");
                if (codigo == null) { responder(ex, 422, error("falta codigo")); return; }
                if (DATOS.containsKey(codigo)) { responder(ex, 409, error("ya existe")); return; }
                guardar(codigo, campo(cuerpo, "nombre"), campo(cuerpo, "monto"));
                responder(ex, 201, json(DATOS.get(codigo)));
            } else {
                Map<String, String> r = DATOS.get(id);
                if (r == null) { responder(ex, 404, error("no existe")); return; }
                if (metodo.equals("GET")) {
                    responder(ex, 200, json(r));
                } else if (metodo.equals("PUT") || metodo.equals("PATCH")) {
                    for (String c : new String[]{"nombre", "monto", "estado"}) {
                        String v = campo(cuerpo, c);
                        if (v != null) r.put(c, v);
                    }
                    responder(ex, 200, json(r));
                } else if (metodo.equals("DELETE")) {
                    DATOS.remove(id);
                    responder(ex, 204, "");
                } else {
                    responder(ex, 405, error("metodo no permitido"));
                }
            }
        } catch (Exception e) {
            responder(ex, 500, error(e.getMessage()));
        }
    }

    static void guardar(String codigo, String nombre, String monto) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("codigo", codigo);
        r.put("nombre", nombre == null ? "" : nombre);
        r.put("monto", monto == null ? "0" : monto);
        r.put("estado", "activo");
        DATOS.put(codigo, r);
    }

    // Lee un campo de un JSON simple (sin objetos anidados)
    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*(\"([^\"]*)\"|[-0-9.]+|true|false)").matcher(json);
        if (!m.find()) return null;
        return m.group(2) != null ? m.group(2) : m.group(1);
    }

    static String json(Map<String, String> r) {
        List<String> partes = new ArrayList<>();
        r.forEach((k, v) -> partes.add("\"" + k + "\":\"" + v.replace("\"", "'") + "\""));
        return "{" + String.join(",", partes) + "}";
    }

    static String error(String mensaje) {
        return "{\"error\":\"" + mensaje + "\"}";
    }

    static void responder(HttpExchange ex, int codigo, String cuerpo) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        if (codigo == 204) {
            ex.sendResponseHeaders(204, -1);
        } else {
            ex.sendResponseHeaders(codigo, bytes.length);
            ex.getResponseBody().write(bytes);
        }
        ex.close();
    }
}
```

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Protegerlo con JWT | [Ejercicio 3](../../ejercicios/3-pago-en-linea/README.md) (`JwtJava.java`) |
| Débito / abono, borrado lógico | [Ejercicio 3: Banco Unión](../../ejercicios/3-pago-en-linea/README.md) |

La lectura de JSON es **simple** (sin objetos anidados). Si el docente permite librerías, se usa Jackson.

## Cómo defenderlo
- "`HttpServer` escucha en el puerto; `createContext` asocia una ruta con un método."
- "Miro el **método** (GET, POST…) y la **ruta** para saber qué hacer, y respondo con el código HTTP correcto."
- "`synchronized` evita que dos peticiones cambien el mapa a la vez."
- "Es el mismo REST que en Laravel, pero sin framework."
