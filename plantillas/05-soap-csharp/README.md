# 05 · Servicio SOAP en C# (ASMX)

[← Plantillas](../README.md) · [Índice](../../README.md) · **[INGE]**

**Qué es:** un servicio web que publica **operaciones** (métodos) y describe su contrato en un **WSDL**.

| Archivo | Qué es |
|---|---|
| `Servicio.asmx` | La "puerta" (una sola línea que apunta al código) |
| `App_Code/Servicio.cs` | El código: los `[WebMethod]` |

## Pasos (Visual Studio)
1. *Crear un proyecto* → buscar **Sitio web vacío de ASP.NET (.NET Framework)** (o *Archivo → Nuevo → Sitio web*). Lenguaje C#.
2. Clic derecho en el sitio → *Agregar → Agregar nuevo elemento* → **Servicio web (ASMX)** → nombre `Servicio`.
3. Se crean `Servicio.asmx` y `App_Code/Servicio.cs`. **Reemplaza el contenido de `App_Code/Servicio.cs`** por el de abajo.
4. `Ctrl + F5` con `Servicio.asmx` seleccionado.
5. En el navegador: la lista de operaciones. Agrega `?WSDL` a la URL para ver el contrato.
6. **Anota el puerto** de la barra de direcciones (lo necesitan los clientes). Si no está fijo: *Propiedades → Web*.

Si le pones otro nombre al servicio, cambia el nombre de la clase **y** `Class="..."` en el `.asmx`.

## Servicio.asmx
```aspx
<%@ WebService Language="C#" CodeBehind="~/App_Code/Servicio.cs" Class="Servicio" %>
```

## App_Code/Servicio.cs
```csharp
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
```

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| `bool verificar(...)` | Copia el patrón de `Verificar` |
| Devolver un objeto / una lista | `Buscar` / `Listar` (clase con propiedades `public`) |
| Devolver un error | `throw new SoapException("...", SoapException.ClientFaultCode)` (culpa del cliente) o `ServerFaultCode` (falló otro servicio) |
| Registrar, borrar, totales | [logica/csharp](../../logica/README.md#soap-más-operaciones) |
| Que el SOAP llame a REST o GraphQL | [Ejercicio 6](../../ejercicios/6-central-pagos/README.md) |
| Consumirlo | [Forms](../11-cliente-forms-csharp/README.md) · [Java, PHP, JS](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |

## Probarlo sin cliente [INGE: curl]
```bat
curl -X POST http://localhost:PUERTO/Servicio.asmx -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"http://tempuri.org/Verificar\"" -d "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><Verificar xmlns=\"http://tempuri.org/\"><codigo>A1</codigo></Verificar></soap:Body></soap:Envelope>"
```
(Pégalo en el **símbolo del sistema**, no en PowerShell. También sirve SoapUI con la URL `?WSDL`.)

## Cómo defenderlo
- "SOAP es un protocolo que intercambia **XML** dentro de un sobre (*Envelope*, *Body*)."
- "El **WSDL** es el contrato: operaciones, parámetros y tipos. De ahí el cliente genera su proxy."
- "`[WebService]` marca la clase y `[WebMethod]` cada operación publicada."
- "Hay **una sola URL por servicio**; los errores viajan como *SOAP Fault*."
- "El `static` en la lista hace que los datos sobrevivan entre peticiones (se pierden al reiniciar)."
