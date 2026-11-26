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
