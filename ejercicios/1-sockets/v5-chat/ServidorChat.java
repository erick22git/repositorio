import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;

// CHAT: lo que escribe uno les llega a todos.
// Comandos: /quien  /privado nombre texto  /salir
public class ServidorChat {

    static final Set<Cliente> CLIENTES = new CopyOnWriteArraySet<>();   // seguro entre hilos
    static final ExecutorService HILOS = Executors.newFixedThreadPool(20);

    record Cliente(String nombre, PrintWriter out) {}

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(5002)) {
            System.out.println("Chat en el puerto 5002");
            while (true) {
                Socket socket = server.accept();
                HILOS.execute(() -> atender(socket));
            }
        }
    }

    static void atender(Socket socket) {
        Cliente yo = null;
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("Tu nombre:");
            String nombre = in.readLine();
            if (nombre == null) return;
            yo = new Cliente(nombre.trim(), out);
            CLIENTES.add(yo);
            difundir("* " + yo.nombre() + " entro al chat", yo);

            String linea;
            while ((linea = in.readLine()) != null && !linea.equals("/salir")) {
                if (linea.equals("/quien")) {
                    CLIENTES.forEach(c -> out.println("- " + c.nombre()));
                } else if (linea.startsWith("/privado ")) {
                    privado(yo, linea);
                } else {
                    difundir(yo.nombre() + ": " + linea, yo);
                }
            }
        } catch (IOException e) {
            System.out.println("Un cliente se cayo: " + e.getMessage());
        } finally {
            if (yo != null) {
                CLIENTES.remove(yo);
                difundir("* " + yo.nombre() + " salio", yo);
            }
        }
    }

    static void difundir(String texto, Cliente menos) {
        System.out.println(texto);
        for (Cliente c : CLIENTES) {
            if (c != menos) c.out().println(texto);
        }
    }

    static void privado(Cliente yo, String linea) {
        String[] p = linea.split(" ", 3);   // /privado ana hola que tal
        if (p.length < 3) { yo.out().println("Uso: /privado nombre texto"); return; }
        for (Cliente c : CLIENTES) {
            if (c.nombre().equalsIgnoreCase(p[1])) {
                c.out().println("(privado) " + yo.nombre() + ": " + p[2]);
                return;
            }
        }
        yo.out().println("No existe " + p[1]);
    }
}
