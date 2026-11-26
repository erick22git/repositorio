// POLICIA: 1) verifica el CI en SEGIP (SOAP)   2) registra la boleta en la Central (REST)
// ANTES DE PEGAR:
//   - Agregar referencia de servicio al SEGIP con el nombre  srSegip
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
        const string REST = "http://localhost:8000/api/multas";

        readonly Dictionary<string, TextBox> cajas = new Dictionary<string, TextBox>();
        TextBox salida;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Policia - Registro de multas";
            ClientSize = new Size(460, 430);

            string[] campos = { "codigo", "ci", "nombre", "fecha", "tipo", "lugar", "monto" };
            int y = 10;
            foreach (string c in campos)
            {
                Controls.Add(new Label { Text = c, Location = new Point(10, y + 3), AutoSize = true });
                cajas[c] = new TextBox { Location = new Point(90, y), Width = 250 };
                Controls.Add(cajas[c]);
                y += 28;
            }
            cajas["fecha"].Text = DateTime.Today.ToString("yyyy-MM-dd");

            var boton = new Button { Text = "Registrar multa", Location = new Point(90, y), Width = 150 };
            boton.Click += Registrar;
            Controls.Add(boton);

            salida = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical,
                                   Location = new Point(10, y + 40), Size = new Size(440, 170) };
            Controls.Add(salida);
        }

        void Registrar(object sender, EventArgs e)
        {
            try
            {
                // 1) SOAP: el CI existe?
                var segip = new srSegip.ServicioSoapClient();
                if (!segip.verificarCI(cajas["ci"].Text.Trim()))
                {
                    salida.Text = "SEGIP: el CI no existe. No se registra.";
                    return;
                }

                // 2) REST: registrar la boleta
                var datos = new Dictionary<string, string>();
                foreach (var par in cajas) datos[par.Key] = par.Value.Text.Trim();

                var web = new WebClient { Encoding = Encoding.UTF8 };
                web.Headers["Accept"] = "application/json";
                web.Headers["Content-Type"] = "application/json";
                salida.Text = "Registrada:\r\n" +
                    web.UploadString(REST, "POST", new JavaScriptSerializer().Serialize(datos));
            }
            catch (WebException ex)
            {
                // 409 = codigo repetido, 422 = datos mal llenados
                var r = ex.Response as HttpWebResponse;
                salida.Text = r == null ? "La Central no responde: " + ex.Message
                    : "Error " + (int)r.StatusCode + "\r\n" + new StreamReader(r.GetResponseStream()).ReadToEnd();
            }
            catch (Exception ex)
            {
                salida.Text = "Error: " + ex.Message;
            }
        }
    }
}
