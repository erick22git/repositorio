import java.rmi.registry.LocateRegistry;

public class ServidorPlataforma {
    public static void main(String[] args) throws Exception {
        LocateRegistry.createRegistry(1099).rebind("Plataforma", new Plataforma());
        System.out.println("Plataforma de Intercambio (RMI) en 1099");
    }
}
