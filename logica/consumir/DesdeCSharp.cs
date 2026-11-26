// Fragmentos para C# (.NET Framework). Pegar dentro de un metodo de tu clase.
// using System.Net; using System.Net.Sockets; using System.IO; using System.Text;

// ---------- REST ----------
var web = new WebClient { Encoding = Encoding.UTF8 };
web.Headers["Accept"] = "application/json";
string lista = web.DownloadString("http://localhost:8000/api/recursos");

web.Headers["Content-Type"] = "application/json";
string creado = web.UploadString("http://localhost:8000/api/recursos", "POST",
    "{\"codigo\":\"X1\",\"nombre\":\"n\",\"monto\":5}");

// ---------- GraphQL ----------
web.Headers["Content-Type"] = "application/json";
string gql = web.UploadString("http://localhost:4000/graphql", "POST",
    "{\"query\":\"{ recursos { codigo nombre } }\"}");

// ---------- Socket TCP (cliente de un servidor Java) ----------
using (var cliente = new TcpClient("localhost", 5002))
using (var red = cliente.GetStream())
{
    var escribir = new StreamWriter(red) { AutoFlush = true, NewLine = "\n" };
    var leer = new StreamReader(red);
    escribir.WriteLine("hola");
    string respuesta = leer.ReadLine();
}
