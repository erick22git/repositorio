using System;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Web.Services;
using System.Web.Services.Protocols;

// SERVIDOR DE PAGOS (contrato del enunciado):
//   Deuda[] verDeuda(String CI, String primerApellido, String segundoApellido, String nombres)
//   PagarDeuda(Deuda c)
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
    public Deuda[] verDeuda(string CI, string primerApellido, string segundoApellido, string nombres)
    {
        if (string.IsNullOrEmpty(CI) || string.IsNullOrEmpty(nombres) || string.IsNullOrEmpty(primerApellido))
        {
            throw new SoapException("Debe llenar CI, nombres y primer apellido", SoapException.ClientFaultCode);
        }
        VerificarSegip(CI.Trim(), nombres, primerApellido, segundoApellido);

        var deudas = new List<Deuda>();
        foreach (var empresa in EMPRESAS)
        {
            string json = Llamar(empresa.Value + "?ci=" + Uri.EscapeDataString(CI.Trim()), "GET", null);
            foreach (var f in new JavaScriptSerializer().Deserialize<List<Factura>>(json))
            {
                deudas.Add(new Deuda { empresa = empresa.Key, idFactura = f.id, ci = f.ci, periodo = f.periodo, monto = f.monto });
            }
        }
        return deudas.ToArray();
    }

    [WebMethod]
    public string PagarDeuda(Deuda c)
    {
        string url;
        if (c == null || !EMPRESAS.TryGetValue(c.empresa ?? "", out url))
        {
            throw new SoapException("Deuda invalida", SoapException.ClientFaultCode);
        }
        // la empresa responde 409 si ya estaba pagada
        Llamar(url + "/" + c.idFactura, "PATCH", "{\"estado\":\"pagada\"}");
        return "Pago registrado en " + c.empresa + ": factura " + c.idFactura + " por Bs " + c.monto;
    }

    // el SEGIP verifica que exista el CI y que los datos coincidan
    private void VerificarSegip(string ci, string nombres, string primerApellido, string segundoApellido)
    {
        var consulta = new
        {
            query = "query($ci: String!) { persona(ci: $ci) { nombres primerApellido segundoApellido } }",
            variables = new { ci = ci }
        };
        string json = Llamar(SEGIP, "POST", new JavaScriptSerializer().Serialize(consulta));
        var respuesta = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(json);
        var data = respuesta["data"] as Dictionary<string, object>;
        var persona = data == null ? null : data["persona"] as Dictionary<string, object>;
        if (persona == null)
        {
            throw new SoapException("SEGIP: el CI " + ci + " no esta registrado", SoapException.ClientFaultCode);
        }
        if (!Igual(persona["nombres"], nombres) || !Igual(persona["primerApellido"], primerApellido)
            || !Igual(persona["segundoApellido"], segundoApellido))
        {
            throw new SoapException("SEGIP: los datos no coinciden con el CI", SoapException.ClientFaultCode);
        }
    }

    private static bool Igual(object a, string b)
    {
        return string.Equals(Convert.ToString(a).Trim(), (b ?? "").Trim(), StringComparison.OrdinalIgnoreCase);
    }

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
            if ((int)r.StatusCode == 409) throw new SoapException("La factura ya fue pagada", SoapException.ClientFaultCode);
            throw new SoapException("Error HTTP " + (int)r.StatusCode + " en " + url, SoapException.ClientFaultCode);
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
    public string ci { get; set; }
    public string periodo { get; set; }
    public double monto { get; set; }
}
