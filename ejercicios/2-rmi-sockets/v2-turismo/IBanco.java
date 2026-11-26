import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBanco extends Remote {
    Pago debitar(String pasaporte, double montoUSD) throws RemoteException;
}
