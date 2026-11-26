import java.io.*;
import java.net.*;

// CLIENTE TCP. El metodo enviar() se puede copiar dentro de un servidor RMI u otro programa.
public class ClienteTCP {

    public static void main(String[] args) {
        String mensaje = args.length > 0 ? args[0] : "hola";
        System.out.println("Respuesta: " + enviar("localhost", 5002, mensaje));
    }

    static String enviar(String host, int puerto, String mensaje) {
        try (Socket client = new Socket(host, puerto);
             PrintStream toServer = new PrintStream(client.getOutputStream());
             BufferedReader fromServer = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            client.setSoTimeout(5000);          // no esperar para siempre
            toServer.println(mensaje);
            return fromServer.readLine();

        } catch (IOException e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
