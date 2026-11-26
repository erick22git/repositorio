import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.RspList;

// Invocacion remota EN GRUPO: una llamada se ejecuta en TODOS los miembros.
// Comandos:  inc 5    ver    /salir
public class ContadorRPC implements Receiver {

    private final AtomicInteger contador = new AtomicInteger();

    // Metodos que se invocan a distancia (deben ser public)
    public int incrementar(int n) {
        int nuevo = contador.addAndGet(n);
        System.out.println("-> incrementar(" + n + "), ahora vale " + nuevo);
        return nuevo;
    }

    public int consultar() {
        return contador.get();
    }

    @Override
    public void viewAccepted(View vista) {
        System.out.println("** Miembros: " + vista.getMembers());
    }

    public static void main(String[] args) throws Exception {
        ContadorRPC yo = new ContadorRPC();
        JChannel canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(args.length > 0 ? args[0] : "nodo");

        RpcDispatcher despachador = new RpcDispatcher(canal, yo);
        despachador.setReceiver(yo);
        canal.connect("ContadorSIS258");

        RequestOptions todos = new RequestOptions(ResponseMode.GET_ALL, 5000);
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null && !linea.equals("/salir")) {
            MethodCall llamada;
            if (linea.startsWith("inc ")) {
                int n = Integer.parseInt(linea.substring(4).trim());
                llamada = new MethodCall("incrementar", new Object[]{n}, new Class<?>[]{int.class});
            } else if (linea.equals("ver")) {
                llamada = new MethodCall("consultar", new Object[0], new Class<?>[0]);
            } else {
                System.out.println("Use:  inc 5  |  ver  |  /salir");
                continue;
            }
            RspList<Object> respuestas = despachador.callRemoteMethods(null, llamada, todos);
            respuestas.forEach((miembro, rsp) -> System.out.println("  " + miembro + " => " + rsp.getValue()));
        }
        canal.close();
    }
}
