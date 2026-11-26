using System;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Web.Services;
using System.Web.Services.Protocols;

// CENTRAL DE PAGOS: hacia el cliente es SOAP; por detras consume GraphQL (SEGIP) y REST (empresas)
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class Servicio : System.Web.Services.WebService
{
    private const string SEGIP = "http://localhost:4000/graphql";

    private static readonly Dictionary<string, string> EMPRESAS = new Dictionary<string, string>
    {
        { "CESSA",  "http://localhost:8001/api/facturas" },
        { "ELAPAS", "http://localhost:8002/api/facturas" }
    };

    [WebMethod]
    public Deuda[] consultarDeudas(string ci)
    {
        VerificarPersona(ci);

        var deudas = new List<Deuda>();
        foreach (var empresa in EMPRESAS)
        {
            string json = Llamar(empresa.Value + "?ci=" + Uri.EscapeDataString(ci), "GET", null);
            foreach (var f in new JavaScriptSerializer().Deserialize<List<Factura>>(json))
            {
                deudas.Add(new Deuda { empresa = empresa.Key, idFactura = f.id, periodo = f.periodo, monto = f.monto });
            }
        }
        return deudas.ToArray();
    }

    [WebMethod]
    public string pagar(string empresa, int idFactura)
    {
        string url;
        if (!EMPRESAS.TryGetValue(empresa ?? "", out url))
        {
            throw new SoapException("Empresa desconocida", SoapException.ClientFaultCode);
        }
        Llamar(url + "/" + idFactura, "PATCH", "{\"estado\":\"pagada\"}");
        return "Pago registrado en " + empresa;
    }

    // Consulta GraphQL: si el CI no existe, se corta aqui
    private void VerificarPersona(string ci)
    {
        var consulta = new
        {
            query = "query($ci: String!) { persona(ci: $ci) { nombre } }",
            variables = new { ci = ci }
        };
        string json = Llamar(SEGIP, "POST", new JavaScriptSerializer().Serialize(consulta));
        if (json.Contains("\"persona\":null"))
        {
            throw new SoapException("SEGIP: el CI no existe", SoapException.ClientFaultCode);
        }
    }

    // Una sola funcion para todas las llamadas HTTP; traduce errores a SoapException
    private string Llamar(string url, string metodo, string cuerpo)
    {
        try
        {
            using (var web = new WebClient { Encoding = Encoding.UTF8 })
            {
                web.Headers["Accept"] = "application/json";
                web.Headers["Content-Type"] = "application/json";
                return metodo == "GET" ? web.DownloadString(url) : web.UploadString(url, metodo, cuerpo);
            }
        }
        catch (WebException ex)
        {
            var r = ex.Response as HttpWebResponse;
            if (r == null) throw new SoapException("No responde: " + url, SoapException.ServerFaultCode);
            throw new SoapException("Error HTTP " + (int)r.StatusCode, SoapException.ClientFaultCode);
        }
    }
}

public class Factura
{
    public int id { get; set; }
    public string ci { get; set; }
    public string periodo { get; set; }
    public double monto { get; set; }
}

public class Deuda
{
    public string empresa { get; set; }
    public int idFactura { get; set; }
    public string periodo { get; set; }
    public double monto { get; set; }
}
