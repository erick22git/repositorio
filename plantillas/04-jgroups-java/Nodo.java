import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jgroups.*;
import org.jgroups.util.Util;

// NODO de un grupo JGroups. Todos los nodos son iguales: no hay servidor.
public class Nodo implements Receiver {

    private static final String GRUPO = "GrupoExamen";   // todos deben usar el mismo nombre

    private JChannel canal;
    private final List<String> estado = new ArrayList<>();   // copia que tiene CADA nodo

    // Se ejecuta cuando alguien entra o sale del grupo
    @Override
    public void viewAccepted(View vista) {
        System.out.println("** Miembros: " + vista.getMembers() + " | coordinador: " + vista.getCoord());
    }

    // Se ejecuta cuando llega un mensaje
    @Override
    public void receive(Message msg) {
        String texto = msg.getObject();
        // AQUI LOGICA  (que hacer con lo que llega)
        synchronized (estado) {
            estado.add(msg.getSrc() + ": " + texto);
        }
        System.out.println(msg.getSrc() + "> " + texto);
    }

    // Lo ejecuta el nodo que YA tiene los datos, para dárselos al nuevo
    @Override
    public void getState(OutputStream salida) throws Exception {
        synchronized (estado) {
            Util.objectToStream(estado, new DataOutputStream(salida));
        }
    }

    // Lo ejecuta el nodo que ACABA de entrar
    @Override
    public void setState(InputStream entrada) throws Exception {
        List<String> recibido = Util.objectFromStream(new DataInputStream(entrada));
        synchronized (estado) {
            estado.clear();
            estado.addAll(recibido);
        }
        System.out.println("** Estado recibido: " + recibido);
    }

    boolean soyCoordinador() {
        return canal.getView().getCoord().equals(canal.getAddress());
    }

    void iniciar(String nombre) throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect(GRUPO);
        canal.getState(null, 10000);   // pide los datos al coordinador

        System.out.println("Escriba mensajes.  /quien  /historial  /salir");
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null) {
            if (linea.equals("/salir")) break;
            else if (linea.equals("/quien")) {
                System.out.println(canal.getView().getMembers() + " | soy coordinador: " + soyCoordinador());
            } else if (linea.equals("/historial")) {
                synchronized (estado) { estado.forEach(System.out::println); }
            } else {
                // AQUI LOGICA  (comandos propios del ejercicio)
                canal.send(new ObjectMessage(null, linea));   // null = a TODO el grupo
            }
        }
        canal.close();
    }

    public static void main(String[] args) throws Exception {
        String nombre = args.length > 0 ? args[0] : "nodo" + (int) (Math.random() * 100);
        new Nodo().iniciar(nombre);
    }
}
