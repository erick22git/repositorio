import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClienteRMI {

    public static void main(String[] args) throws Exception {
        String dato = args.length > 0 ? args[0] : "hola";

        Registry reg = LocateRegistry.getRegistry("localhost", 1099);
        IServicio servicio = (IServicio) reg.lookup("Servicio");  // mismo nombre que rebind

        // AQUI LOGICA (pedir datos, mostrar menu, etc.)
        Respuesta r = servicio.procesar(dato);
        System.out.println(r);
    }
}
