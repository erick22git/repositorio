# 01 · Socket TCP en Java

[← Plantillas](../README.md) · [Índice](../../README.md) · Estilo **[INGE]** (`procesarSolicitud`, puerto 5002) y **[PRÁCTICA]** (hilos)

**Qué es:** un servidor que espera en el puerto 5002, recibe **una línea** y responde **una línea**. Atiende varios clientes a la vez.

| Archivo | Qué hace | ¿Lo tocas? |
|---|---|---|
| `ServidorTCP.java` | Espera clientes y responde | Solo `procesarSolicitud` y el puerto |
| `ClienteTCP.java` | Envía un texto y muestra la respuesta | El texto, host y puerto |

## Pasos
1. Crea un proyecto Java ([cómo](../../00-preparacion/README.md#d4-proyecto-java)) con estas dos clases.
2. Ejecuta `ServidorTCP` (se queda esperando: es normal).
3. Ejecuta `ClienteTCP`. Debe salir `Respuesta: OK:hola`.
4. Escribe tu lógica en `procesarSolicitud`.

## ServidorTCP.java
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

    // TODO LO PROPIO DEL EJERCICIO VA AQUI
    static String procesarSolicitud(String mensaje) {
        // AQUI LOGICA
        return "OK:" + mensaje;
    }
}
```

## ClienteTCP.java
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

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Un formato `operacion:dato` | `String[] p = mensaje.split(":");` → [ejercicio 1](../../ejercicios/1-sockets/README.md) |
| Que el servidor haga preguntas | [Ejercicio 1, versión 4](../../ejercicios/1-sockets/README.md#versión-4--conversación-el-servidor-pregunta) |
| Chat entre clientes | [Ejercicio 1, versión 5](../../ejercicios/1-sockets/README.md#versión-5--chat-multicliente) |
| Llamarlo desde otro servidor | Copia el método `enviar` de `ClienteTCP` |
| Llamarlo desde PHP o C# | [logica/consumir](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |
| Guardar datos en archivo | [logica](../../logica/README.md#java-guardar-en-archivo) |

## Cómo defenderlo
- "`ServerSocket` abre el puerto y `accept()` **se bloquea** hasta que llega un cliente."
- "Cada cliente se atiende en **un hilo nuevo**, así uno lento no frena a los demás."
- "TCP es **orientado a conexión**: garantiza entrega y orden."
- "El protocolo es mío: una línea de pedido y una línea de respuesta, por eso uso `println` y `readLine`."

## Errores típicos
- `Address already in use`: ya hay un servidor corriendo. Detenlo.
- `Connection refused`: el servidor no está encendido o el puerto no coincide.
- El cliente se queda colgado: usaste `print` en vez de `println` (falta el salto de línea).
