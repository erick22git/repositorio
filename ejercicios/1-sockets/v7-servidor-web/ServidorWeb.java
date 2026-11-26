import java.io.*;
import java.net.*;

// Un servidor web hecho solo con sockets: el navegador es el cliente.
public class ServidorWeb {

    public static void main(String[] args) throws IOException {
        int puerto = 8090;     // en el repo del curso usa el 80
        try (ServerSocket server = new ServerSocket(puerto)) {
            System.out.println("Abrir http://localhost:" + puerto + "/?nombre=Ana");
            while (true) {
                Socket client = server.accept();
                new Thread(() -> atender(client)).start();
            }
        }
    }

    static void atender(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream out = new PrintStream(client.getOutputStream())) {

            String peticion = in.readLine();          // GET /?nombre=Ana HTTP/1.1
            if (peticion == null) return;
            System.out.println(peticion);
            String linea;
            while ((linea = in.readLine()) != null && !linea.isEmpty()) { } // saltar cabeceras

            String ruta = peticion.split(" ")[1];
            String nombre = ruta.contains("nombre=") ? ruta.substring(ruta.indexOf("nombre=") + 7) : "visitante";
            String html = "<html><body><h1>Hola " + nombre.replaceAll("[<>]", "") + "</h1></body></html>";

            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Content-Type: text/html; charset=utf-8\r\n");
            out.print("Content-Length: " + html.getBytes("UTF-8").length + "\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");                       // linea vacia = fin de cabeceras
            out.write(html.getBytes("UTF-8"));
            out.flush();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
