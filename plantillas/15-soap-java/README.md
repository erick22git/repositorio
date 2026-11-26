# 15 · Servicio SOAP en Java, sin librerías

[← Plantillas](../README.md) · [Índice](../../README.md) · **[EXTRA]** (la Práctica 5 pidió "SOAP con Java")

**Qué es:** un servicio SOAP 1.1 hecho solo con el JDK (`HttpServer` + lector XML). Publica su **WSDL** en `?wsdl`, así que lo consumen igual que al ASMX: referencia de servicio en C#, SoapUI, [SoapDesdeJava](../../logica/README.md#soapdesdejavajava) o PHP `SoapClient`.

Tiene **las mismas operaciones y nombres** que la [plantilla 05 (C#)](../05-soap-csharp/README.md): `Verificar`, `Buscar`, `Listar`, `Registrar`. Un cliente hecho para una sirve para la otra cambiando solo la URL.

> ¿Por qué no `@WebService` (JAX-WS)? Desde Java 11 JAX-WS ya no viene en el JDK: hay que bajar `jaxws-rt` con Maven (internet). Si tu PC ya lo tiene, también vale (ver al final).

| Archivo | Qué hace | ¿Lo tocas? |
|---|---|---|
| `ServidorSoap.java` | Recibe el sobre SOAP, llama a la operación y arma la respuesta | `operar(...)` y los datos |
| `Wsdl.java` | El contrato, escrito a mano | Si agregas o cambias operaciones |

## Pasos
1. Proyecto Java (NetBeans *Java with Ant* o consola) con los dos archivos.
2. Ejecuta `ServidorSoap`.
3. Navegador: `http://localhost:8086/Servicio?wsdl` → sale el WSDL.
4. Prueba con curl o con un cliente (abajo).

```bat
javac -d out *.java
java -cp out ServidorSoap
```

## ServidorSoap.java
```java
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
```

## Wsdl.java
```java
// El contrato (WSDL) escrito a mano. Si agregas una operacion, agregala en las 4 partes:
// 1) elementos en <types>   2) <message>   3) <portType>   4) <binding>
public class Wsdl {

    static String texto(String url) {
        return """
<?xml version="1.0" encoding="utf-8"?>
<wsdl:definitions xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:tns="http://tempuri.org/"
    xmlns:s="http://www.w3.org/2001/XMLSchema" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    targetNamespace="http://tempuri.org/">
  <wsdl:types>
    <s:schema elementFormDefault="qualified" targetNamespace="http://tempuri.org/">
      <s:complexType name="Dato">
        <s:sequence>
          <s:element minOccurs="0" maxOccurs="1" name="Codigo" type="s:string"/>
          <s:element minOccurs="0" maxOccurs="1" name="Nombre" type="s:string"/>
          <s:element minOccurs="1" maxOccurs="1" name="Monto" type="s:double"/>
        </s:sequence>
      </s:complexType>
      <s:complexType name="ArrayOfDato">
        <s:sequence>
          <s:element minOccurs="0" maxOccurs="unbounded" name="Dato" nillable="true" type="tns:Dato"/>
        </s:sequence>
      </s:complexType>
      <s:element name="Verificar"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="codigo" type="s:string"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="VerificarResponse"><s:complexType><s:sequence>
        <s:element minOccurs="1" maxOccurs="1" name="VerificarResult" type="s:boolean"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="Buscar"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="codigo" type="s:string"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="BuscarResponse"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="BuscarResult" type="tns:Dato"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="Listar"><s:complexType/></s:element>
      <s:element name="ListarResponse"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="ListarResult" type="tns:ArrayOfDato"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="Registrar"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="codigo" type="s:string"/>
        <s:element minOccurs="0" maxOccurs="1" name="nombre" type="s:string"/>
        <s:element minOccurs="1" maxOccurs="1" name="monto" type="s:double"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="RegistrarResponse"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="RegistrarResult" type="tns:Dato"/>
      </s:sequence></s:complexType></s:element>
    </s:schema>
  </wsdl:types>
  <wsdl:message name="VerificarSoapIn"><wsdl:part name="parameters" element="tns:Verificar"/></wsdl:message>
  <wsdl:message name="VerificarSoapOut"><wsdl:part name="parameters" element="tns:VerificarResponse"/></wsdl:message>
  <wsdl:message name="BuscarSoapIn"><wsdl:part name="parameters" element="tns:Buscar"/></wsdl:message>
  <wsdl:message name="BuscarSoapOut"><wsdl:part name="parameters" element="tns:BuscarResponse"/></wsdl:message>
  <wsdl:message name="ListarSoapIn"><wsdl:part name="parameters" element="tns:Listar"/></wsdl:message>
  <wsdl:message name="ListarSoapOut"><wsdl:part name="parameters" element="tns:ListarResponse"/></wsdl:message>
  <wsdl:message name="RegistrarSoapIn"><wsdl:part name="parameters" element="tns:Registrar"/></wsdl:message>
  <wsdl:message name="RegistrarSoapOut"><wsdl:part name="parameters" element="tns:RegistrarResponse"/></wsdl:message>
  <wsdl:portType name="ServicioSoap">
    <wsdl:operation name="Verificar"><wsdl:input message="tns:VerificarSoapIn"/><wsdl:output message="tns:VerificarSoapOut"/></wsdl:operation>
    <wsdl:operation name="Buscar"><wsdl:input message="tns:BuscarSoapIn"/><wsdl:output message="tns:BuscarSoapOut"/></wsdl:operation>
    <wsdl:operation name="Listar"><wsdl:input message="tns:ListarSoapIn"/><wsdl:output message="tns:ListarSoapOut"/></wsdl:operation>
    <wsdl:operation name="Registrar"><wsdl:input message="tns:RegistrarSoapIn"/><wsdl:output message="tns:RegistrarSoapOut"/></wsdl:operation>
  </wsdl:portType>
  <wsdl:binding name="ServicioSoap" type="tns:ServicioSoap">
    <soap:binding transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="Verificar"><soap:operation soapAction="http://tempuri.org/Verificar" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
    <wsdl:operation name="Buscar"><soap:operation soapAction="http://tempuri.org/Buscar" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
    <wsdl:operation name="Listar"><soap:operation soapAction="http://tempuri.org/Listar" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
    <wsdl:operation name="Registrar"><soap:operation soapAction="http://tempuri.org/Registrar" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
  </wsdl:binding>
  <wsdl:service name="Servicio">
    <wsdl:port name="ServicioSoap" binding="tns:ServicioSoap">
      <soap:address location="URL"/>
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>
""".replace("URL", url);
    }
}
```

## Probar
```bat
curl -X POST http://localhost:8086/Servicio -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"http://tempuri.org/Buscar\"" -d "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><Buscar xmlns=\"http://tempuri.org/\"><codigo>A1</codigo></Buscar></soap:Body></soap:Envelope>"
```
| Llamada | Respuesta |
|---|---|
| `Verificar(A1)` / `Verificar(X)` | `true` / `false` |
| `Buscar(A1)` | `Dato { Codigo A1, Nombre Ejemplo uno, Monto 100 }` |
| `Buscar(ZZ)` | **Fault** "No existe ZZ" |
| `Registrar(B1, Nuevo, 5.5)` | El `Dato` creado; repetido → Fault "Ya existe B1" |
| `Listar()` | Arreglo de `Dato` |

**Desde C# (Forms):** *Agregar referencia de servicio* → `http://localhost:8086/Servicio?wsdl` → nombre `srServicio` → `new srServicio.ServicioSoapClient().Verificar("A1")`.
**Desde Java:** [SoapDesdeJava](../../logica/README.md#soapdesdejavajava) con `URL = "http://localhost:8086/Servicio"`.

## Agregar una operación (ej. `double Total()`)
1. En `operar`: `case "Total": return "<TotalResult>" + suma + "</TotalResult>";`
2. En `Wsdl.java`, copiando `Listar`: elementos `Total` y `TotalResponse` (con `TotalResult` de tipo `s:double`), los dos `<wsdl:message>`, la `<wsdl:operation>` del `portType` y la del `binding` (con `soapAction="http://tempuri.org/Total"`).
3. Reinicia y, en el cliente C#, **Actualizar referencia de servicio**.

Tipos del WSDL: `s:string`, `s:int`, `s:double`, `s:boolean`. Los parámetros llegan como texto en `p.get("nombre")`: conviértelos con `Integer.parseInt`, `Double.parseDouble`.

## Con JAX-WS (solo si ya tienes el jar)
```java
@WebService(serviceName = "Servicio")
public class Servicio {
    @WebMethod
    public boolean Verificar(@WebParam(name = "codigo") String codigo) { return true; }
}
// main:  jakarta.xml.ws.Endpoint.publish("http://localhost:8086/Servicio", new Servicio());
```
Dependencia Maven: `com.sun.xml.ws:jaxws-rt:4.0.3`.

## Cómo defenderlo
- "SOAP manda **XML** dentro de un sobre `Envelope/Body`; el elemento del Body es el nombre de la operación."
- "El **WSDL** es el contrato: tipos, mensajes, operaciones (`portType`), cómo se envían (`binding`) y la dirección (`service`)."
- "Los errores viajan como **SOAP Fault** con `faultcode` Client o Server."
- "Es estilo *document/literal*, el mismo que usa ASMX, por eso un cliente .NET lo consume igual."
