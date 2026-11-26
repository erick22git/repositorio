import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.Util;
public class NodoPuerta implements Receiver {
    private JChannel canal;
    private final String nombre;
    private final int aforo;
    // estado replicado: igual en todas las puertas
    private int ocupacion = 0;
    private View vistaAnterior;
    public NodoPuerta(String nombre, int aforo) {
        this.nombre = nombre;
        this.aforo = aforo;
    }
    public void iniciar() throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect("AforoSIS258");
        canal.getState(null, 10000);   // la puerta que llega tarde pide la ocupacion
        leerTeclado();
        canal.close();
    }
    private boolean soyCoordinador() {
        return canal.getAddress().equals(canal.getView().getCoord());
    }
    // ================= membresia =================
    @Override
    public void viewAccepted(View vista) {
        if (vistaAnterior != null) {
            Address[][] cambios = View.diff(vistaAnterior, vista);
            for (Address a : cambios[0]) {
                System.out.println("** ENTRO la puerta: " + a);
            }
            for (Address a : cambios[1]) {
                System.out.println("** SALIO la puerta: " + a);
            }
        }
        vistaAnterior = vista;
        System.out.println("** Coordinador actual: " + vista.getCoord() + " | puertas: " + vista.getMembers());
    }
    // ================= mensajes =================
    @Override
    public void receive(Message msg) {
        MensajeAforo m = msg.getObject();
        switch (m.getTipo()) {
            case SOLICITUD:
                atenderSolicitud(m, msg.getSrc());
                break;
            case ACEPTADO:
                // el coordinador ya lo sumo al decidir, no lo suma dos veces
                if (!msg.getSrc().equals(canal.getAddress())) {
                    sumar(m.getPersonas());
                }
                System.out.println(">> Ingresaron " + m.getPersonas() + " por la puerta " + m.getPuerta()
                        + " | ocupacion: " + getOcupacion() + "/" + aforo);
                avisarAforoCompleto();
                break;
            case RECHAZADO:
                System.out.println(">> RECHAZADO: no pueden ingresar " + m.getPersonas()
                        + " personas, ocupacion " + getOcupacion() + "/" + aforo);
                break;
            case SALIDA:
                restar(m.getPersonas());
                System.out.println(">> Salieron " + m.getPersonas() + " por la puerta " + m.getPuerta()
                        + " | ocupacion: " + getOcupacion() + "/" + aforo);
                break;
            default:
                break;
        }
    }
    // solo el coordinador decide; comprobar y sumar van juntos
    // para que dos solicitudes seguidas no pasen las dos
    private void atenderSolicitud(MensajeAforo m, Address origen) {
        if (!soyCoordinador()) {
            return;
        }
        boolean aceptado;
        synchronized (this) {
            aceptado = ocupacion + m.getPersonas() <= aforo;
            if (aceptado) {
                ocupacion += m.getPersonas();
            }
        }
        try {
            if (aceptado) {
                // multicast: todas las puertas suman
                canal.send(new ObjectMessage(null, new MensajeAforo(TipoMensaje.ACEPTADO, m.getPuerta(), m.getPersonas())));
            } else {
                // unicast: solo a la puerta que pidio
                canal.send(new ObjectMessage(origen, new MensajeAforo(TipoMensaje.RECHAZADO, m.getPuerta(), m.getPersonas())));
            }
        } catch (Exception ex) {
            System.out.println("Error respondiendo: " + ex.getMessage());
        }
    }
    // ================= estado replicado =================
    @Override
    public void getState(OutputStream salida) throws Exception {
        synchronized (this) {
            Util.objectToStream(ocupacion, new DataOutputStream(salida));
        }
    }
    @Override
    public void setState(InputStream entrada) throws Exception {
        int recibido = Util.objectFromStream(new DataInputStream(entrada));
        synchronized (this) {
            ocupacion = recibido;
        }
        System.out.println("** Estado recibido: ocupacion " + recibido + "/" + aforo);
    }
    private synchronized void sumar(int n) {
        ocupacion += n;
    }
    private synchronized void restar(int n) {
        ocupacion = Math.max(0, ocupacion - n);
    }
    private synchronized int getOcupacion() {
        return ocupacion;
    }
    private void avisarAforoCompleto() {
        if (getOcupacion() >= aforo) {
            System.out.println("!!!!! AFORO COMPLETO !!!!!");
        }
    }
    // ================= comandos =================
    private void leerTeclado() throws Exception {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Comandos: /entrar n | /salir n | /estado | /fin");
        String linea;
        while ((linea = teclado.readLine()) != null) {
            String[] partes = linea.trim().split(" ");
            try {
                if (partes[0].equals("/entrar") && partes.length == 2) {
                    int n = Integer.parseInt(partes[1]);
                    // unicast al coordinador, primer miembro de la vista
                    Address coord = canal.getView().getCoord();
                    canal.send(new ObjectMessage(coord, new MensajeAforo(TipoMensaje.SOLICITUD, nombre, n)));
                } else if (partes[0].equals("/salir") && partes.length == 2) {
                    int n = Integer.parseInt(partes[1]);
                    canal.send(new ObjectMessage(null, new MensajeAforo(TipoMensaje.SALIDA, nombre, n)));
                } else if (partes[0].equals("/estado")) {
                    System.out.println("Ocupacion: " + getOcupacion() + "/" + aforo
                            + " | coordinador: " + canal.getView().getCoord());
                    avisarAforoCompleto();
                } else if (partes[0].equals("/fin")) {
                    break;
                } else {
                    System.out.println("Comando no valido");
                }
            } catch (NumberFormatException ex) {
                System.out.println("n debe ser un numero");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        if (args.length < 2) {
            System.out.println("Uso: java NodoPuerta <nombre> <aforoMaximo>");
            return;
        }
        String nombre = args[0];
        int aforo = Integer.parseInt(args[1]);
        new NodoPuerta(nombre, aforo).iniciar();
    }
}
