# 03 · RMI en Java

[← Plantillas](../README.md) · [Índice](../../README.md) · **[INGE]**

**Qué es:** llamar un método de un objeto que vive en **otro programa**, como si fuera local.

| Archivo | Qué es | Lo necesita |
|---|---|---|
| `IServicio.java` | La **interfaz** (contrato) | Servidor **y** cliente |
| `Respuesta.java` | El objeto que viaja (`Serializable`) | Servidor **y** cliente |
| `Servicio.java` | La implementación | Servidor |
| `ServidorRMI.java` | Publica el objeto en el registro | Servidor |
| `ClienteRMI.java` | Busca el objeto y lo llama | Cliente |

## Pasos
1. Pon los 5 archivos en un proyecto (si son dos proyectos, la interfaz y la respuesta van en **los dos, con el mismo paquete**).
2. Ejecuta `ServidorRMI`.
3. Ejecuta `ClienteRMI`. Debe salir `Respuesta{ok=true, mensaje=Recibido: hola}`.

## IServicio.java
```java
import java.rmi.Remote;
import java.rmi.RemoteException;

// CONTRATO: lo que el cliente puede llamar a distancia.
public interface IServicio extends Remote {

    // TODOS los metodos llevan "throws RemoteException"
    Respuesta procesar(String dato) throws RemoteException;
}
```

## Respuesta.java
```java
import java.io.Serializable;

// Objeto que viaja por la red: por eso implementa Serializable.
public class Respuesta implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean ok;
    private final String mensaje;

    public Respuesta(boolean ok, String mensaje) {
        this.ok = ok;
        this.mensaje = mensaje;
    }

    public boolean isOk() { return ok; }
    public String getMensaje() { return mensaje; }

    @Override
    public String toString() {
        return "Respuesta{ok=" + ok + ", mensaje=" + mensaje + "}";
    }
}
```

## Servicio.java
```java
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
```

## ServidorRMI.java
```java
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Levanta el registro RMI y publica el objeto con un NOMBRE.
public class ServidorRMI {

    public static void main(String[] args) throws Exception {
        // Solo si el cliente esta en OTRA computadora, quitar // y poner tu IP:
        // System.setProperty("java.rmi.server.hostname", "192.168.1.10");

        Registry reg = LocateRegistry.createRegistry(1099);
        reg.rebind("Servicio", new Servicio());   // el cliente usa este mismo nombre
        System.out.println("Servidor RMI listo en el puerto 1099");
    }
}
```

## ClienteRMI.java
```java
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClienteRMI {

    public static void main(String[] args) throws Exception {
        String dato = args.length > 0 ? args[0] : "hola";

        Registry reg = LocateRegistry.getRegistry("localhost", 1099);
        IServicio servicio = (IServicio) reg.lookup("Servicio");  // mismo nombre que rebind

        // AQUI LOGICA (pedir datos, mostrar menu, etc.)
        Respuesta r = servicio.procesar(dato);
        System.out.println(r);
    }
}
```

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Otro método | Declararlo en la interfaz (con `throws RemoteException`) **y** implementarlo |
| Devolver una lista | `ArrayList<Respuesta>` (ya es serializable) |
| Que el servidor RMI llame a sockets | Copia `ClienteTCP.enviar` / `ClienteUDP.enviar` → [ejercicio 2](../../ejercicios/2-rmi-sockets/README.md) |
| Dos servidores RMI en la misma PC | El segundo usa `createRegistry(1100)` |

## Cómo defenderlo
- "La interfaz extiende `Remote` y es el **contrato** que conocen los dos lados."
- "La implementación extiende `UnicastRemoteObject`, que la **exporta** para recibir llamadas."
- "`createRegistry(1099)` crea la guía telefónica; `rebind` registra el objeto con un nombre; el cliente hace `lookup`."
- "El cliente recibe un **stub** (un representante local); los objetos viajan **serializados**."
- "Todo método remoto lanza `RemoteException` porque la red puede fallar."

## Errores típicos
- `NotBoundException`: el nombre del `lookup` no es **idéntico** al del `rebind` (cuidan mayúsculas).
- `Connection refused`: puerto distinto o servidor apagado.
- `NotSerializableException`: falta `implements Serializable`.
- `ClassCastException` / `ClassNotFoundException`: la interfaz está en paquetes distintos en cliente y servidor.
