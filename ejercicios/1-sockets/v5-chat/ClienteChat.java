import java.io.*;
import java.net.*;

// Necesita DOS hilos: uno escucha al servidor y otro lee el teclado.
public class ClienteChat {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 5002);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Thread receptor = new Thread(() -> {
            try {
                String linea;
                while ((linea = in.readLine()) != null) System.out.println(linea);
            } catch (IOException e) {
                System.out.println("Conexion cerrada");
            }
        });
        receptor.setDaemon(true);
        receptor.start();

        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null) {
            out.println(linea);
            if (linea.equals("/salir")) break;
        }
        socket.close();
    }
}
