// Cliente de escritorio. NO hace falta arrastrar controles: se crean por codigo.
// Referencia a agregar: System.Web.Extensions (para leer JSON)
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace WindowsFormsApp1   // <-- dejar el nombre que tenga TU proyecto
{
    public partial class Form1 : Form
    {
        // CAMBIAR por las direcciones de tus servicios
        const string REST = "http://localhost:8000/api/recursos";
        const string LOGIN = "http://localhost:8000/api/login";
        const string GRAPHQL = "http://localhost:4000/graphql";

        string token = "";
        TextBox txtCodigo, txtNombre, txtMonto, txtSalida;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Cliente";
            ClientSize = new Size(520, 420);

            txtCodigo = Caja("Codigo", 10);
            txtNombre = Caja("Nombre", 40);
            txtMonto = Caja("Monto", 70);

            Boton("Entrar", 10, (s, e) => Ejecutar(IniciarSesion));
            Boton("Consultar", 110, (s, e) => Ejecutar(Consultar));
            Boton("Registrar", 210, (s, e) => Ejecutar(Registrar));
            Boton("SOAP", 310, (s, e) => Ejecutar(LlamarSoap));
            Boton("GraphQL", 410, (s, e) => Ejecutar(LlamarGraphQL));

            txtSalida = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical,
                                      Location = new Point(10, 145), Size = new Size(500, 265) };
            Controls.Add(txtSalida);
        }

        TextBox Caja(string titulo, int y)
        {
            Controls.Add(new Label { Text = titulo, Location = new Point(10, y + 3), AutoSize = true });
            var t = new TextBox { Location = new Point(90, y), Width = 250 };
            Controls.Add(t);
            return t;
        }

        void Boton(string texto, int x, EventHandler accion)
        {
            var b = new Button { Text = texto, Location = new Point(x, 105), Width = 95 };
            b.Click += accion;
            Controls.Add(b);
        }

        // Muestra cualquier error en la caja en vez de cerrar el programa
        void Ejecutar(Func<string> accion)
        {
            try { txtSalida.Text = accion(); }
            catch (WebException ex)
            {
                var r = ex.Response as HttpWebResponse;
                txtSalida.Text = r == null ? "El servidor no respondio: " + ex.Message
                                           : "Error HTTP " + (int)r.StatusCode;
            }
            catch (Exception ex) { txtSalida.Text = "Error: " + ex.Message; }
        }

        // ---------- Utilidades HTTP ----------
        WebClient Web()
        {
            var w = new WebClient { Encoding = Encoding.UTF8 };
            w.Headers["Accept"] = "application/json";
            w.Headers["Content-Type"] = "application/json";
            if (token != "") w.Headers["Authorization"] = "Bearer " + token;
            return w;
        }

        static string Json(object o) { return new JavaScriptSerializer().Serialize(o); }

        // ---------- Acciones ----------
        string IniciarSesion()
        {
            string r = Web().UploadString(LOGIN, "POST", Json(new { email = "admin@test.com", password = "123456" }));
            var d = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(r);
            token = d["token"].ToString();
            return "Sesion iniciada";
        }

        string Consultar()
        {
            string url = txtCodigo.Text == "" ? REST : REST + "/" + txtCodigo.Text;
            return Web().DownloadString(url);
        }

        string Registrar()
        {
            // AQUI LOGICA (validar campos, consultar otro servicio antes, etc.)
            var cuerpo = new { codigo = txtCodigo.Text, nombre = txtNombre.Text, monto = double.Parse(txtMonto.Text) };
            return Web().UploadString(REST, "POST", Json(cuerpo));
        }

        string LlamarSoap()
        {
            // 1. Clic derecho en el proyecto > Agregar > Referencia de servicio > URL ?WSDL
            // 2. Quitar las // y usar el nombre que le pusiste (ej. srServicio)
            //
            // var cliente = new srServicio.ServicioSoapClient();
            // return cliente.Verificar(txtCodigo.Text).ToString();
            return "Falta agregar la referencia de servicio";
        }

        string LlamarGraphQL()
        {
            string query = "{ recursos { codigo nombre monto estado } }";
            return Web().UploadString(GRAPHQL, "POST", Json(new { query }));
        }
    }
}
