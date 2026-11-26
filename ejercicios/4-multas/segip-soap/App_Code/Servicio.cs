using System.Collections.Generic;
using System.Web.Services;

// SEGIP: responde si un CI existe.
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class Servicio : System.Web.Services.WebService
{
    private static readonly List<string> carnets = new List<string> { "11021654", "5551234", "7778899" };

    [WebMethod]
    public bool verificarCI(string ci)
    {
        return ci != null && carnets.Contains(ci.Trim());
    }
}
