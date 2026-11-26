import java.net.URI;
import java.net.http.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.regex.*;

// ORQUESTADOR: cotizacion (TCP) + token (ATC) + los dos bancos (REST)
public class Plataforma extends UnicastRemoteObject implements IPlataforma {
    private static final long serialVersionUID = 1L;

    static final String ATC = "http://localhost:8083/api/login";
    static final String[] BANCOS = {
            "http://localhost:8001/api/cuentas",     // BNB (PHP / Laravel)
            "http://localhost:8085/api/cuentas"      // Union (Java)
    };

    public Plataforma() throws RemoteException {
        super();
    }

    @Override
    public synchronized boolean realizarTransaccion(String origen, String destino, double monto, String moneda)
            throws RemoteException {
        try {
            if (monto <= 0 || origen.equals(destino)) return fallo("datos invalidos");

            // 1) Cotizacion del dia
            double cambio = Double.parseDouble(ClienteTCP.enviar("localhost", 5002, LocalDate.now().toString()));

            // 2) Token
            String login = pedir("POST", ATC, "{\"user\":\"comercio\",\"password\":\"1234\"}", null).body();
            String token = campo(login, "token");
            if (token == null) return fallo("ATC no dio token");

            // 3) Buscar cada cuenta en los bancos
            String[] cliente = buscar(origen, token);
            String[] vendedor = buscar(destino, token);
            if (cliente == null || vendedor == null) return fallo("cuenta no encontrada");

            // 4) Convertir el monto a la moneda de cada cuenta
            double aDebitar = convertir(monto, moneda, cliente[1], cambio);
            double aAbonar = convertir(monto, moneda, vendedor[1], cambio);

            // 5) Debitar (el banco rechaza con 409 si no alcanza)
            if (!operar(cliente[0], "debito", aDebitar, token)) return fallo("no se pudo debitar");

            // 6) Abonar. Si falla, DEVOLVER el dinero (compensacion)
            if (!operar(vendedor[0], "abono", aAbonar, token)) {
                operar(cliente[0], "abono", aDebitar, token);
                return fallo("no se pudo abonar, se devolvio");
            }
            System.out.println("Transaccion OK: " + monto + " " + moneda + " de " + origen + " a " + destino);
            return true;

        } catch (Exception e) {
            return fallo("error: " + e);
        }
    }

    private boolean fallo(String motivo) {
        System.out.println("Transaccion RECHAZADA: " + motivo);
        return false;
    }

    // Devuelve {url de la cuenta, moneda} o null
    private String[] buscar(String nro, String token) throws Exception {
        for (String banco : BANCOS) {
            HttpResponse<String> r = pedir("GET", banco + "/" + nro, null, token);
            if (r.statusCode() == 200) return new String[]{banco + "/" + nro, campo(r.body(), "moneda")};
        }
        return null;
    }

    private boolean operar(String url, String operacion, double monto, String token) throws Exception {
        String json = "{\"operacion\":\"" + operacion + "\",\"monto\":" + monto + "}";
        return pedir("PATCH", url, json, token).statusCode() == 200;
    }

    static double convertir(double monto, String de, String a, double cambio) {
        if (de.equalsIgnoreCase(a)) return monto;
        double r = de.equalsIgnoreCase("USD") ? monto * cambio : monto / cambio;
        return Math.round(r * 100) / 100.0;
    }

    private HttpResponse<String> pedir(String metodo, String url, String json, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                                      : HttpRequest.BodyPublishers.ofString(json));
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
