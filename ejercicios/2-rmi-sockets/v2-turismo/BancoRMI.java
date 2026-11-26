import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

// Banco: servidor RMI y a la vez cliente UDP del antifraude.
public class BancoRMI extends UnicastRemoteObject implements IBanco {
    private static final long serialVersionUID = 1L;

    private final Map<String, Double> saldos = new HashMap<>(Map.of("123", 5000.0, "456", 100.0));

    public BancoRMI() throws RemoteException {
        super();
    }

    @Override
    public synchronized Pago debitar(String pasaporte, double montoUSD) throws RemoteException {
        Double saldo = saldos.get(pasaporte);
        if (saldo == null) return new Pago(false, "sin cuenta");
        if (saldo < montoUSD) return new Pago(false, "saldo insuficiente");

        String riesgo = ClienteUDP.enviar("localhost", 6789, String.valueOf(montoUSD));
        if (riesgo.startsWith("ERROR")) return new Pago(false, "antifraude no responde");

        saldos.put(pasaporte, saldo - montoUSD);
        return new Pago(true, "pagado, riesgo " + riesgo);
    }
}
