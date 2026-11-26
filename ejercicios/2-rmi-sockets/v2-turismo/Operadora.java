import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

// ORQUESTADOR: 1) Migracion (TCP)  2) Banco (RMI, que consulta antifraude UDP)  3) voucher
public class Operadora extends UnicastRemoteObject implements IOperadora {
    private static final long serialVersionUID = 1L;

    private static final Map<String, Double> PRECIOS = Map.of("T1", 300.0, "T2", 800.0);

    public Operadora() throws RemoteException {
        super();
    }

    @Override
    public Voucher comprarTour(String pasaporte, String codigoTour, int personas) throws RemoteException {
        Double precio = PRECIOS.get(codigoTour);
        if (precio == null || personas <= 0) return new Voucher(false, "tour o cantidad invalida", 0);
        double total = precio * personas;

        String migracion = ClienteTCP.enviar("localhost", 5002, "pasaporte:" + pasaporte);
        if (migracion == null || !migracion.startsWith("valido")) {
            return new Voucher(false, "Migracion: " + migracion, total);
        }

        try {
            IBanco banco = (IBanco) LocateRegistry.getRegistry("localhost", 1100).lookup("Banco");
            Pago pago = banco.debitar(pasaporte, total);
            return new Voucher(pago.aprobado, pago.detalle, total);
        } catch (Exception e) {
            return new Voucher(false, "Banco no disponible", total);
        }
    }
}
