import java.io.*;
import java.net.*;

// El servidor PREGUNTA y el cliente responde, varias veces, en la MISMA conexion.
public class ServidorTCP {

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(5002)) {
            System.out.println("Servidor en el puerto 5002");
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

            String linea;
            do {
                out.println("Primer numero:");
                double a = Double.parseDouble(in.readLine());
                out.println("Segundo numero:");
                double b = Double.parseDouble(in.readLine());
                out.println("Operacion (+ - * /):");
                String op = in.readLine();

                double r = switch (op) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> a / b;
                    default  -> Double.NaN;
                };
                out.println("Resultado " + r + ". Otra vez? (s/n)");
                linea = in.readLine();
            } while ("s".equalsIgnoreCase(linea));

            out.println("FIN");

        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        }
    }
}
