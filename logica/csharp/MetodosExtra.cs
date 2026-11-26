// Pegar DENTRO de la clase Servicio (App_Code/Servicio.cs)

[WebMethod]
public Dato Registrar(string codigo, string nombre, double monto)
{
    if (string.IsNullOrWhiteSpace(codigo) || monto <= 0)
        throw new SoapException("Datos invalidos", SoapException.ClientFaultCode);
    if (datos.Exists(d => d.Codigo == codigo))
        throw new SoapException("Ya existe " + codigo, SoapException.ClientFaultCode);

    var nuevo = new Dato { Codigo = codigo, Nombre = nombre, Monto = monto };
    datos.Add(nuevo);
    return nuevo;
}

[WebMethod]
public bool Eliminar(string codigo)
{
    return datos.RemoveAll(d => d.Codigo == codigo) > 0;
}

[WebMethod]
public double Total()
{
    double suma = 0;
    foreach (var d in datos) suma += d.Monto;
    return suma;
}
