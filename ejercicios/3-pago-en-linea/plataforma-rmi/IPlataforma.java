import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPlataforma extends Remote {
    boolean realizarTransaccion(String nrocuentacliente, String cuentavendedor,
                                double monto, String moneda) throws RemoteException;
}
