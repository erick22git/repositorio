# Ejercicio 7 · JGroups (comunicación en grupo)

[← Ejercicios](../README.md) · [Índice](../../README.md) · **[PRÁCTICA 3]**

| Versión | Me piden… |
|---|---|
| [1](#versión-1--chat-replicado) | Chat con historial para el que llega tarde |
| [2](#versión-2--subasta-sin-servidor) | Subasta sin servidor central |
| [3](#versión-3--invocación-en-grupo) | Ejecutar un método en todos (RPC) |

Antes: [tener el jar](../../00-preparacion/README.md#d5-agregar-el-jar-de-jgroups) y leer la [plantilla 04](../../plantillas/04-jgroups-java/README.md) (cómo ejecutar y qué hacer si no se ven).

**Siempre:** abre varias terminales, arranca los nodos **con unos segundos de diferencia** y agrega `-Djava.net.preferIPv4Stack=true`.

---

## Versión 1 · Chat replicado
Es la plantilla tal cual: [Nodo.java](../../plantillas/04-jgroups-java/README.md#nodojava).

| Comando | Hace |
|---|---|
| texto | Les llega a todos |
| `/quien` | Miembros y coordinador |
| `/historial` | Mensajes guardados |
| `/salir` | Sale |

**Prueba clave:** abre `ana`, escribe dos mensajes, **recién** abre `beto` → beto recibe el historial.

**Cómo defenderlo:** "El que llega pide el **estado** al coordinador con `getState`; así todas las réplicas quedan iguales."

**Si te piden `/privado nombre texto`** (fragmento orientativo **[EXTRA]**, no incluido en las pruebas):
```java
// dentro del ciclo que lee el teclado
if (linea.startsWith("/privado ")) {
    String[] p = linea.split(" ", 3);
    for (Address a : canal.getView().getMembers()) {
        if (a.toString().equals(p[1])) canal.send(new ObjectMessage(a, "(privado) " + p[2]));
    }
    continue;
}
```
> Si tu `receive` guarda todo en el historial, no guardes los mensajes que empiezan con `(privado)`.

---

## Versión 2 · Subasta sin servidor
Grupo `RemateSIS258`. Archivos: `Mensaje.java`, `Subasta.java`, `Remate.java`.

```bat
javac -cp C:\libs\jgroups\* -d out *.java
java -Djava.net.preferIPv4Stack=true -cp "out;C:\libs\jgroups\*" Remate ana
java -Djava.net.preferIPv4Stack=true -cp "out;C:\libs\jgroups\*" Remate beto
```

| Escribe | Pasa |
|---|---|
| `/crear laptop 100 60` | Subasta con base 100 que dura 60 segundos |
| `/pujar laptop 150` | Aceptada y la ven todos |
| `/pujar laptop 120` | Rechazada: debe superar 150.0 |
| `/subastas` | Lista |
| `/estado laptop` | Detalle e historial |
| `/ganadas` | Lo que ganaste y cuánto pagas |

**Prueba de caída:** crea la subasta, **cierra la primera ventana** (el coordinador) y espera: la otra la cierra a tiempo.

### Mensaje.java
```java
import java.io.Serializable;

// PROTOCOLO. CREAR y PUJAR son PROPUESTAS (van solo al coordinador).
// NUEVA, ACEPTADA y CIERRE son DECISIONES (van a todos). RECHAZADA vuelve al que pidio.
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    enum Tipo { CREAR, PUJAR, NUEVA, ACEPTADA, RECHAZADA, CIERRE }

    final Tipo tipo;
    final String articulo, texto;
    final double monto;
    final long dato;          // CREAR: segundos   NUEVA: instante de cierre

    Mensaje(Tipo tipo, String articulo, double monto, long dato, String texto) {
        this.tipo = tipo;
        this.articulo = articulo;
        this.monto = monto;
        this.dato = dato;
        this.texto = texto;
    }
}
```
### Subasta.java
```java
import java.io.Serializable;
import java.util.ArrayList;

public class Subasta implements Serializable {
    private static final long serialVersionUID = 1L;

    final String articulo;
    final long cierraEn;                // instante absoluto (sobrevive a la transferencia de estado)
    double mejor;
    String postor = "nadie";
    boolean abierta = true;
    final ArrayList<String> historial = new ArrayList<>();

    Subasta(String articulo, double base, long cierraEn) {
        this.articulo = articulo;
        this.mejor = base;
        this.cierraEn = cierraEn;
    }

    @Override
    public String toString() {
        long quedan = Math.max(0, (cierraEn - System.currentTimeMillis()) / 1000);
        return articulo + " | mejor " + mejor + " (" + postor + ") | "
                + (abierta ? "quedan " + quedan + " s" : "CERRADA");
    }
}
```
### Remate.java
```java
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
```

### Cómo defenderlo
- "No hay servidor: **todos** guardan las subastas."
- "Las pujas son **propuestas** que van solo al coordinador; él decide y difunde. Así dos pujas simultáneas se resuelven **en el mismo orden para todos**."
- "La hora de cierre es **absoluta**, por eso quien entra tarde o toma el mando sabe cuándo cerrar. (Supone relojes parecidos entre máquinas.)"
- "Si cae el coordinador, la **vista** cambia y el siguiente más antiguo asume sin perder datos."
- "Los mensajes son objetos con un **tipo** (enum), no texto suelto."

---

## Versión 3 · Invocación en grupo
### ContadorRPC.java
```java
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.RspList;

// Invocacion remota EN GRUPO: una llamada se ejecuta en TODOS los miembros.
// Comandos:  inc 5    ver    /salir
public class ContadorRPC implements Receiver {

    private final AtomicInteger contador = new AtomicInteger();

    // Metodos que se invocan a distancia (deben ser public)
    public int incrementar(int n) {
        int nuevo = contador.addAndGet(n);
        System.out.println("-> incrementar(" + n + "), ahora vale " + nuevo);
        return nuevo;
    }

    public int consultar() {
        return contador.get();
    }

    @Override
    public void viewAccepted(View vista) {
        System.out.println("** Miembros: " + vista.getMembers());
    }

    public static void main(String[] args) throws Exception {
        ContadorRPC yo = new ContadorRPC();
        JChannel canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(args.length > 0 ? args[0] : "nodo");

        RpcDispatcher despachador = new RpcDispatcher(canal, yo);
        despachador.setReceiver(yo);
        canal.connect("ContadorSIS258");

        RequestOptions todos = new RequestOptions(ResponseMode.GET_ALL, 5000);
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null && !linea.equals("/salir")) {
            MethodCall llamada;
            if (linea.startsWith("inc ")) {
                int n = Integer.parseInt(linea.substring(4).trim());
                llamada = new MethodCall("incrementar", new Object[]{n}, new Class<?>[]{int.class});
            } else if (linea.equals("ver")) {
                llamada = new MethodCall("consultar", new Object[0], new Class<?>[0]);
            } else {
                System.out.println("Use:  inc 5  |  ver  |  /salir");
                continue;
            }
            RspList<Object> respuestas = despachador.callRemoteMethods(null, llamada, todos);
            respuestas.forEach((miembro, rsp) -> System.out.println("  " + miembro + " => " + rsp.getValue()));
        }
        canal.close();
    }
}
```

Ejecuta dos nodos; escribe `inc 5` y `ver`: responden **todos**.

**Cómo defenderlo:** "`RpcDispatcher` es como RMI pero **en grupo**: una llamada se ejecuta en cada miembro y `RspList` junta las respuestas. Un nodo que entra después empieza en 0 porque aquí no implementé transferencia de estado."
