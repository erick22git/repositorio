import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IOperadora extends Remote {
    Voucher comprarTour(String pasaporte, String codigoTour, int personas) throws RemoteException;
}
