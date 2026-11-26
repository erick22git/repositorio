// Cliente 2:  REST -> registrar reservas        SOAP -> tarifa e historial
// ANTES DE PEGAR:
//   - Agregar referencia de servicio al SOAP del hotel con el nombre  srHotel
//   - Agregar referencia  System.Web.Extensions
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Text;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace WindowsFormsApp1   // <-- dejar el nombre que tenga TU proyecto
{
    public partial class Form1 : Form
    {
        const string API = "http://localhost:8000/api";

        readonly Dictionary<string, TextBox> cajas = new Dictionary<string, TextBox>();
        TextBox salida;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Recepcion";
            ClientSize = new Size(470, 440);

            string[] campos = { "fecha", "idHuesped", "codHabitacion", "hotelDestino", "noches" };
            string[] valores = { DateTime.Today.ToString("yyyy-MM-dd"), "H1", "B101", "B", "2" };
            for (int i = 0; i < campos.Length; i++)
            {
                Controls.Add(new Label { Text = campos[i], Location = new Point(10, 13 + i * 28), AutoSize = true });
                cajas[campos[i]] = new TextBox { Text = valores[i], Location = new Point(120, 10 + i * 28), Width = 220 };
                Controls.Add(cajas[campos[i]]);
            }

            Boton("Reservar", 10, () => Reservar());
            Boton("Tarifa", 130, () => Tarifa());
            Boton("Historial", 250, () => Historial());

            salida = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical,
                                   Location = new Point(10, 200), Size = new Size(450, 230) };
            Controls.Add(salida);
        }

        void Boton(string texto, int x, Func<string> accion)
        {
            var b = new Button { Text = texto, Location = new Point(x, 160), Width = 110 };
            b.Click += (s, e) =>
            {
                try { salida.Text = accion(); }
                catch (WebException ex)
                {
                    var r = ex.Response as HttpWebResponse;
                    salida.Text = r == null ? "El servidor no responde: " + ex.Message
                        : "Error " + (int)r.StatusCode + "\r\n" + new StreamReader(r.GetResponseStream()).ReadToEnd();
                }
                catch (Exception ex) { salida.Text = "Error: " + ex.Message; }
            };
            Controls.Add(b);
        }

        // ---------- REST ----------
        string Enviar(string ruta, object cuerpo, string token)
        {
            var web = new WebClient { Encoding = Encoding.UTF8 };
            web.Headers["Accept"] = "application/json";
            web.Headers["Content-Type"] = "application/json";
            if (token != null) web.Headers["Authorization"] = "Bearer " + token;
            return web.UploadString(API + ruta, "POST", new JavaScriptSerializer().Serialize(cuerpo));
        }

        string Reservar()
        {
            string login = Enviar("/login", new { email = "admin@test.com", password = "123456" }, null);
            var datos = new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(login);

            var reserva = new Dictionary<string, string>();
            foreach (var par in cajas) reserva[par.Key] = par.Value.Text.Trim();

            return Enviar("/reserva", reserva, datos["token"].ToString());
        }

        // ---------- SOAP ----------
        string Tarifa()
        {
            var hotel = new srHotel.ServicioSoapClient();
            return "Tarifa: " + hotel.consultarTarifa(cajas["codHabitacion"].Text.Trim()) + " Bs";
        }

        string Historial()
        {
            var hotel = new srHotel.ServicioSoapClient();
            var texto = new StringBuilder("Historial:\r\n");
            foreach (var m in hotel.historial(cajas["codHabitacion"].Text.Trim()))
            {
                texto.AppendLine(m.fecha + " - " + m.noches + " noche(s)");
            }
            return texto.ToString();
        }
    }
}
