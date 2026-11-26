import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

// Banco Union: REST en Java, protegido con JWT.  Recurso /api/cuentas
public class ServidorUnion {

    static final String BASE = "/api/cuentas";

    static class Cuenta {
        String nrocuenta, nombre, moneda;
        double saldo;
        boolean activo = true;          // borrado logico

        Cuenta(String n, String nom, String mon, double s) {
            nrocuenta = n; nombre = nom; moneda = mon; saldo = s;
        }

        String json() {
            return "{\"nrocuenta\":\"" + nrocuenta + "\",\"nombre\":\"" + nombre
                    + "\",\"moneda\":\"" + moneda + "\",\"saldo\":" + saldo + "}";
        }
    }

    static final Map<String, Cuenta> CUENTAS = new LinkedHashMap<>();

    static void guardar(Cuenta c) { CUENTAS.put(c.nrocuenta, c); }

    public static void main(String[] args) throws IOException {
        guardar(new Cuenta("32212", "Cliente Uno", "Bs", 40));
        guardar(new Cuenta("32215", "Cliente Dos", "USD", 50));
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);
        server.createContext(BASE, ServidorUnion::atender);
        server.start();
        System.out.println("Banco Union en http://localhost:8085" + BASE);
    }

    static synchronized void atender(HttpExchange ex) throws IOException {
        try {
            // 1) PORTERO: sin token valido no pasa nadie
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !JwtJava.verificar(auth.replace("Bearer ", ""))) {
                responder(ex, 401, error("token invalido o ausente"));
                return;
            }

            String metodo = ex.getRequestMethod();
            String nro = ex.getRequestURI().getPath().substring(BASE.length()).replace("/", "");
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            // 2) Sin numero: listar o crear
            if (nro.isEmpty()) {
                if (metodo.equals("GET")) {
                    List<String> lista = new ArrayList<>();
                    for (Cuenta c : CUENTAS.values()) if (c.activo) lista.add(c.json());
                    responder(ex, 200, "[" + String.join(",", lista) + "]");
                } else if (metodo.equals("POST")) {
                    String n = campo(cuerpo, "nrocuenta"), nom = campo(cuerpo, "nombre"), mon = campo(cuerpo, "moneda");
                    if (n == null || nom == null || mon == null) { responder(ex, 422, error("faltan datos")); return; }
                    if (CUENTAS.containsKey(n)) { responder(ex, 409, error("ya existe")); return; }
                    guardar(new Cuenta(n, nom, mon, 0));
                    responder(ex, 201, CUENTAS.get(n).json());
                } else {
                    responder(ex, 405, error("metodo no permitido"));
                }
                return;
            }

            // 3) Con numero: ver, operar o borrar
            Cuenta c = CUENTAS.get(nro);
            if (c == null || !c.activo) { responder(ex, 404, error("cuenta no existe")); return; }

            if (metodo.equals("GET")) {
                responder(ex, 200, c.json());
            } else if (metodo.equals("PATCH")) {
                String operacion = campo(cuerpo, "operacion");
                String texto = campo(cuerpo, "monto");
                double monto = texto == null ? 0 : Double.parseDouble(texto);
                if (monto <= 0) { responder(ex, 422, error("monto invalido")); return; }

                if ("debito".equals(operacion)) {
                    if (c.saldo < monto) { responder(ex, 409, error("saldo insuficiente")); return; }
                    c.saldo -= monto;
                } else if ("abono".equals(operacion)) {
                    c.saldo += monto;
                } else {
                    responder(ex, 422, error("operacion debe ser debito o abono"));
                    return;
                }
                responder(ex, 200, c.json());
            } else if (metodo.equals("DELETE")) {
                c.activo = false;
                responder(ex, 204, "");
            } else {
                responder(ex, 405, error("metodo no permitido"));
            }
        } catch (Exception e) {
            responder(ex, 500, error("error interno"));
        }
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*(\"([^\"]*)\"|[-0-9.]+)").matcher(json);
        if (!m.find()) return null;
        return m.group(2) != null ? m.group(2) : m.group(1);
    }

    static String error(String m) { return "{\"error\":\"" + m + "\"}"; }

    static void responder(HttpExchange ex, int codigo, String cuerpo) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        if (codigo == 204) {
            ex.sendResponseHeaders(204, -1);
        } else {
            ex.sendResponseHeaders(codigo, b.length);
            ex.getResponseBody().write(b);
        }
        ex.close();
    }
}
