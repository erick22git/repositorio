import java.rmi.registry.LocateRegistry;

public class ServidorBanco {
    public static void main(String[] args) throws Exception {
        // Puerto 1100: el 1099 ya lo ocupa la Operadora
        LocateRegistry.createRegistry(1100).rebind("Banco", new BancoRMI());
        System.out.println("Banco (RMI) listo en 1100");
    }
}
