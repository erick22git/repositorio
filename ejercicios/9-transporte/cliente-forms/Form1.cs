using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace WindowsFormsApp1
{
    public partial class Form1 : Form
    {
        const string API = "http://localhost:8000/api";

        string token = "";
        TextBox txtEmail, txtPassword, txtSalida, txtAsiento, txtCi, txtResultado;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Terminal de buses - venta de pasajes";
            ClientSize = new Size(560, 470);

            txtEmail = Caja("Email", 10, "admin@test.com");
            txtPassword = Caja("Password", 40, "123456");
            txtPassword.PasswordChar = '*';
            txtSalida = Caja("Salida (id)", 80, "1");
            txtAsiento = Caja("Nro asiento", 110, "");
            txtCi = Caja("CI pasajero", 140, "");

            Boton("Iniciar sesion", 10, 175, (s, e) => Ejecutar(IniciarSesion));
            Boton("Ver salidas", 140, 175, (s, e) => Ejecutar(VerSalidas));
            Boton("Ver salida", 270, 175, (s, e) => Ejecutar(VerSalida));
            Boton("Vender asiento", 400, 175, (s, e) => Ejecutar(Vender));

            txtResultado = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical, ReadOnly = true,
                                         Location = new Point(10, 215), Size = new Size(540, 245) };
            Controls.Add(txtResultado);
        }

        TextBox Caja(string titulo, int y, string valor)
        {
            Controls.Add(new Label { Text = titulo, Location = new Point(10, y + 3), AutoSize = true });
            var t = new TextBox { Location = new Point(110, y), Width = 250, Text = valor };
            Controls.Add(t);
            return t;
        }

        void Boton(string texto, int x, int y, EventHandler accion)
        {
            var b = new Button { Text = texto, Location = new Point(x, y), Width = 120 };
            b.Click += accion;
            Controls.Add(b);
        }

        void Ejecutar(Func<string> accion)
        {
            try { txtResultado.Text = accion(); }
            catch (WebException ex)
            {
                var r = ex.Response as HttpWebResponse;
                if (r == null) { txtResultado.Text = "El servidor no respondio: " + ex.Message; return; }
                string cuerpo = new StreamReader(r.GetResponseStream()).ReadToEnd();
                txtResultado.Text = "Error " + (int)r.StatusCode + Environment.NewLine + Mensaje(cuerpo);
            }
            catch (Exception ex) { txtResultado.Text = "Error: " + ex.Message; }
        }

        static string Mensaje(string json)
        {
            try
            {
                var d = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(json);
                if (d.ContainsKey("mensaje")) return d["mensaje"].ToString();
                if (d.ContainsKey("message")) return d["message"].ToString();
            }
            catch { }
            return json;
        }

        WebClient Web()
        {
            var w = new WebClient { Encoding = Encoding.UTF8 };
            w.Headers["Accept"] = "application/json";
            w.Headers["Content-Type"] = "application/json";
            if (token != "") w.Headers["Authorization"] = "Bearer " + token;
            return w;
        }

        static string Json(object o) { return new JavaScriptSerializer().Serialize(o); }

        string IniciarSesion()
        {
            string r = Web().UploadString(API + "/login", "POST", Json(new { email = txtEmail.Text, password = txtPassword.Text }));
            var d = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(r);
            token = d["token"].ToString();
            return "Sesion iniciada. Token:" + Environment.NewLine + token;
        }

        string VerSalidas()
        {
            var lista = new JavaScriptSerializer().Deserialize<List<Dictionary<string, object>>>(Web().DownloadString(API + "/salidas"));
            var sb = new StringBuilder();
            foreach (var s in lista)
            {
                sb.AppendLine(s["id"] + ") " + s["destino"] + "  " + s["fecha"] + " " + s["hora"] + "  bus " + s["bus"]
                              + "  Bs " + s["precio"] + "  vendidos " + s["asientos_count"] + "/" + s["capacidad"]);
            }
            return sb.Length == 0 ? "No hay salidas" : sb.ToString();
        }

        string VerSalida()
        {
            var s = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(Web().DownloadString(API + "/salidas/" + txtSalida.Text));
            var sb = new StringBuilder();
            sb.AppendLine("Destino: " + s["destino"] + "   Fecha: " + s["fecha"] + " " + s["hora"]);
            sb.AppendLine("Bus: " + s["bus"] + "   Precio: Bs " + s["precio"]);
            sb.AppendLine("Disponibles: " + s["disponibles"] + " de " + s["capacidad"]);
            sb.AppendLine("Asientos vendidos:");
            foreach (Dictionary<string, object> a in (System.Collections.ArrayList)s["asientos"])
            {
                sb.AppendLine("  Nro " + a["numero_asiento"] + " - CI " + a["ci_pasajero"]);
            }
            return sb.ToString();
        }

        string Vender()
        {
            if (token == "") return "Primero inicie sesion";
            int nro;
            if (!int.TryParse(txtAsiento.Text, out nro)) return "El numero de asiento debe ser un numero";
            if (txtCi.Text.Trim() == "") return "Ingrese el CI del pasajero";

            var cuerpo = new { salida_id = int.Parse(txtSalida.Text), numero_asiento = nro, ci_pasajero = txtCi.Text.Trim() };
            Web().UploadString(API + "/asientos", "POST", Json(cuerpo));
            return "Asiento " + nro + " vendido al CI " + txtCi.Text + Environment.NewLine + Environment.NewLine + VerSalida();
        }
    }
}
