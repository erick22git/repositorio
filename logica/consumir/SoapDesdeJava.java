import java.net.URI;
import java.net.http.*;
import java.util.regex.*;

// [EXTRA] Consumir un servicio SOAP desde Java SIN wsimport ni librerias.
public class SoapDesdeJava {

    // CAMBIAR el puerto por el que abrio Visual Studio
    static final String URL = "http://localhost:51305/Servicio.asmx";

    public static void main(String[] args) {
        System.out.println(llamar("Verificar", "<codigo>A1</codigo>"));
    }

    // operacion = nombre del [WebMethod];  parametros = <nombre>valor</nombre> (igual que en el WSDL)
    static String llamar(String operacion, String parametros) {
        String sobre = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                + "<" + operacion + " xmlns=\"http://tempuri.org/\">" + parametros + "</" + operacion + ">"
                + "</soap:Body></soap:Envelope>";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "\"http://tempuri.org/" + operacion + "\"")
                    .POST(HttpRequest.BodyPublishers.ofString(sobre))
                    .build();
            String xml = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString()).body();

            Matcher ok = Pattern.compile("<" + operacion + "Result>(.*)</" + operacion + "Result>",
                    Pattern.DOTALL).matcher(xml);
            if (ok.find()) return ok.group(1);

            Matcher error = Pattern.compile("<faultstring>(.*?)</faultstring>", Pattern.DOTALL).matcher(xml);
            return error.find() ? "ERROR:" + error.group(1) : "ERROR:respuesta inesperada " + xml;

        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
