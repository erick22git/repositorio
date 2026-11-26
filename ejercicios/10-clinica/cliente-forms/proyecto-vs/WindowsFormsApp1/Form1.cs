using System;
using System.Collections;
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
        const string REST = "http://localhost:8000/api";
        const string GRAPHQL = "http://localhost:4000/graphql";

        string token = "";
        TextBox txtCi, txtNombres, txtApellidos, txtFecha, txtIdCita, txtResultado;
        ComboBox cmbEspecialidad, cmbHora;
        CheckBox chkSeguro;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Clinica - reserva de citas";
            ClientSize = new Size(620, 520);

            txtCi = Caja("CI", 10);
            txtNombres = Caja("Nombres", 40);
            txtApellidos = Caja("Apellidos", 70);
            chkSeguro = new CheckBox { Text = "Tiene seguro", Location = new Point(380, 70), AutoSize = true };
            Controls.Add(chkSeguro);

            Controls.Add(new Label { Text = "Especialidad", Location = new Point(10, 113), AutoSize = true });
            cmbEspecialidad = new ComboBox { Location = new Point(110, 110), Width = 160, DropDownStyle = ComboBoxStyle.DropDownList };
            cmbEspecialidad.Items.AddRange(new object[] { "Medicina General", "Pediatria", "Cardiologia", "Traumatologia" });
            cmbEspecialidad.SelectedIndex = 0;
            Controls.Add(cmbEspecialidad);

            txtFecha = Caja("Fecha", 140);
            txtFecha.Text = DateTime.Today.AddDays(1).ToString("yyyy-MM-dd");

            Controls.Add(new Label { Text = "Hora", Location = new Point(10, 173), AutoSize = true });
            cmbHora = new ComboBox { Location = new Point(110, 170), Width = 160, DropDownStyle = ComboBoxStyle.DropDownList };
            Controls.Add(cmbHora);

            txtIdCita = Caja("Id cita", 200);

            Boton("Iniciar sesion", 10, 235, (s, e) => Ejecutar(IniciarSesion));
            Boton("Buscar paciente", 130, 235, (s, e) => Ejecutar(BuscarPaciente));
            Boton("Registrar paciente", 250, 235, (s, e) => Ejecutar(RegistrarPaciente));
            Boton("Horarios libres", 370, 235, (s, e) => Ejecutar(HorariosLibres));
            Boton("Reservar cita", 10, 265, (s, e) => Ejecutar(Reservar));
            Boton("Citas del CI", 130, 265, (s, e) => Ejecutar(CitasDelPaciente));
            Boton("Cancelar cita", 250, 265, (s, e) => Ejecutar(Cancelar));

            txtResultado = new TextBox { Multiline = true, ScrollBars = ScrollBars.Vertical, ReadOnly = true,
                                         Location = new Point(10, 305), Size = new Size(600, 205) };
            Controls.Add(txtResultado);
        }

        TextBox Caja(string titulo, int y)
        {
            Controls.Add(new Label { Text = titulo, Location = new Point(10, y + 3), AutoSize = true });
            var t = new TextBox { Location = new Point(110, y), Width = 250 };
            Controls.Add(t);
            return t;
        }

        void Boton(string texto, int x, int y, EventHandler accion)
        {
            var b = new Button { Text = texto, Location = new Point(x, y), Width = 115 };
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

        static Dictionary<string, object> Leer(string json)
        {
            return new JavaScriptSerializer().Deserialize<Dictionary<string, object>>(json);
        }

        Dictionary<string, object> GraphQL(string query, object variables)
        {
            string r = Web().UploadString(GRAPHQL, "POST", Json(new { query, variables }));
            var d = Leer(r);
            if (d.ContainsKey("errors"))
            {
                var primero = (Dictionary<string, object>)((ArrayList)d["errors"])[0];
                throw new Exception(primero["message"].ToString());
            }
            return (Dictionary<string, object>)d["data"];
        }

        string IniciarSesion()
        {
            var d = Leer(Web().UploadString(REST + "/login", "POST", Json(new { email = "admin@test.com", password = "123456" })));
            token = d["token"].ToString();
            return "Sesion iniciada en la API de pacientes";
        }

        string BuscarPaciente()
        {
            if (token == "") return "Primero inicie sesion";
            var p = Leer(Web().DownloadString(REST + "/pacientes/" + txtCi.Text.Trim()));
            txtNombres.Text = p["nombres"].ToString();
            txtApellidos.Text = p["apellidos"].ToString();
            chkSeguro.Checked = (bool)p["seguro"];
            return "Paciente: " + p["nombres"] + " " + p["apellidos"] + Environment.NewLine
                 + "Seguro: " + ((bool)p["seguro"] ? "si (la consulta es gratis)" : "no");
        }

        string RegistrarPaciente()
        {
            if (token == "") return "Primero inicie sesion";
            if (txtCi.Text.Trim() == "" || txtNombres.Text.Trim() == "" || txtApellidos.Text.Trim() == "")
                return "CI, nombres y apellidos son obligatorios";
            var cuerpo = new { ci = txtCi.Text.Trim(), nombres = txtNombres.Text.Trim(), apellidos = txtApellidos.Text.Trim(), seguro = chkSeguro.Checked };
            Web().UploadString(REST + "/pacientes", "POST", Json(cuerpo));
            return "Paciente registrado";
        }

        string HorariosLibres()
        {
            var data = GraphQL("query($e:String!,$f:String!){ horariosLibres(especialidad:$e, fecha:$f) }",
                               new { e = cmbEspecialidad.Text, f = txtFecha.Text });
            cmbHora.Items.Clear();
            foreach (var h in (ArrayList)data["horariosLibres"]) cmbHora.Items.Add(h);
            if (cmbHora.Items.Count > 0) cmbHora.SelectedIndex = 0;
            return cmbHora.Items.Count + " horarios libres para " + cmbEspecialidad.Text + " el " + txtFecha.Text;
        }

        string Reservar()
        {
            if (cmbHora.Text == "") return "Primero presione 'Horarios libres' y elija una hora";
            var data = GraphQL(
                "mutation($ci:String!,$e:String!,$f:String!,$h:String!){ reservarCita(ci:$ci, especialidad:$e, fecha:$f, hora:$h){ id paciente costo } }",
                new { ci = txtCi.Text.Trim(), e = cmbEspecialidad.Text, f = txtFecha.Text, h = cmbHora.Text });
            var c = (Dictionary<string, object>)data["reservarCita"];
            txtIdCita.Text = c["id"].ToString();
            return "Cita " + c["id"] + " reservada para " + c["paciente"] + Environment.NewLine
                 + cmbEspecialidad.Text + " - " + txtFecha.Text + " " + cmbHora.Text + Environment.NewLine
                 + "Costo: Bs " + c["costo"];
        }

        string CitasDelPaciente()
        {
            var data = GraphQL("query($ci:String){ citas(ci:$ci){ id especialidad fecha hora estado costo } }", new { ci = txtCi.Text.Trim() });
            var sb = new StringBuilder();
            foreach (Dictionary<string, object> c in (ArrayList)data["citas"])
            {
                sb.AppendLine(c["id"] + ") " + c["fecha"] + " " + c["hora"] + "  " + c["especialidad"] + "  " + c["estado"] + "  Bs " + c["costo"]);
            }
            return sb.Length == 0 ? "El paciente no tiene citas" : sb.ToString();
        }

        string Cancelar()
        {
            int id;
            if (!int.TryParse(txtIdCita.Text, out id)) return "Escriba el id de la cita";
            var data = GraphQL("mutation($id:Int!){ cancelarCita(id:$id){ id estado } }", new { id });
            var c = (Dictionary<string, object>)data["cancelarCita"];
            return "Cita " + c["id"] + " " + c["estado"];
        }
    }
}
