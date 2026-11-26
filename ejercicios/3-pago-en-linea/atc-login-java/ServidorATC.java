import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

// Servicio ATC:  GET /api/usuarios   GET /api/usuarios/{id}   POST /api/login -> token
public class ServidorATC {

    // id -> {user, password}
    static final Map<String, String[]> USUARIOS = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        USUARIOS.put("1", new String[]{"comercio", "1234"});
        USUARIOS.put("2", new String[]{"admin", "admin"});

        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
        server.createContext("/api/login", ServidorATC::login);
        server.createContext("/api/usuarios", ServidorATC::usuarios);
        server.start();
        System.out.println("ATC en http://localhost:8083/api/login");
    }

    static void login(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { responder(ex, 405, "{\"error\":\"use POST\"}"); return; }
        String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String user = campo(cuerpo, "user");
        String password = campo(cuerpo, "password");

        for (String[] u : USUARIOS.values()) {
            if (u[0].equals(user) && u[1].equals(password)) {
                responder(ex, 200, "{\"token\":\"" + JwtJava.crear(user) + "\",\"type\":\"bearer\"}");
                return;
            }
        }
        responder(ex, 401, "{\"error\":\"credenciales invalidas\"}");
    }

    static void usuarios(HttpExchange ex) throws IOException {
        String id = ex.getRequestURI().getPath().replaceFirst("/api/usuarios/?", "");
        List<String> lista = new ArrayList<>();
        USUARIOS.forEach((k, u) -> {
            if (id.isEmpty() || id.equals(k)) lista.add("{\"id\":\"" + k + "\",\"user\":\"" + u[0] + "\"}");
        });
        if (lista.isEmpty()) responder(ex, 404, "{\"error\":\"no existe\"}");
        else responder(ex, 200, id.isEmpty() ? "[" + String.join(",", lista) + "]" : lista.get(0));
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static void responder(HttpExchange ex, int codigo, String cuerpo) throws IOException {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(codigo, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }
}
