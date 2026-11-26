# Ejercicio 1 · Sockets

[← Ejercicios](../README.md) · [Índice](../../README.md)

| Versión | Me piden… |
|---|---|
| [1](#versión-1--validar-un-documento) | Validar un pasaporte/CI |
| [2](#versión-2--calculadora-con-protocolo) | Operaciones `operacion:a:b` |
| [3](#versión-3--banco-consultar-debitar-abonar) | Banco: consultar, debitar, abonar |
| [4](#versión-4--conversación-el-servidor-pregunta) | El servidor pregunta varias veces |
| [5](#versión-5--chat-multicliente) | Chat entre varios clientes |
| [6](#versión-6--udp-antifraude) | Lo mismo con UDP |
| [7](#versión-7--servidor-web-con-sockets) | Servidor web |

```
 Cliente  ----- "pasaporte:123" ----->  Servidor :5002
          <---- "valido:Bolivia" -----
```

**Cómo ejecutar cualquier versión:** pon los `.java` de la carpeta de la versión en un proyecto, ejecuta **primero el servidor**, después el cliente ([cómo crear el proyecto](../../00-preparacion/README.md#d4-proyecto-java)).

En las versiones 1 a 3 el cliente es el de la [plantilla 01](../../plantillas/01-socket-tcp-java/README.md#clientetcpjava): cambia `"hola"` por tu mensaje, o pásalo como argumento:

```bat
java -cp out ClienteTCP pasaporte:123
```

---

## Versión 1 · Validar un documento
**[INGE]** (idea del examen resuelto en el repositorio: Migración)

| Envío | Respuesta |
|---|---|
| `pasaporte:123` | `valido:Bolivia` |
| `pasaporte:999` | `invalido:no registrado` |
| `123` | `invalido:formato` |

### ServidorTCP.java
```java
import java.io.*;
import java.net.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorTCP {

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

**Qué hace:** parte el mensaje por `:` y busca el número en un mapa.

**Cómo defenderlo:** "Definí un protocolo de texto `tipo:numero`. El `Map` simula la base de datos. Siempre respondo algo, aun si el mensaje está mal, para que el cliente no se quede esperando."

**Si te piden:** *más datos* → guarda `"Bolivia;Juan"` como valor. *Leer de archivo* → [GuardarEnArchivo](../../logica/README.md#java-guardar-en-archivo).

---

## Versión 2 · Calculadora con protocolo
**[INGE]** (carpeta `tcp protocolos`)

| Envío | Respuesta |
|---|---|
| `sumar:3:4` | `ok:7.0` |
| `dividir:5:0` | `error:division por cero` |
| `potencia:2:3` | `error:operacion desconocida` |

### ServidorTCP.java
```java
import java.io.*;
import java.net.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorTCP {

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

    // Protocolo:  sumar:3:4  ->  ok:7.0     |   error:motivo
    static String procesarSolicitud(String mensaje) {
        try {
            String[] p = mensaje.split(":");
            double a = Double.parseDouble(p[1]);
            double b = Double.parseDouble(p[2]);

            return switch (p[0].toLowerCase()) {
                case "sumar"       -> "ok:" + (a + b);
                case "restar"      -> "ok:" + (a - b);
                case "multiplicar" -> "ok:" + (a * b);
                case "dividir"     -> b == 0 ? "error:division por cero" : "ok:" + (a / b);
                default            -> "error:operacion desconocida";
            };
        } catch (Exception e) {
            return "error:use operacion:numero:numero";
        }
    }
}
```

**Qué hace:** la primera parte dice la operación; `switch` elige.

**Cómo defenderlo:** "El protocolo es `operacion:a:b`. Valido división por cero y los errores de formato con `try/catch`; el servidor nunca se cae por un mensaje malo."

**Si te piden:** *otra operación* → agrega un `case`. Ejemplo: `case "potencia" -> "ok:" + Math.pow(a, b);`

---

## Versión 3 · Banco: consultar, debitar, abonar
**[EXTRA]** sobre el estilo del curso

| Envío | Respuesta |
|---|---|
| `consultar:11021654` | `1515-5100.0:6576-6500.0` (el orden puede variar) |
| `debitar:1515:100` | `ok:5000.0` |
| `debitar:1515:999999` | `error:saldo insuficiente` |
| `abonar:1515:50` | `ok:` y el saldo nuevo |

### ServidorTCP.java
```java
import java.io.*;
import java.net.*;

// SERVIDOR TCP multihilo. Recibe UNA linea y responde UNA linea.
public class ServidorTCP {

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

    // cuenta -> saldo       y       cuenta -> ci del titular
    static final java.util.Map<String, Double> SALDOS = new java.util.LinkedHashMap<>(
            java.util.Map.of("1515", 5100.0, "6576", 6500.0));
    static final java.util.Map<String, String> TITULAR = java.util.Map.of(
            "1515", "11021654", "6576", "11021654");

    // consultar:ci            ->  1515-5100.0:6576-6500.0
    // debitar:cuenta:monto    ->  ok:saldoNuevo
    // abonar:cuenta:monto     ->  ok:saldoNuevo
    // synchronized: dos clientes no pueden mover el saldo a la vez
    static synchronized String procesarSolicitud(String mensaje) {
        try {
            String[] p = mensaje.split(":");
            if (p[0].equals("consultar")) {
                java.util.List<String> cuentas = new java.util.ArrayList<>();
                SALDOS.forEach((cuenta, saldo) -> {
                    if (TITULAR.get(cuenta).equals(p[1])) cuentas.add(cuenta + "-" + saldo);
                });
                return cuentas.isEmpty() ? "error:sin cuentas" : String.join(":", cuentas);
            }

            String cuenta = p[1];
            double monto = Double.parseDouble(p[2]);
            if (!SALDOS.containsKey(cuenta)) return "error:cuenta no existe";
            if (monto <= 0) return "error:monto invalido";

            if (p[0].equals("debitar")) {
                if (SALDOS.get(cuenta) < monto) return "error:saldo insuficiente";
                SALDOS.put(cuenta, SALDOS.get(cuenta) - monto);
            } else if (p[0].equals("abonar")) {
                SALDOS.put(cuenta, SALDOS.get(cuenta) + monto);
            } else {
                return "error:operacion desconocida";
            }
            return "ok:" + SALDOS.get(cuenta);
        } catch (Exception e) {
            return "error:formato";
        }
    }
}
```

**Qué hace:** guarda saldos en memoria y los modifica según el mensaje.

**Cómo defenderlo:** "El método es `synchronized` porque hay un hilo por cliente: sin eso, dos débitos simultáneos podrían leer el mismo saldo y perderse uno (condición de carrera)."

---

## Versión 4 · Conversación (el servidor pregunta)
**[INGE]** (protocolo interactivo del repositorio)

Ejecuta `ServidorTCP` y luego `ClienteTCP` **de esta carpeta** (no el de la plantilla). Contesta por teclado.

### ServidorTCP.java
```java
import java.io.*;
import java.net.*;

// El servidor PREGUNTA y el cliente responde, varias veces, en la MISMA conexion.
public class ServidorTCP {

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(5002)) {
            System.out.println("Servidor en el puerto 5002");
            while (true) {
                Socket client = server.accept();
                new Thread(() -> atender(client)).start();
            }
        }
    }

    static void atender(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream out = new PrintStream(client.getOutputStream())) {

            String linea;
            do {
                out.println("Primer numero:");
                double a = Double.parseDouble(in.readLine());
                out.println("Segundo numero:");
                double b = Double.parseDouble(in.readLine());
                out.println("Operacion (+ - * /):");
                String op = in.readLine();

                double r = switch (op) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    case "/" -> a / b;
                    default  -> Double.NaN;
                };
                out.println("Resultado " + r + ". Otra vez? (s/n)");
                linea = in.readLine();
            } while ("s".equalsIgnoreCase(linea));

            out.println("FIN");

        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        }
    }
}
```

### ClienteTCP.java
```java
import java.io.*;
import java.net.*;

// Lee lo que dice el servidor, contesta por teclado, hasta recibir FIN.
public class ClienteTCP {

    public static void main(String[] args) throws IOException {
        try (Socket client = new Socket("localhost", 5002);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream out = new PrintStream(client.getOutputStream());
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            String delServidor;
            while ((delServidor = in.readLine()) != null && !delServidor.equals("FIN")) {
                System.out.println(delServidor);
                String respuesta = teclado.readLine();
                if (respuesta == null) break;
                out.println(respuesta);
            }
            System.out.println("Conversacion terminada");
        }
    }
}
```

**Qué hace:** varias preguntas y respuestas en **la misma conexión**; el servidor manda `FIN` para terminar.

**Cómo defenderlo:** "La conexión queda abierta y el protocolo tiene estado: el servidor sabe en qué paso va porque el código es secuencial dentro del hilo de ese cliente. `FIN` es la señal acordada para cerrar."

---

## Versión 5 · Chat multicliente
**[PRÁCTICA 1]**

Ejecuta `ServidorChat` y **dos o más** `ClienteChat`. Cada cliente escribe primero su nombre.

| Comando | Hace |
|---|---|
| `hola` | Les llega a los demás |
| `/quien` | Lista conectados |
| `/privado ana secreto` | Solo a ana |
| `/salir` | Sale |

### ServidorChat.java
```java
import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;

// CHAT: lo que escribe uno les llega a todos.
// Comandos: /quien  /privado nombre texto  /salir
public class ServidorChat {

    static final Set<Cliente> CLIENTES = new CopyOnWriteArraySet<>();   // seguro entre hilos
    static final ExecutorService HILOS = Executors.newFixedThreadPool(20);

    record Cliente(String nombre, PrintWriter out) {}

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(5002)) {
            System.out.println("Chat en el puerto 5002");
            while (true) {
                Socket socket = server.accept();
                HILOS.execute(() -> atender(socket));
            }
        }
    }

    static void atender(Socket socket) {
        Cliente yo = null;
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("Tu nombre:");
            String nombre = in.readLine();
            if (nombre == null) return;
            yo = new Cliente(nombre.trim(), out);
            CLIENTES.add(yo);
            difundir("* " + yo.nombre() + " entro al chat", yo);

            String linea;
            while ((linea = in.readLine()) != null && !linea.equals("/salir")) {
                if (linea.equals("/quien")) {
                    CLIENTES.forEach(c -> out.println("- " + c.nombre()));
                } else if (linea.startsWith("/privado ")) {
                    privado(yo, linea);
                } else {
                    difundir(yo.nombre() + ": " + linea, yo);
                }
            }
        } catch (IOException e) {
            System.out.println("Un cliente se cayo: " + e.getMessage());
        } finally {
            if (yo != null) {
                CLIENTES.remove(yo);
                difundir("* " + yo.nombre() + " salio", yo);
            }
        }
    }

    static void difundir(String texto, Cliente menos) {
        System.out.println(texto);
        for (Cliente c : CLIENTES) {
            if (c != menos) c.out().println(texto);
        }
    }

    static void privado(Cliente yo, String linea) {
        String[] p = linea.split(" ", 3);   // /privado ana hola que tal
        if (p.length < 3) { yo.out().println("Uso: /privado nombre texto"); return; }
        for (Cliente c : CLIENTES) {
            if (c.nombre().equalsIgnoreCase(p[1])) {
                c.out().println("(privado) " + yo.nombre() + ": " + p[2]);
                return;
            }
        }
        yo.out().println("No existe " + p[1]);
    }
}
```

### ClienteChat.java
```java
import java.io.*;
import java.net.*;

// Necesita DOS hilos: uno escucha al servidor y otro lee el teclado.
public class ClienteChat {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 5002);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Thread receptor = new Thread(() -> {
            try {
                String linea;
                while ((linea = in.readLine()) != null) System.out.println(linea);
            } catch (IOException e) {
                System.out.println("Conexion cerrada");
            }
        });
        receptor.setDaemon(true);
        receptor.start();

        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null) {
            out.println(linea);
            if (linea.equals("/salir")) break;
        }
        socket.close();
    }
}
```

**Qué hace:** el servidor guarda la salida de cada cliente y reenvía lo que llega.

**Cómo defenderlo:** "Uso un **pool de hilos** para no crear hilos sin límite y un conjunto **seguro para concurrencia** porque varios hilos lo leen y modifican. El cliente necesita dos hilos: uno recibe y otro lee el teclado; si no, no vería mensajes mientras escribe."

### Si te piden salas (`/entrar sala`)
```java
// guardar la sala de cada cliente
static final java.util.Map<PrintWriter, String> SALAS = new java.util.concurrent.ConcurrentHashMap<>();
// al recibir  "/entrar redes":
SALAS.put(out, linea.substring(8).trim());
// en difundir, enviar solo a los de la misma sala:
if (java.util.Objects.equals(SALAS.get(c.out()), SALAS.get(menos.out()))) c.out().println(texto);
```
*(Fragmento orientativo **[EXTRA]**, no incluido en las pruebas.)*

---

## Versión 6 · UDP antifraude
**[INGE]** (examen resuelto del repositorio)

Cliente: el de la [plantilla 02](../../plantillas/02-socket-udp-java/README.md#clienteudpjava), enviando `1500`.

| Envío | Respuesta |
|---|---|
| `1500` | `alto` |
| `500` | `bajo` |

### ServidorUDP.java
```java
import java.net.*;

// SERVIDOR UDP: recibe un datagrama y responde a quien lo mando.
public class ServidorUDP {

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

**Cómo defenderlo:** "Es una consulta corta de ida y vuelta, por eso UDP es suficiente. Si se pierde, el cliente lo nota por el timeout y puede reintentar."

---

## Versión 7 · Servidor web con sockets
**[INGE]** (`ServidorWeb`)

Ejecuta y abre en el navegador `http://localhost:8090/?nombre=Ana`.

### ServidorWeb.java
```java
import java.io.*;
import java.net.*;

// Un servidor web hecho solo con sockets: el navegador es el cliente.
public class ServidorWeb {

    public static void main(String[] args) throws IOException {
        int puerto = 8090;     // en el repo del curso usa el 80
        try (ServerSocket server = new ServerSocket(puerto)) {
            System.out.println("Abrir http://localhost:" + puerto + "/?nombre=Ana");
            while (true) {
                Socket client = server.accept();
                new Thread(() -> atender(client)).start();
            }
        }
    }

    static void atender(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintStream out = new PrintStream(client.getOutputStream())) {

            String peticion = in.readLine();          // GET /?nombre=Ana HTTP/1.1
            if (peticion == null) return;
            System.out.println(peticion);
            String linea;
            while ((linea = in.readLine()) != null && !linea.isEmpty()) { } // saltar cabeceras

            String ruta = peticion.split(" ")[1];
            String nombre = ruta.contains("nombre=") ? ruta.substring(ruta.indexOf("nombre=") + 7) : "visitante";
            String html = "<html><body><h1>Hola " + nombre.replaceAll("[<>]", "") + "</h1></body></html>";

            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Content-Type: text/html; charset=utf-8\r\n");
            out.print("Content-Length: " + html.getBytes("UTF-8").length + "\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");                       // linea vacia = fin de cabeceras
            out.write(html.getBytes("UTF-8"));
            out.flush();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
```

**Qué hace:** lee la petición HTTP como texto y responde HTML.

**Cómo defenderlo:** "HTTP es un protocolo de texto sobre TCP: primera línea, cabeceras, una **línea vacía** y el cuerpo. El navegador hace de cliente."

> El repositorio usa el puerto **80**; en Windows suele estar ocupado, por eso aquí 8090.
