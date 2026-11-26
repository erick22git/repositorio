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
