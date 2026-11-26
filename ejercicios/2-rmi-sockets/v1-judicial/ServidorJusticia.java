import java.rmi.registry.LocateRegistry;

public class ServidorJusticia {
    public static void main(String[] args) throws Exception {
        LocateRegistry.createRegistry(1099).rebind("Justicia", new Justicia());
        System.out.println("Servidor Justicia (RMI) listo en 1099");
    }
}
