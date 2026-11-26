import java.net.URI;
import java.net.http.*;

// Consumir GraphQL desde Java: es un POST con {"query":"..."}
public class GraphQLDesdeJava {

    public static void main(String[] args) throws Exception {
        System.out.println(consultar("{ recursos { codigo nombre estado } }"));
        // Con comillas dentro de la consulta:
        System.out.println(consultar("{ recurso(codigo: \"A1\") { nombre } }"));
    }

    static String consultar(String query) throws Exception {
        String json = "{\"query\":\"" + query.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:4000/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
