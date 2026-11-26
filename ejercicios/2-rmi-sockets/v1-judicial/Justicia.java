import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// INTERMEDIARIO: hacia el Juez es servidor RMI, hacia los bancos es cliente TCP y UDP.
public class Justicia extends UnicastRemoteObject implements IJusticia {
    private static final long serialVersionUID = 1L;

    public Justicia() throws RemoteException {
        super();
    }

    @Override
    public RespuestaCuenta consultarCuentas(String ci, String nombres, String apellidos) throws RemoteException {
        RespuestaCuenta r = new RespuestaCuenta();

        agregar(r, Banco.MERCANTIL, ClienteTCP.enviar("localhost", 5002, "buscar:" + ci), ci, nombres, apellidos);
        agregar(r, Banco.BCP, ClienteUDP.enviar("localhost", 6789, "buscar:" + ci), ci, nombres, apellidos);

        if (!r.error && r.cuentas.isEmpty()) r.mensaje = "No tiene cuentas";
        return r;
    }

    // El banco responde:  cuenta1-saldo1:cuenta2-saldo2   (vacio si no tiene)
    private void agregar(RespuestaCuenta r, Banco banco, String texto, String ci, String nombres, String apellidos) {
        if (texto == null || texto.startsWith("ERROR")) {
            r.error = true;
            r.mensaje = "No responde el banco " + banco;
            return;
        }
        if (texto.isEmpty()) return;

        for (String par : texto.split(":")) {
            String[] p = par.split("-");
            r.cuentas.add(new Cuenta(banco, p[0], ci, nombres, apellidos, Double.parseDouble(p[1])));
        }
    }

    @Override
    public boolean congelar(Cuenta cuenta, double monto) throws RemoteException {
        String mensaje = "congelar:" + cuenta.getNrocuenta() + ":" + monto;
        String respuesta = cuenta.getBanco() == Banco.MERCANTIL
                ? ClienteTCP.enviar("localhost", 5002, mensaje)
                : ClienteUDP.enviar("localhost", 6789, mensaje);
        return "ok".equals(respuesta);
    }
}
