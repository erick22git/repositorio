using System.Collections.Generic;
using System.Web.Services;
using System.Web.Services.Protocols;

// Hotel Mirador Andino: tarifas e historial
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
public class Servicio : System.Web.Services.WebService
{
    private static readonly Dictionary<string, double> tarifas = new Dictionary<string, double>
    {
        { "B101", 180 }, { "B201", 400 }
    };

    private static readonly Dictionary<string, List<Movimiento>> movimientos = new Dictionary<string, List<Movimiento>>
    {
        { "B101", new List<Movimiento> { new Movimiento { fecha = "2026-10-01", noches = 2 },
                                         new Movimiento { fecha = "2026-10-15", noches = 1 } } }
    };

    [WebMethod]
    public double consultarTarifa(string codHabitacion)
    {
        double tarifa;
        if (!tarifas.TryGetValue(codHabitacion ?? "", out tarifa))
        {
            throw new SoapException("No existe la habitacion", SoapException.ClientFaultCode);
        }
        return tarifa;
    }

    // Un historial son varios movimientos, por eso devuelve un arreglo
    [WebMethod]
    public Movimiento[] historial(string codHabitacion)
    {
        List<Movimiento> lista;
        return movimientos.TryGetValue(codHabitacion ?? "", out lista) ? lista.ToArray() : new Movimiento[0];
    }
}

public class Movimiento
{
    public string fecha { get; set; }
    public int noches { get; set; }
}
