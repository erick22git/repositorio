import java.net.URI;
import java.net.http.*;
import java.util.Scanner;

// Entidad Recaudadora: 1) consulta por GraphQL   2) si se puede, paga por socket TCP
public class Recaudadora {

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Codigo de boleta: ");
        String codigo = sc.nextLine().trim().replaceAll("[\"\\\\]", "");
        System.out.print("CI del conductor: ");
        String ci = sc.nextLine().trim().replaceAll("[\"\\\\]", "");

        // La consulta GraphQL viaja dentro de un JSON: {"query":"..."}
        String json = "{\"query\":\"{ verificarMulta(codigo:\\\"" + codigo + "\\\", ci:\\\"" + ci
                + "\\\") { sePuedePagar mensaje multa { monto estado } } }\"}";

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:4000/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        String respuesta = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString()).body();
        System.out.println("Consulta: " + respuesta);

        if (!respuesta.contains("\"sePuedePagar\":true")) {
            System.out.println("No se puede pagar.");
            return;
        }

        System.out.print("Pagar? (s/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("s")) {
            System.out.println(ClienteTCP.enviar("localhost", 5002, "PAGAR:" + codigo + ":" + ci));
        }
    }
}
