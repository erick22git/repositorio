// En App_Code/Servicio.cs, dentro de la clase Servicio:
[WebMethod]
public double pagarTodo(string ci)
{
    double total = 0;
    foreach (Deuda d in consultarDeudas(ci))     // reutiliza el metodo anterior
    {
        pagar(d.empresa, d.idFactura);
        total += d.monto;
    }
    return total;
}
