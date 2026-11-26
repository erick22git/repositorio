import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

// SOAP en Java sin librerias. Los datos los pide a la API REST de Node (misma base de datos).
public class ServidorCotizaciones {

    static final int PUERTO = 8086;
    static final String RUTA = "/cotizaciones";
    static final String NS = "http://tempuri.org/";
    static final String REST = "http://localhost:3000/api/cotizaciones";

    // ================== OPERACIONES ==================
    static String operar(String op, Map<String, String> p) throws Exception {
        if (op.equals("obtenerCotizacion")) {
            HttpResponse<String> r = pedir("GET", REST + "/" + p.getOrDefault("fecha", "").trim(), null);
            return "<obtenerCotizacionResult>" + respuesta(r.body()) + "</obtenerCotizacionResult>";
        }
        if (op.equals("registrarCotizacion")) {
            String monto = p.getOrDefault("monto", "").trim().replace(',', '.');
            String json = "{\"fecha\":\"" + p.getOrDefault("fecha", "").trim() + "\",\"monto\":\"" + monto + "\"}";
            HttpResponse<String> r = pedir("POST", REST, json);
            return "<registrarCotizacionResult>" + respuesta(r.body()) + "</registrarCotizacionResult>";
        }
        throw new IllegalArgumentException("Operacion desconocida " + op);
    }

    // convierte el JSON de la API REST en el XML de la respuesta
    static String respuesta(String json) {
        String xml = "<exito>" + "true".equals(campo(json, "exito")) + "</exito><mensaje>" + esc(campo(json, "mensaje")) + "</mensaje>";
        if (json.contains("\"cotizacion\":{")) {
            xml += "<cotizacion><fecha>" + campo(json, "fecha") + "</fecha><cotizacion>" + campo(json, "cotizacion")
                    + "</cotizacion><cotizacionOficial>" + campo(json, "cotizacion_oficial") + "</cotizacionOficial></cotizacion>";
        }
        return xml;
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        server.createContext(RUTA, ServidorCotizaciones::atender);
        server.start();
        System.out.println("SOAP en http://localhost:" + PUERTO + RUTA + "   WSDL: http://localhost:" + PUERTO + RUTA + "?wsdl");
    }

    static void atender(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("GET")) {
            responder(ex, 200, Wsdl.texto("http://localhost:" + PUERTO + RUTA));
            return;
        }
        try {
            Document doc = DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder().parse(ex.getRequestBody());
            Element body = (Element) doc.getElementsByTagNameNS("*", "Body").item(0);
            Element operacion = null;
            for (Node n = body.getFirstChild(); n != null && operacion == null; n = n.getNextSibling()) {
                if (n instanceof Element) operacion = (Element) n;
            }
            Map<String, String> params = new HashMap<>();
            for (Node n = operacion.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) params.put(n.getLocalName(), n.getTextContent());
            }
            String op = operacion.getLocalName();
            System.out.println("Llego " + op + " " + params);
            responder(ex, 200, sobre("<" + op + "Response xmlns=\"" + NS + "\">" + operar(op, params) + "</" + op + "Response>"));
        } catch (java.net.ConnectException e) {
            responder(ex, 500, sobre("<soap:Fault><faultcode>soap:Server</faultcode><faultstring>La API REST no responde</faultstring></soap:Fault>"));
        } catch (Exception e) {
            responder(ex, 500, sobre("<soap:Fault><faultcode>soap:Client</faultcode><faultstring>" + esc(String.valueOf(e.getMessage())) + "</faultstring></soap:Fault>"));
        }
    }

    static HttpResponse<String> pedir(String metodo, String url, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json").header("Content-Type", "application/json");
        b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
                .send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*(\"([^\"]*)\"|[-0-9.]+|true|false)").matcher(json);
        if (!m.find()) return "";
        return m.group(2) != null ? m.group(2) : m.group(1);
    }

    static String sobre(String contenido) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + contenido + "</soap:Body></soap:Envelope>";
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void responder(HttpExchange ex, int codigo, String xml) throws IOException {
        byte[] b = xml.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
        ex.sendResponseHeaders(codigo, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }
}
