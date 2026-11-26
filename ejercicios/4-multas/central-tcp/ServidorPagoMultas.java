import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.regex.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorPagoMultas {

    static final int PUERTO = 5002;   // cambiar si el enunciado pide otro

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servidor TCP escuchando en el puerto " + PUERTO);
            while (true) {
                Socket client = server.accept();            // se queda esperando un cliente
                new Thread(() -> atender(client)).start();  // un hilo por cliente
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    static void atender(Socket client) {
        try (client;
             BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream toClient = new PrintStream(client.getOutputStream())) {

            String recibido = fromClient.readLine();
            System.out.println("Llego: " + recibido);
            String respuesta = procesarSolicitud(recibido);
            toClient.println(respuesta);
            System.out.println("Respondi: " + respuesta);

        } catch (Exception e) {
            System.out.println("Error con un cliente: " + e.getMessage());
        }
    }

    static final String REST = "http://localhost:8000/api/multas/";

    // PAGAR:codigo:ci   ->  OK:multa pagada  |  ERROR:motivo
    // synchronized: la misma multa no se puede cobrar dos veces a la vez
    static synchronized String procesarSolicitud(String mensaje) {
        try {
            String[] p = mensaje.split(":");
            if (p.length != 3 || !p[0].equals("PAGAR")) return "ERROR:use PAGAR:codigo:ci";

            HttpResponse<String> r = pedir("GET", REST + URLEncoder.encode(p[1], "UTF-8"), null);
            if (r.statusCode() == 404) return "ERROR:la multa no existe";
            if (r.statusCode() != 200) return "ERROR:la central respondio " + r.statusCode();

            if (!p[2].equals(campo(r.body(), "ci"))) return "ERROR:la multa no es de ese conductor";

            String estado = campo(r.body(), "estado");
            if (!"pendiente".equals(estado)) return "ERROR:la multa esta " + estado;

            r = pedir("PATCH", REST + URLEncoder.encode(p[1], "UTF-8"), "{\"estado\":\"pagada\"}");
            return r.statusCode() == 200 ? "OK:multa pagada" : "ERROR:no se pudo registrar el pago";

        } catch (Exception e) {
            return "ERROR:la central REST no responde";
        }
    }

    static HttpResponse<String> pedir(String metodo, String url, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                                             : HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
