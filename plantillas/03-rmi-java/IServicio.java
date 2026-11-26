import java.rmi.Remote;
import java.rmi.RemoteException;

// CONTRATO: lo que el cliente puede llamar a distancia.
public interface IServicio extends Remote {

    // TODOS los metodos llevan "throws RemoteException"
    Respuesta procesar(String dato) throws RemoteException;
}
