import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class SitioComercio {
    public static void main(String[] args) throws Exception {
        IPlataforma plataforma = (IPlataforma) LocateRegistry.getRegistry("localhost", 1099).lookup("Plataforma");

        Scanner sc = new Scanner(System.in);
        System.out.print("Cuenta del cliente: ");
        String origen = sc.nextLine().trim();
        System.out.print("Cuenta del vendedor: ");
        String destino = sc.nextLine().trim();
        System.out.print("Monto: ");
        double monto = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Moneda (Bs / USD): ");
        String moneda = sc.nextLine().trim();

        boolean ok = plataforma.realizarTransaccion(origen, destino, monto, moneda);
        System.out.println(ok ? "PAGO APROBADO" : "PAGO RECHAZADO");
    }
}
