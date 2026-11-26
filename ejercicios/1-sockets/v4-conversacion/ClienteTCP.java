import java.io.*;
import java.net.*;

// Lee lo que dice el servidor, contesta por teclado, hasta recibir FIN.
public class ClienteTCP {

    public static void main(String[] args) throws IOException {
        try (Socket client = new Socket("localhost", 5002);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream out = new PrintStream(client.getOutputStream());
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            String delServidor;
            while ((delServidor = in.readLine()) != null && !delServidor.equals("FIN")) {
                System.out.println(delServidor);
                String respuesta = teclado.readLine();
                if (respuesta == null) break;
                out.println(respuesta);
            }
            System.out.println("Conversacion terminada");
        }
    }
}
