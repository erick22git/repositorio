import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

// SOAP 1.1 en Java SIN librerias (solo el JDK). Publica el WSDL en ?wsdl.
// Mismas operaciones que el ASMX de la plantilla 05: Verificar, Buscar, Listar, Registrar.
public class ServidorSoap {

    static final int PUERTO = 8086;
    static final String RUTA = "/Servicio";
    static final String NS = "http://tempuri.org/";

    static class Dato {
        String codigo, nombre;
        double monto;
        Dato(String c, String n, double m) { codigo = c; nombre = n; monto = m; }
        String xml() {
            return "<Codigo>" + esc(codigo) + "</Codigo><Nombre>" + esc(nombre) + "</Nombre><Monto>" + monto + "</Monto>";
        }
    }

    static final Map<String, Dato> DATOS = new LinkedHashMap<>();

    // ================== OPERACIONES (AQUI LOGICA) ==================
    static String operar(String op, Map<String, String> p) throws SoapFault {
        switch (op) {
            case "Verificar":
                return "<VerificarResult>" + DATOS.containsKey(p.get("codigo")) + "</VerificarResult>";
            case "Buscar": {
                Dato d = DATOS.get(p.get("codigo"));
                if (d == null) throw new SoapFault("Client", "No existe " + p.get("codigo"));
                return "<BuscarResult>" + d.xml() + "</BuscarResult>";
            }
            case "Listar": {
                StringBuilder sb = new StringBuilder("<ListarResult>");
                for (Dato d : DATOS.values()) sb.append("<Dato>").append(d.xml()).append("</Dato>");
                return sb.append("</ListarResult>").toString();
            }
            case "Registrar": {
                String codigo = p.get("codigo");
                double monto;
                try { monto = Double.parseDouble(p.get("monto")); }
                catch (Exception e) { throw new SoapFault("Client", "monto invalido"); }
                if (codigo == null || codigo.isBlank() || monto <= 0) throw new SoapFault("Client", "Datos invalidos");
                if (DATOS.containsKey(codigo)) throw new SoapFault("Client", "Ya existe " + codigo);
                Dato d = new Dato(codigo, p.getOrDefault("nombre", ""), monto);
                DATOS.put(codigo, d);
                return "<RegistrarResult>" + d.xml() + "</RegistrarResult>";
            }
            default:
                throw new SoapFault("Client", "Operacion desconocida " + op);
        }
    }

    public static void main(String[] args) throws IOException {
        DATOS.put("A1", new Dato("A1", "Ejemplo uno", 100));
        DATOS.put("A2", new Dato("A2", "Ejemplo dos", 200));
        HttpServer server = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        server.createContext(RUTA, ServidorSoap::atender);
        server.start();
        System.out.println("SOAP en http://localhost:" + PUERTO + RUTA + "   WSDL: http://localhost:" + PUERTO + RUTA + "?wsdl");
    }

    static synchronized void atender(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("GET")) {
            responder(ex, 200, Wsdl.texto("http://localhost:" + PUERTO + RUTA));
            return;
        }
        try {
            Document doc = DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder().parse(ex.getRequestBody());
            Element body = (Element) doc.getElementsByTagNameNS("*", "Body").item(0);
            Element operacion = primerHijo(body);
            Map<String, String> params = new HashMap<>();
            for (Node n = operacion.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) params.put(n.getLocalName(), n.getTextContent());
            }
            String op = operacion.getLocalName();
            System.out.println("Llego " + op + " " + params);
            String resultado = operar(op, params);
            responder(ex, 200, sobre("<" + op + "Response xmlns=\"" + NS + "\">" + resultado + "</" + op + "Response>"));
        } catch (SoapFault f) {
            responder(ex, 500, sobre("<soap:Fault><faultcode>soap:" + f.codigo + "</faultcode><faultstring>"
                    + esc(f.getMessage()) + "</faultstring></soap:Fault>"));
        } catch (Exception e) {
            responder(ex, 500, sobre("<soap:Fault><faultcode>soap:Server</faultcode><faultstring>"
                    + esc(String.valueOf(e.getMessage())) + "</faultstring></soap:Fault>"));
        }
    }

    static Element primerHijo(Element e) {
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element) return (Element) n;
        throw new IllegalArgumentException("Body vacio");
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

    static class SoapFault extends Exception {
        final String codigo;
        SoapFault(String codigo, String mensaje) { super(mensaje); this.codigo = codigo; }
    }
}
