# 02 · Socket UDP en Java

[← Plantillas](../README.md) · [Índice](../../README.md) · **[INGE]** (puerto 6789)

**Qué es:** igual que TCP pero **sin conexión**: se mandan paquetes sueltos (datagramas).

| Archivo | Qué hace |
|---|---|
| `ServidorUDP.java` | Recibe un datagrama y contesta al que lo envió |
| `ClienteUDP.java` | Envía y espera la respuesta 5 segundos |

## Pasos
1. Ejecuta `ServidorUDP`.
2. Ejecuta `ClienteUDP`. Debe salir `Respuesta: OK:hola`.
3. Escribe tu lógica en `procesarSolicitud`.

## ServidorUDP.java
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

    static String procesarSolicitud(String mensaje) {
        // AQUI LOGICA
        return "OK:" + mensaje;
    }
}
```

## ClienteUDP.java
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

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Antifraude / alerta por monto | [Ejercicio 1, versión 6](../../ejercicios/1-sockets/README.md#versión-6--udp-antifraude) |
| Un banco por UDP | [Ejercicio 2, versión 1](../../ejercicios/2-rmi-sockets/README.md) |
| Mensajes largos | Agranda el `bufer` (1000 bytes) en los dos lados |

## Cómo defenderlo
- "UDP **no establece conexión** y no garantiza entrega ni orden; a cambio es más liviano."
- "El servidor responde usando la **dirección y puerto que vienen dentro del paquete** recibido."
- "Pongo un **timeout** porque si el servidor está apagado, UDP no avisa."
- "Uso `getLength()` para no leer basura del resto del búfer."

## TCP contra UDP
| | TCP | UDP |
|---|---|---|
| Conexión | Sí | No |
| Entrega garantizada | Sí | No |
| Clases | `ServerSocket`, `Socket` | `DatagramSocket`, `DatagramPacket` |
