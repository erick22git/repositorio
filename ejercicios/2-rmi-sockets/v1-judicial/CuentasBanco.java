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
