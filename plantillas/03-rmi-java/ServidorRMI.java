import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Levanta el registro RMI y publica el objeto con un NOMBRE.
public class ServidorRMI {

    public static void main(String[] args) throws Exception {
        // Solo si el cliente esta en OTRA computadora, quitar // y poner tu IP:
        // System.setProperty("java.rmi.server.hostname", "192.168.1.10");

        Registry reg = LocateRegistry.createRegistry(1099);
        reg.rebind("Servicio", new Servicio());   // el cliente usa este mismo nombre
        System.out.println("Servidor RMI listo en el puerto 1099");
    }
}
