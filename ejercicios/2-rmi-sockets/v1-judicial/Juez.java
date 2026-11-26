import java.rmi.registry.LocateRegistry;

public class Juez {
    public static void main(String[] args) throws Exception {
        IJusticia justicia = (IJusticia) LocateRegistry.getRegistry("localhost", 1099).lookup("Justicia");

        RespuestaCuenta r = justicia.consultarCuentas("11021654", "Juan Perez", "Segovia");
        System.out.println(r);

        // Ordena congelar 100 en la primera cuenta encontrada y vuelve a consultar
        if (!r.cuentas.isEmpty()) {
            boolean ok = justicia.congelar(r.cuentas.get(0), 100);
            System.out.println("Congelado: " + ok);
            System.out.println(justicia.consultarCuentas("11021654", "Juan Perez", "Segovia"));
        }
    }
}
