// A) Una empresa mas: en el diccionario EMPRESAS agregar una linea
//    { "ENTEL", "http://localhost:8003/api/facturas" }
//    y levantar otra copia del proyecto Laravel en el puerto 8003 con su propia base.

// B) Total de deuda por CI (dentro de la clase Servicio):
[WebMethod]
public double totalDeuda(string ci)
{
    double total = 0;
    foreach (Deuda d in consultarDeudas(ci)) total += d.monto;
    return total;
}
