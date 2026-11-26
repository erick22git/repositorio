import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Turista {
    public static void main(String[] args) throws Exception {
        IOperadora operadora = (IOperadora) LocateRegistry.getRegistry("localhost", 1099).lookup("Operadora");

        Scanner sc = new Scanner(System.in);
        System.out.print("Pasaporte: ");
        String pasaporte = sc.nextLine();
        System.out.print("Codigo de tour (T1, T2): ");
        String tour = sc.nextLine();
        System.out.print("Personas: ");
        int personas = Integer.parseInt(sc.nextLine().trim());

        System.out.println(operadora.comprarTour(pasaporte, tour, personas));
    }
}
