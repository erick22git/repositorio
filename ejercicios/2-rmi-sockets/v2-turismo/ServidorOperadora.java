import java.rmi.registry.LocateRegistry;

public class ServidorOperadora {
    public static void main(String[] args) throws Exception {
        LocateRegistry.createRegistry(1099).rebind("Operadora", new Operadora());
        System.out.println("Operadora (RMI) lista en 1099");
    }
}
