// Contrato WSDL del servicio de cotizaciones (escrito a mano, como en la plantilla 15)
public class Wsdl {

    static String texto(String url) {
        return """
<?xml version="1.0" encoding="utf-8"?>
<wsdl:definitions xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:tns="http://tempuri.org/"
    xmlns:s="http://www.w3.org/2001/XMLSchema" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    targetNamespace="http://tempuri.org/">
  <wsdl:types>
    <s:schema elementFormDefault="qualified" targetNamespace="http://tempuri.org/">
      <s:complexType name="Cotizacion">
        <s:sequence>
          <s:element minOccurs="0" maxOccurs="1" name="fecha" type="s:string"/>
          <s:element minOccurs="1" maxOccurs="1" name="cotizacion" type="s:double"/>
          <s:element minOccurs="1" maxOccurs="1" name="cotizacionOficial" type="s:double"/>
        </s:sequence>
      </s:complexType>
      <s:complexType name="Respuesta">
        <s:sequence>
          <s:element minOccurs="1" maxOccurs="1" name="exito" type="s:boolean"/>
          <s:element minOccurs="0" maxOccurs="1" name="mensaje" type="s:string"/>
          <s:element minOccurs="0" maxOccurs="1" name="cotizacion" type="tns:Cotizacion"/>
        </s:sequence>
      </s:complexType>
      <s:element name="obtenerCotizacion"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="fecha" type="s:string"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="obtenerCotizacionResponse"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="obtenerCotizacionResult" type="tns:Respuesta"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="registrarCotizacion"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="fecha" type="s:string"/>
        <s:element minOccurs="0" maxOccurs="1" name="monto" type="s:string"/>
      </s:sequence></s:complexType></s:element>
      <s:element name="registrarCotizacionResponse"><s:complexType><s:sequence>
        <s:element minOccurs="0" maxOccurs="1" name="registrarCotizacionResult" type="tns:Respuesta"/>
      </s:sequence></s:complexType></s:element>
    </s:schema>
  </wsdl:types>
  <wsdl:message name="obtenerCotizacionSoapIn"><wsdl:part name="parameters" element="tns:obtenerCotizacion"/></wsdl:message>
  <wsdl:message name="obtenerCotizacionSoapOut"><wsdl:part name="parameters" element="tns:obtenerCotizacionResponse"/></wsdl:message>
  <wsdl:message name="registrarCotizacionSoapIn"><wsdl:part name="parameters" element="tns:registrarCotizacion"/></wsdl:message>
  <wsdl:message name="registrarCotizacionSoapOut"><wsdl:part name="parameters" element="tns:registrarCotizacionResponse"/></wsdl:message>
  <wsdl:portType name="CotizacionesSoap">
    <wsdl:operation name="obtenerCotizacion"><wsdl:input message="tns:obtenerCotizacionSoapIn"/><wsdl:output message="tns:obtenerCotizacionSoapOut"/></wsdl:operation>
    <wsdl:operation name="registrarCotizacion"><wsdl:input message="tns:registrarCotizacionSoapIn"/><wsdl:output message="tns:registrarCotizacionSoapOut"/></wsdl:operation>
  </wsdl:portType>
  <wsdl:binding name="CotizacionesSoap" type="tns:CotizacionesSoap">
    <soap:binding transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="obtenerCotizacion"><soap:operation soapAction="http://tempuri.org/obtenerCotizacion" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
    <wsdl:operation name="registrarCotizacion"><soap:operation soapAction="http://tempuri.org/registrarCotizacion" style="document"/>
      <wsdl:input><soap:body use="literal"/></wsdl:input><wsdl:output><soap:body use="literal"/></wsdl:output></wsdl:operation>
  </wsdl:binding>
  <wsdl:service name="Cotizaciones">
    <wsdl:port name="CotizacionesSoap" binding="tns:CotizacionesSoap">
      <soap:address location="URL"/>
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>
""".replace("URL", url);
    }
}
