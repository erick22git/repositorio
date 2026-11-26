import java.io.*;
import java.util.HashMap;
import java.util.concurrent.*;
import org.jgroups.*;
import org.jgroups.util.Util;

// REMATE sin servidor central. Comandos:
//   /crear articulo base segundos   /pujar articulo monto   /subastas
//   /estado articulo   /quien   /ganadas   /salir
public class Remate implements Receiver {

    private JChannel canal;
    private final HashMap<String, Subasta> subastas = new HashMap<>();   // estado replicado

    // ---------- JGroups ----------
    @Override
    public void viewAccepted(View vista) {
        System.out.println("** Miembros: " + vista.getMembers() + " | coordinador: " + vista.getCoord());
    }

    @Override
    public void receive(Message msg) {
        Mensaje m = msg.getObject();
        switch (m.tipo) {
            case CREAR, PUJAR -> decidir(msg.getSrc(), m);      // solo llegan al coordinador
            case RECHAZADA    -> System.out.println("RECHAZADO: " + m.texto);
            default           -> aplicar(m);                    // decisiones: las aplican todos
        }
    }

    // Solo el coordinador decide: asi dos pujas simultaneas se resuelven en UN orden
    private void decidir(Address quien, Mensaje m) {
        synchronized (subastas) {
            Subasta s = subastas.get(m.articulo);
            String error = null;
            Mensaje decision = null;

            if (m.tipo == Mensaje.Tipo.CREAR) {
                if (s != null) error = "ya existe " + m.articulo;
                else decision = new Mensaje(Mensaje.Tipo.NUEVA, m.articulo, m.monto,
                        System.currentTimeMillis() + m.dato * 1000, quien.toString());
            } else {
                if (s == null) error = "no existe la subasta " + m.articulo;
                else if (!s.abierta || System.currentTimeMillis() >= s.cierraEn) error = "ya cerro";
                else if (m.monto <= s.mejor) error = "debe superar " + s.mejor;
                else decision = new Mensaje(Mensaje.Tipo.ACEPTADA, m.articulo, m.monto, 0, quien.toString());
            }

            if (error != null) {
                enviar(quien, new Mensaje(Mensaje.Tipo.RECHAZADA, m.articulo, 0, 0, error));
            } else {
                aplicar(decision);            // el coordinador lo aplica YA (para la siguiente decision)
                enviar(null, decision);       // y lo difunde a todos
            }
        }
    }

    // aplicar() se puede llamar dos veces en el coordinador sin duplicar nada
    private void aplicar(Mensaje m) {
        synchronized (subastas) {
            Subasta s = subastas.get(m.articulo);
            if (m.tipo == Mensaje.Tipo.NUEVA && s == null) {
                subastas.put(m.articulo, new Subasta(m.articulo, m.monto, m.dato));
                System.out.println("NUEVA subasta: " + m.articulo + " base " + m.monto);
            } else if (m.tipo == Mensaje.Tipo.ACEPTADA && s != null && m.monto > s.mejor) {
                s.mejor = m.monto;
                s.postor = m.texto;
                s.historial.add(m.texto + " -> " + m.monto);
                System.out.println("PUJA: " + m.texto + " ofrece " + m.monto + " por " + m.articulo);
            } else if (m.tipo == Mensaje.Tipo.CIERRE && s != null && s.abierta) {
                s.abierta = false;
                System.out.println("CERRADA " + m.articulo + " | gana " + s.postor + " con " + s.mejor);
            }
        }
    }

    @Override
    public void getState(OutputStream salida) throws Exception {
        synchronized (subastas) {
            Util.objectToStream(subastas, new DataOutputStream(salida));
        }
    }

    @Override
    public void setState(InputStream entrada) throws Exception {
        HashMap<String, Subasta> recibido = Util.objectFromStream(new DataInputStream(entrada));
        synchronized (subastas) {
            subastas.clear();
            subastas.putAll(recibido);
        }
        System.out.println("** Estado recibido: " + recibido.size() + " subasta(s)");
    }

    // ---------- Utilidades ----------
    private boolean soyCoordinador() {
        View v = canal.getView();
        return v != null && v.getCoord().equals(canal.getAddress());
    }

    private void enviar(Address destino, Mensaje m) {
        try {
            canal.send(new ObjectMessage(destino, m));
        } catch (Exception e) {
            System.out.println("No se pudo enviar: " + e.getMessage());
        }
    }

    // Cada medio segundo: SOLO el coordinador cierra lo vencido.
    // Si el coordinador cae, el nuevo sigue solo porque tiene el mismo estado.
    private void cerrarVencidas() {
        if (!soyCoordinador()) return;
        synchronized (subastas) {
            for (Subasta s : subastas.values()) {
                if (s.abierta && System.currentTimeMillis() >= s.cierraEn) {
                    Mensaje cierre = new Mensaje(Mensaje.Tipo.CIERRE, s.articulo, 0, 0, "");
                    aplicar(cierre);
                    enviar(null, cierre);
                }
            }
        }
    }

    // ---------- Consola ----------
    private void comando(String linea) {
        String[] p = linea.trim().split("\\s+");
        try {
            switch (p[0]) {
                case "/crear" -> enviar(canal.getView().getCoord(), new Mensaje(Mensaje.Tipo.CREAR, p[1],
                        Double.parseDouble(p[2]), Long.parseLong(p[3]), ""));
                case "/pujar" -> enviar(canal.getView().getCoord(), new Mensaje(Mensaje.Tipo.PUJAR, p[1],
                        Double.parseDouble(p[2]), 0, ""));
                case "/subastas" -> {
                    synchronized (subastas) { subastas.values().forEach(System.out::println); }
                }
                case "/estado" -> {
                    synchronized (subastas) {
                        Subasta s = subastas.get(p[1]);
                        System.out.println(s == null ? "No existe" : s + "\n  " + String.join("\n  ", s.historial));
                    }
                }
                case "/ganadas" -> {
                    synchronized (subastas) {
                        double total = 0;
                        for (Subasta s : subastas.values()) {
                            if (!s.abierta && s.postor.equals(canal.getName())) {
                                System.out.println(s);
                                total += s.mejor;
                            }
                        }
                        System.out.println("Total a pagar: " + total);
                    }
                }
                case "/quien" -> System.out.println(canal.getView().getMembers()
                        + " | coordinador: " + canal.getView().getCoord());
                default -> System.out.println("Comando desconocido");
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            System.out.println("Faltan datos o no son numeros");
        }
    }

    public static void main(String[] args) throws Exception {
        Remate r = new Remate();
        r.canal = new JChannel(System.getProperty("config", "udp.xml"));
        r.canal.name(args.length > 0 ? args[0] : "postor" + (int) (Math.random() * 100));
        r.canal.setReceiver(r);
        r.canal.connect("RemateSIS258");
        r.canal.getState(null, 10000);

        ScheduledExecutorService reloj = Executors.newSingleThreadScheduledExecutor();
        reloj.scheduleAtFixedRate(r::cerrarVencidas, 500, 500, TimeUnit.MILLISECONDS);

        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null && !linea.equals("/salir")) {
            r.comando(linea);
        }
        reloj.shutdownNow();
        r.canal.close();
    }
}
