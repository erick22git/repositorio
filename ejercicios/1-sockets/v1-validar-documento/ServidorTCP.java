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

    // Pasaportes (o CI) registrados:  numero -> pais
    static final java.util.Map<String, String> REGISTRO = java.util.Map.of(
            "123", "Bolivia",
            "456", "Peru",
            "11021654", "Bolivia");

    // Protocolo:  pasaporte:123   ->  valido:Bolivia   |   invalido:motivo
    static String procesarSolicitud(String mensaje) {
        if (mensaje == null) return "invalido:mensaje vacio";

        String[] partes = mensaje.split(":");
        if (partes.length != 2) return "invalido:formato";

        String numero = partes[1].trim();
        String pais = REGISTRO.get(numero);
        return pais != null ? "valido:" + pais : "invalido:no registrado";
    }
}
