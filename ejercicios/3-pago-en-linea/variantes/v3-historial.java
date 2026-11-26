// 1) En IPlataforma.java (las dos copias) agregar:
java.util.List<String> historial() throws RemoteException;

// 2) En Plataforma.java agregar el atributo y el metodo:
private final java.util.ArrayList<String> movimientos = new java.util.ArrayList<>();

@Override
public synchronized java.util.List<String> historial() throws RemoteException {
    return new java.util.ArrayList<>(movimientos);
}

// 3) En realizarTransaccion, justo antes del  "return true;" :
movimientos.add(java.time.LocalDateTime.now() + " | " + origen + " -> " + destino + " | " + monto + " " + moneda);

// 4) En SitioComercio.java:
plataforma.historial().forEach(System.out::println);
