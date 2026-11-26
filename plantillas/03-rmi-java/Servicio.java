import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// IMPLEMENTACION del contrato. Aqui trabaja el servidor.
public class Servicio extends UnicastRemoteObject implements IServicio {

    private static final long serialVersionUID = 1L;

    public Servicio() throws RemoteException {
        super();
    }

    @Override
    public Respuesta procesar(String dato) throws RemoteException {
        System.out.println("Me llamaron con: " + dato);
        // AQUI LOGICA  (aqui se llama a los sockets, bancos, etc.)
        return new Respuesta(true, "Recibido: " + dato);
    }
}
