using System;
using System.Collections.Generic;
using System.Web.Services;
using System.Web.Services.Protocols;

[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class Servicio : System.Web.Services.WebService
{
    // Datos en memoria (si no piden base de datos)
    private static List<Dato> datos = new List<Dato>
    {
        new Dato { Codigo = "A1", Nombre = "Ejemplo uno", Monto = 100 },
        new Dato { Codigo = "A2", Nombre = "Ejemplo dos", Monto = 200 }
    };

    // Cada [WebMethod] es una operacion que aparece en el WSDL

    [WebMethod]
    public bool Verificar(string codigo)          // devuelve si / no
    {
        // AQUI LOGICA
        return datos.Exists(d => d.Codigo == codigo);
    }

    [WebMethod]
    public Dato Buscar(string codigo)             // devuelve un objeto
    {
        Dato d = datos.Find(x => x.Codigo == codigo);
        if (d == null)
        {
            throw new SoapException("No existe " + codigo, SoapException.ClientFaultCode);
        }
        // AQUI LOGICA
        return d;
    }

    [WebMethod]
    public Dato[] Listar()                        // devuelve una lista (ArrayOfDato)
    {
        // AQUI LOGICA
        return datos.ToArray();
    }
}

// Clase que viaja en la respuesta: propiedades public
public class Dato
{
    public string Codigo { get; set; }
    public string Nombre { get; set; }
    public double Monto { get; set; }
}
