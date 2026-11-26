# 04 · JGroups

[← Plantillas](../README.md) · [Índice](../../README.md) · **[PRÁCTICA 3]**

**Qué es:** varios nodos iguales en un **grupo**. Lo que uno envía les llega a todos. No hay servidor.

| Archivo | Qué hace |
|---|---|
| `Nodo.java` | Se une al grupo, envía, recibe, y le pasa el historial al que llega tarde |

## Pasos
1. Antes (con internet): descargar el jar a `C:\libs\jgroups\`.
2. Proyecto Java con `Nodo.java` y [agrega el jar](../../00-preparacion/README.md#d5-agregar-el-jar-de-jgroups).
3. Abre **dos o tres terminales** y ejecuta en cada una, con **unos segundos de diferencia**:

```bat
javac -cp C:\libs\jgroups\* -d out Nodo.java
java -Djava.net.preferIPv4Stack=true -cp "out;C:\libs\jgroups\*" Nodo ana
java -Djava.net.preferIPv4Stack=true -cp "out;C:\libs\jgroups\*" Nodo beto
```

(En NetBeans: *Properties → Run → Arguments* para el nombre.)

> Al arrancar salen muchas líneas `INFO` y `WARN` de JGroups: **es normal**. Lo tuyo son las líneas con `**` y los mensajes.

## Nodo.java
```java
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jgroups.*;
import org.jgroups.util.Util;

// NODO de un grupo JGroups. Todos los nodos son iguales: no hay servidor.
public class Nodo implements Receiver {

    private static final String GRUPO = "GrupoExamen";   // todos deben usar el mismo nombre

    private JChannel canal;
    private final List<String> estado = new ArrayList<>();   // copia que tiene CADA nodo

    // Se ejecuta cuando alguien entra o sale del grupo
    @Override
    public void viewAccepted(View vista) {
        System.out.println("** Miembros: " + vista.getMembers() + " | coordinador: " + vista.getCoord());
    }

    // Se ejecuta cuando llega un mensaje
    @Override
    public void receive(Message msg) {
        String texto = msg.getObject();
        // AQUI LOGICA  (que hacer con lo que llega)
        synchronized (estado) {
            estado.add(msg.getSrc() + ": " + texto);
        }
        System.out.println(msg.getSrc() + "> " + texto);
    }

    // Lo ejecuta el nodo que YA tiene los datos, para dárselos al nuevo
    @Override
    public void getState(OutputStream salida) throws Exception {
        synchronized (estado) {
            Util.objectToStream(estado, new DataOutputStream(salida));
        }
    }

    // Lo ejecuta el nodo que ACABA de entrar
    @Override
    public void setState(InputStream entrada) throws Exception {
        List<String> recibido = Util.objectFromStream(new DataInputStream(entrada));
        synchronized (estado) {
            estado.clear();
            estado.addAll(recibido);
        }
        System.out.println("** Estado recibido: " + recibido);
    }

    boolean soyCoordinador() {
        return canal.getView().getCoord().equals(canal.getAddress());
    }

    void iniciar(String nombre) throws Exception {
        canal = new JChannel(System.getProperty("config", "udp.xml"));
        canal.name(nombre);
        canal.setReceiver(this);
        canal.connect(GRUPO);
        canal.getState(null, 10000);   // pide los datos al coordinador

        System.out.println("Escriba mensajes.  /quien  /historial  /salir");
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        String linea;
        while ((linea = teclado.readLine()) != null) {
            if (linea.equals("/salir")) break;
            else if (linea.equals("/quien")) {
                System.out.println(canal.getView().getMembers() + " | soy coordinador: " + soyCoordinador());
            } else if (linea.equals("/historial")) {
                synchronized (estado) { estado.forEach(System.out::println); }
            } else {
                // AQUI LOGICA  (comandos propios del ejercicio)
                canal.send(new ObjectMessage(null, linea));   // null = a TODO el grupo
            }
        }
        canal.close();
    }

    public static void main(String[] args) throws Exception {
        String nombre = args.length > 0 ? args[0] : "nodo" + (int) (Math.random() * 100);
        new Nodo().iniciar(nombre);
    }
}
```

## Si cada nodo se ve solo (red de la facultad o Wi-Fi)
El multicast está bloqueado. Usa TCP (todo en una línea):

```bat
java -Djava.net.preferIPv4Stack=true -Dconfig=tcp.xml -Djgroups.bind_addr=192.168.1.25 -Djgroups.tcpping.initial_hosts=192.168.1.25[7800],192.168.1.30[7800] -cp "out;C:\libs\jgroups\*" Nodo ana
```
En una sola PC usa `127.0.0.1` y `initial_hosts=127.0.0.1[7800],127.0.0.1[7801]`.

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Mensaje privado | `canal.send(new ObjectMessage(direccion, texto))` → [ejercicio 7](../../ejercicios/7-jgroups/README.md) |
| Subasta / que alguien decida | El **coordinador** decide → [ejercicio 7, versión 2](../../ejercicios/7-jgroups/README.md#versión-2--subasta-sin-servidor) |
| Ejecutar un método en todos | `RpcDispatcher` → [versión 3](../../ejercicios/7-jgroups/README.md#versión-3--invocación-en-grupo) |
| Enviar objetos | Que la clase sea `Serializable` |

## Cómo defenderlo
- "Es **comunicación indirecta**: el emisor no conoce a los receptores, solo el nombre del grupo."
- "`viewAccepted` avisa cada vez que alguien entra, sale o se cae; el **coordinador** es el más antiguo."
- "`getState` lo ejecuta quien ya tiene los datos y `setState` el recién llegado: así la réplica queda igual."
- "`receive` lo llama un hilo de JGroups, por eso uso `synchronized`."

## Errores típicos
- Dos nodos y cada uno es coordinador: espera unos segundos entre arranques, o usa `tcp.xml`.
- Al cerrar un nodo puede salir `IOException: Stream closed` en los otros: es solo aviso.
