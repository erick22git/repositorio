import java.io.*;
import java.net.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorTCP {

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

    // Protocolo:  sumar:3:4  ->  ok:7.0     |   error:motivo
    static String procesarSolicitud(String mensaje) {
        try {
            String[] p = mensaje.split(":");
            double a = Double.parseDouble(p[1]);
            double b = Double.parseDouble(p[2]);

            return switch (p[0].toLowerCase()) {
                case "sumar"       -> "ok:" + (a + b);
                case "restar"      -> "ok:" + (a - b);
                case "multiplicar" -> "ok:" + (a * b);
                case "dividir"     -> b == 0 ? "error:division por cero" : "ok:" + (a / b);
                default            -> "error:operacion desconocida";
            };
        } catch (Exception e) {
            return "error:use operacion:numero:numero";
        }
    }
}
