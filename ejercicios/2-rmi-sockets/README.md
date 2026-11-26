# Ejercicio 2 · RMI que por detrás usa sockets

[← Ejercicios](../README.md) · [Índice](../../README.md)

| Versión | Enunciado |
|---|---|
| [1](#versión-1--sistema-de-consultas-judiciales) | Juez → Justicia (RMI) → bancos (TCP y UDP) |
| [2](#versión-2--turismo-operadora-migración-banco-y-antifraude) | Turista → Operadora (RMI) → Migración (TCP), Banco (RMI) → Antifraude (UDP) |
| [3](#versión-3--me-cambian-el-tema) | Cómo adaptarlo a otro tema |

La idea que se repite: **el servidor RMI es a la vez cliente de otros** (intermediario).

---

## Versión 1 · Sistema de consultas judiciales
**[PRÁCTICA 2]**, con la forma de código del curso

```
 Juez ──RMI──> Justicia :1099 ──TCP "buscar:ci"──> Banco Mercantil :5002
                              └─UDP "buscar:ci"──> Banco BCP :6789
 Respuesta del banco:  cuenta1-saldo1:cuenta2-saldo2   (vacío si no tiene)
```

### Orden de arranque (cada uno en su ventana)
1. `ServidorMercantil`
2. `ServidorBCP`
3. `ServidorJusticia`
4. `Juez`

Todo puede ir en **un solo proyecto**.
```bat
cd ejercicios\2-rmi-sockets\v1-judicial
javac -d out *.java
java -cp out ServidorMercantil
```

### Resultado esperado
Dos cuentas (1515 con 5100 y 6576 con 6500), `Congelado: true` y la cuenta 1515 queda en 5000.

### Compartidos
#### Banco.java
```java
public enum Banco { MERCANTIL, BCP }
```
#### Cuenta.java
```java
import java.io.Serializable;

public class Cuenta implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Banco banco;
    private final String nrocuenta, ci, nombres, apellidos;
    private final double saldo;

    public Cuenta(Banco banco, String nrocuenta, String ci, String nombres, String apellidos, double saldo) {
        this.banco = banco;
        this.nrocuenta = nrocuenta;
        this.ci = ci;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.saldo = saldo;
    }

    public Banco getBanco() { return banco; }
    public String getNrocuenta() { return nrocuenta; }
    public double getSaldo() { return saldo; }

    @Override
    public String toString() {
        return banco + " | cuenta " + nrocuenta + " | " + nombres + " " + apellidos + " (CI " + ci + ") | saldo " + saldo;
    }
}
```
#### RespuestaCuenta.java
```java
import java.io.Serializable;
import java.util.ArrayList;

public class RespuestaCuenta implements Serializable {
    private static final long serialVersionUID = 1L;

    boolean error = false;                          // true si algo fallo
    String mensaje = "ok";
    ArrayList<Cuenta> cuentas = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("error=" + error + " mensaje=" + mensaje);
        for (Cuenta c : cuentas) sb.append("\n  ").append(c);
        return sb.toString();
    }
}
```
#### IJusticia.java
```java
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IJusticia extends Remote {
    RespuestaCuenta consultarCuentas(String ci, String nombres, String apellidos) throws RemoteException;
    boolean congelar(Cuenta cuenta, double monto) throws RemoteException;
}
```

### Justicia (el intermediario)
#### Justicia.java
```java
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
```
#### ServidorJusticia.java
```java
import java.rmi.registry.LocateRegistry;

public class ServidorJusticia {
    public static void main(String[] args) throws Exception {
        LocateRegistry.createRegistry(1099).rebind("Justicia", new Justicia());
        System.out.println("Servidor Justicia (RMI) listo en 1099");
    }
}
```
#### ClienteTCP.java
```java
import java.io.*;
import java.net.*;

// CLIENTE TCP. El metodo enviar() se puede copiar dentro de un servidor RMI u otro programa.
public class ClienteTCP {

    public static void main(String[] args) {
        String mensaje = args.length > 0 ? args[0] : "hola";
        System.out.println("Respuesta: " + enviar("localhost", 5002, mensaje));
    }

    static String enviar(String host, int puerto, String mensaje) {
        try (Socket client = new Socket(host, puerto);
             PrintStream toServer = new PrintStream(client.getOutputStream());
             BufferedReader fromServer = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            client.setSoTimeout(5000);          // no esperar para siempre
            toServer.println(mensaje);
            return fromServer.readLine();

        } catch (IOException e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
```
#### ClienteUDP.java
```java
import java.net.*;

// CLIENTE UDP. El metodo enviar() se puede copiar dentro de un servidor RMI u otro programa.
public class ClienteUDP {

    public static void main(String[] args) {
        String mensaje = args.length > 0 ? args[0] : "hola";
        System.out.println("Respuesta: " + enviar("localhost", 6789, mensaje));
    }

    static String enviar(String host, int puerto, String mensaje) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);   // UDP no avisa si el servidor esta apagado
            byte[] salida = mensaje.getBytes();
            socket.send(new DatagramPacket(salida, salida.length, InetAddress.getByName(host), puerto));

            byte[] bufer = new byte[1000];
            DatagramPacket respuesta = new DatagramPacket(bufer, bufer.length);
            socket.receive(respuesta);
            return new String(respuesta.getData(), 0, respuesta.getLength());

        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
```

### Bancos
#### CuentasBanco.java
```java
import java.util.*;

// Logica comun de un banco (la usan el servidor TCP y el UDP).
public class CuentasBanco {
    private final Map<String, Double> saldos = new LinkedHashMap<>();   // cuenta -> saldo
    private final Map<String, String> titular = new HashMap<>();        // cuenta -> ci

    void agregar(String cuenta, String ci, double saldo) {
        saldos.put(cuenta, saldo);
        titular.put(cuenta, ci);
    }

    // buscar:ci               ->  cuenta1-saldo1:cuenta2-saldo2   (vacio si no hay)
    // congelar:cuenta:monto   ->  ok | error:motivo
    synchronized String procesar(String mensaje) {
        try {
            String[] p = mensaje.split(":");
            if (p[0].equals("buscar")) {
                List<String> lista = new ArrayList<>();
                saldos.forEach((cuenta, saldo) -> {
                    if (titular.get(cuenta).equals(p[1])) lista.add(cuenta + "-" + saldo);
                });
                return String.join(":", lista);
            }
            if (p[0].equals("congelar")) {
                double monto = Double.parseDouble(p[2]);
                Double saldo = saldos.get(p[1]);
                if (saldo == null) return "error:cuenta no existe";
                if (monto <= 0 || monto > saldo) return "error:monto invalido";
                saldos.put(p[1], saldo - monto);    // el dinero congelado ya no esta disponible
                return "ok";
            }
            return "error:operacion desconocida";
        } catch (Exception e) {
            return "error:formato";
        }
    }
}
```
#### ServidorMercantil.java
```java
import java.io.*;
import java.net.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorMercantil {

    static final int PUERTO = 5002;   // cambiar si el enunciado pide otro

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servidor TCP escuchando en el puerto " + PUERTO);
            while (true) {
                Socket client = server.accept();            // se queda esperando un cliente
                new Thread(() -> atender(client)).start();  // un hilo por cliente
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    static void atender(Socket client) {
        try (client;
             BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream toClient = new PrintStream(client.getOutputStream())) {

            String recibido = fromClient.readLine();
            System.out.println("Llego: " + recibido);
            String respuesta = procesarSolicitud(recibido);
            toClient.println(respuesta);
            System.out.println("Respondi: " + respuesta);

        } catch (Exception e) {
            System.out.println("Error con un cliente: " + e.getMessage());
        }
    }

    static final CuentasBanco BANCO = new CuentasBanco();
    static { BANCO.agregar("1515", "11021654", 5100); }     // datos del enunciado

    static String procesarSolicitud(String mensaje) {
        return BANCO.procesar(mensaje);
    }
}
```
#### ServidorBCP.java
```java
import java.net.*;

// SERVIDOR UDP: recibe un datagrama y responde a quien lo mando.
public class ServidorBCP {

    static final int PUERTO = 6789;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            System.out.println("Servidor UDP escuchando en el puerto " + PUERTO);
            byte[] bufer = new byte[1000];
            while (true) {
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socket.receive(peticion);   // se queda esperando

                // IMPORTANTE: usar getLength(), si no llega basura del bufer
                String recibido = new String(peticion.getData(), 0, peticion.getLength());
                System.out.println("Llego: " + recibido);

                byte[] salida = procesarSolicitud(recibido).getBytes();
                socket.send(new DatagramPacket(salida, salida.length,
                        peticion.getAddress(), peticion.getPort()));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static final CuentasBanco BANCO = new CuentasBanco();
    static { BANCO.agregar("6576", "11021654", 6500); }     // datos del enunciado

    static String procesarSolicitud(String mensaje) {
        return BANCO.procesar(mensaje);
    }
}
```

### Cliente
#### Juez.java
```java
import java.rmi.registry.LocateRegistry;

public class Juez {
    public static void main(String[] args) throws Exception {
        IJusticia justicia = (IJusticia) LocateRegistry.getRegistry("localhost", 1099).lookup("Justicia");

        RespuestaCuenta r = justicia.consultarCuentas("11021654", "Juan Perez", "Segovia");
        System.out.println(r);

        // Ordena congelar 100 en la primera cuenta encontrada y vuelve a consultar
        if (!r.cuentas.isEmpty()) {
            boolean ok = justicia.congelar(r.cuentas.get(0), 100);
            System.out.println("Congelado: " + ok);
            System.out.println(justicia.consultarCuentas("11021654", "Juan Perez", "Segovia"));
        }
    }
}
```

### Qué hace
El juez pide las cuentas de una persona. Justicia pregunta a cada banco con **su propio protocolo de transporte**, une las respuestas y devuelve un objeto. También puede congelar un monto.

### Cómo defenderlo
- "El juez **solo conoce la interfaz** `IJusticia`; no sabe que hay sockets detrás: eso es **transparencia**."
- "Justicia es un **middleware**: traduce una llamada RMI a mensajes TCP y UDP."
- "`Cuenta` y `RespuestaCuenta` son `Serializable` porque viajan por RMI."
- "Si un banco no responde, devuelvo `error=true` con el mensaje en vez de caerme: **tolerancia a fallos**."
- "El banco usa `synchronized` para que dos congelamientos no se pisen."

---

## Versión 2 · Turismo: Operadora, Migración, Banco y Antifraude
**[INGE]** (basado en la resolución de examen del repositorio)

```
 Turista ─RMI→ Operadora :1099 ─TCP "pasaporte:123"→ Migración :5002
                              └─RMI→ Banco :1100 ─UDP "monto"→ Antifraude :6789
```

### Orden de arranque
1. `ServidorMigracion`  2. `ServidorAntifraude`  3. `ServidorBanco`  4. `ServidorOperadora`  5. `Turista`

### Datos de prueba
| Pasaporte | Tour | Personas | Resultado |
|---|---|---|---|
| 123 | T1 | 2 | Aprobado, total 600, riesgo bajo |
| 123 | T2 | 2 | Aprobado, total 1600, riesgo alto |
| 456 | T1 | 1 | Rechazado: saldo insuficiente |
| 999 | T1 | 1 | Rechazado por Migración |

### Interfaces y objetos
#### IOperadora.java
```java
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IOperadora extends Remote {
    Voucher comprarTour(String pasaporte, String codigoTour, int personas) throws RemoteException;
}
```
#### Voucher.java
```java
import java.io.Serializable;

public class Voucher implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean ok;
    private final String mensaje;
    private final double totalUSD;

    public Voucher(boolean ok, String mensaje, double totalUSD) {
        this.ok = ok;
        this.mensaje = mensaje;
        this.totalUSD = totalUSD;
    }

    @Override
    public String toString() {
        return "Voucher{ok=" + ok + ", mensaje=" + mensaje + ", totalUSD=" + totalUSD + "}";
    }
}
```
#### IBanco.java
```java
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBanco extends Remote {
    Pago debitar(String pasaporte, double montoUSD) throws RemoteException;
}
```
#### Pago.java
```java
import java.io.Serializable;

public class Pago implements Serializable {
    private static final long serialVersionUID = 1L;

    final boolean aprobado;
    final String detalle;

    public Pago(boolean aprobado, String detalle) {
        this.aprobado = aprobado;
        this.detalle = detalle;
    }
}
```

### Operadora
#### Operadora.java
```java
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

// ORQUESTADOR: 1) Migracion (TCP)  2) Banco (RMI, que consulta antifraude UDP)  3) voucher
public class Operadora extends UnicastRemoteObject implements IOperadora {
    private static final long serialVersionUID = 1L;

    private static final Map<String, Double> PRECIOS = Map.of("T1", 300.0, "T2", 800.0);

    public Operadora() throws RemoteException {
        super();
    }

    @Override
    public Voucher comprarTour(String pasaporte, String codigoTour, int personas) throws RemoteException {
        Double precio = PRECIOS.get(codigoTour);
        if (precio == null || personas <= 0) return new Voucher(false, "tour o cantidad invalida", 0);
        double total = precio * personas;

        String migracion = ClienteTCP.enviar("localhost", 5002, "pasaporte:" + pasaporte);
        if (migracion == null || !migracion.startsWith("valido")) {
            return new Voucher(false, "Migracion: " + migracion, total);
        }

        try {
            IBanco banco = (IBanco) LocateRegistry.getRegistry("localhost", 1100).lookup("Banco");
            Pago pago = banco.debitar(pasaporte, total);
            return new Voucher(pago.aprobado, pago.detalle, total);
        } catch (Exception e) {
            return new Voucher(false, "Banco no disponible", total);
        }
    }
}
```
#### ServidorOperadora.java
```java
import java.rmi.registry.LocateRegistry;

public class ServidorOperadora {
    public static void main(String[] args) throws Exception {
        LocateRegistry.createRegistry(1099).rebind("Operadora", new Operadora());
        System.out.println("Operadora (RMI) lista en 1099");
    }
}
```

### Banco
#### BancoRMI.java
```java
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
```
#### ServidorBanco.java
```java
import java.rmi.registry.LocateRegistry;

public class ServidorBanco {
    public static void main(String[] args) throws Exception {
        // Puerto 1100: el 1099 ya lo ocupa la Operadora
        LocateRegistry.createRegistry(1100).rebind("Banco", new BancoRMI());
        System.out.println("Banco (RMI) listo en 1100");
    }
}
```

### Sockets
#### ServidorMigracion.java
```java
import java.io.*;
import java.net.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorMigracion {

    static final int PUERTO = 5002;   // cambiar si el enunciado pide otro

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("Servidor TCP escuchando en el puerto " + PUERTO);
            while (true) {
                Socket client = server.accept();            // se queda esperando un cliente
                new Thread(() -> atender(client)).start();  // un hilo por cliente
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    static void atender(Socket client) {
        try (client;
             BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream toClient = new PrintStream(client.getOutputStream())) {

            String recibido = fromClient.readLine();
            System.out.println("Llego: " + recibido);
            String respuesta = procesarSolicitud(recibido);
            toClient.println(respuesta);
            System.out.println("Respondi: " + respuesta);

        } catch (Exception e) {
            System.out.println("Error con un cliente: " + e.getMessage());
        }
    }

    // Pasaportes (o CI) registrados:  numero -> pais
    static final java.util.Map<String, String> REGISTRO = java.util.Map.of(
            "123", "Bolivia",
            "456", "Peru",
            "11021654", "Bolivia");

    // Protocolo:  pasaporte:123   ->  valido:Bolivia   |   invalido:motivo
    static String procesarSolicitud(String mensaje) {
        if (mensaje == null) return "invalido:mensaje vacio";

        String[] partes = mensaje.split(":");
        if (partes.length != 2) return "invalido:formato";

        String numero = partes[1].trim();
        String pais = REGISTRO.get(numero);
        return pais != null ? "valido:" + pais : "invalido:no registrado";
    }
}
```
#### ServidorAntifraude.java
```java
import java.net.*;

// SERVIDOR UDP: recibe un datagrama y responde a quien lo mando.
public class ServidorAntifraude {

    static final int PUERTO = 6789;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            System.out.println("Servidor UDP escuchando en el puerto " + PUERTO);
            byte[] bufer = new byte[1000];
            while (true) {
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socket.receive(peticion);   // se queda esperando

                // IMPORTANTE: usar getLength(), si no llega basura del bufer
                String recibido = new String(peticion.getData(), 0, peticion.getLength());
                System.out.println("Llego: " + recibido);

                byte[] salida = procesarSolicitud(recibido).getBytes();
                socket.send(new DatagramPacket(salida, salida.length,
                        peticion.getAddress(), peticion.getPort()));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Protocolo:  1500  (monto)  ->  alto | bajo
    //             123:1500       ->  tambien vale, toma el ultimo dato
    static String procesarSolicitud(String mensaje) {
        try {
            String[] partes = mensaje.trim().split(":");
            double monto = Double.parseDouble(partes[partes.length - 1]);
            return monto > 1000 ? "alto" : "bajo";     // CAMBIAR el limite
        } catch (NumberFormatException e) {
            return "error:monto invalido";
        }
    }
}
```
#### ClienteTCP.java
```java
import java.io.*;
import java.net.*;

// CLIENTE TCP. El metodo enviar() se puede copiar dentro de un servidor RMI u otro programa.
public class ClienteTCP {

    public static void main(String[] args) {
        String mensaje = args.length > 0 ? args[0] : "hola";
        System.out.println("Respuesta: " + enviar("localhost", 5002, mensaje));
    }

    static String enviar(String host, int puerto, String mensaje) {
        try (Socket client = new Socket(host, puerto);
             PrintStream toServer = new PrintStream(client.getOutputStream());
             BufferedReader fromServer = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            client.setSoTimeout(5000);          // no esperar para siempre
            toServer.println(mensaje);
            return fromServer.readLine();

        } catch (IOException e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
```
#### ClienteUDP.java
```java
import java.net.*;

// CLIENTE UDP. El metodo enviar() se puede copiar dentro de un servidor RMI u otro programa.
public class ClienteUDP {

    public static void main(String[] args) {
        String mensaje = args.length > 0 ? args[0] : "hola";
        System.out.println("Respuesta: " + enviar("localhost", 6789, mensaje));
    }

    static String enviar(String host, int puerto, String mensaje) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);   // UDP no avisa si el servidor esta apagado
            byte[] salida = mensaje.getBytes();
            socket.send(new DatagramPacket(salida, salida.length, InetAddress.getByName(host), puerto));

            byte[] bufer = new byte[1000];
            DatagramPacket respuesta = new DatagramPacket(bufer, bufer.length);
            socket.receive(respuesta);
            return new String(respuesta.getData(), 0, respuesta.getLength());

        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
```

### Cliente
#### Turista.java
```java
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Turista {
    public static void main(String[] args) throws Exception {
        IOperadora operadora = (IOperadora) LocateRegistry.getRegistry("localhost", 1099).lookup("Operadora");

        Scanner sc = new Scanner(System.in);
        System.out.print("Pasaporte: ");
        String pasaporte = sc.nextLine();
        System.out.print("Codigo de tour (T1, T2): ");
        String tour = sc.nextLine();
        System.out.print("Personas: ");
        int personas = Integer.parseInt(sc.nextLine().trim());

        System.out.println(operadora.comprarTour(pasaporte, tour, personas));
    }
}
```

### Si te piden: 50 % de descuento a bolivianos
En `Operadora.java`, justo **después** del `if` de Migración:
```java
if (migracion.endsWith("Bolivia")) total = total * 0.5;
```

### Qué hace
Una compra pasa por tres controles en orden: documento válido, dinero suficiente y riesgo. Si uno falla, se corta y el voucher explica por qué.

### Cómo defenderlo
- "La Operadora **orquesta**: decide el orden y qué hacer si algo falla."
- "Uso **dos registros RMI en puertos distintos** (1099 y 1100) porque están en la misma PC."
- "El débito es `synchronized`: revisar saldo y descontar no se puede interrumpir."
- "Primero valido y **al final** cobro, así no hay que devolver dinero."

---

## Versión 3 · Me cambian el tema

| En el ejemplo | Tu examen puede decir |
|---|---|
| Justicia / Operadora | Seguro, universidad, farmacia, aduana |
| Banco TCP | Cualquier "sistema existente" que habla texto |
| Banco UDP / Antifraude | Sensor, alerta, cotización |
| `buscar:ci` | `consultar:matricula`, `stock:codigo` |

Pasos: (1) cambia la **interfaz** RMI, (2) cambia los **mensajes** de los sockets en los dos lados, (3) cambia los datos de ejemplo.

Si el enunciado da **su propio formato de mensaje**, respétalo letra por letra.
