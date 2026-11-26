// CLIENTE PAGOS: CI, nombres y apellidos -> "Obtener el Detalle de Deudas" -> elegir -> pagar
// ANTES DE PEGAR: referencia de servicio al ServidorPagos con el nombre  srPagos
using System;
using System.Drawing;
using System.Windows.Forms;

namespace WindowsFormsApp1   // <-- dejar el nombre que tenga TU proyecto
{
    public partial class Form1 : Form
    {
        TextBox txtCi, txtNombres, txtPrimer, txtSegundo;
        DataGridView tabla;
        Label lblEstado;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Cliente Pagos";
            ClientSize = new Size(620, 420);

            txtCi = Caja("CI", 10, "11021654");
            txtNombres = Caja("Nombres", 38, "Juan");
            txtPrimer = Caja("Primer apellido", 66, "Perez");
            txtSegundo = Caja("Segundo apellido", 94, "Segovia");

            var detalle = new Button { Text = "Obtener el Detalle de Deudas", Location = new Point(360, 8), Width = 240, Height = 30 };
            detalle.Click += (s, e) => Ejecutar(Consultar);
            var pagar = new Button { Text = "Pagar la deuda seleccionada", Location = new Point(360, 44), Width = 240, Height = 30 };
            pagar.Click += (s, e) => Ejecutar(Pagar);

            lblEstado = new Label { Location = new Point(360, 92), AutoSize = true, Text = "Llene sus datos" };
            tabla = new DataGridView
            {
                Location = new Point(10, 130), Size = new Size(600, 280), ReadOnly = true,
                AllowUserToAddRows = false, SelectionMode = DataGridViewSelectionMode.FullRowSelect, MultiSelect = false
            };
            Controls.AddRange(new Control[] { detalle, pagar, lblEstado, tabla });
        }

        TextBox Caja(string titulo, int y, string valor)
        {
            Controls.Add(new Label { Text = titulo, Location = new Point(10, y + 3), AutoSize = true });
            var t = new TextBox { Location = new Point(130, y), Width = 210, Text = valor };
            Controls.Add(t);
            return t;
        }

        // los SoapException del servidor llegan aqui como FaultException
        void Ejecutar(Action accion)
        {
            try { accion(); }
            catch (System.ServiceModel.FaultException ex) { lblEstado.Text = "Error"; MessageBox.Show(ex.Message, "Error"); }
            catch (Exception ex) { lblEstado.Text = "Error"; MessageBox.Show("No se pudo conectar: " + ex.Message, "Error"); }
        }

        void Consultar()
        {
            var central = new srPagos.ServicioSoapClient();
            srPagos.Deuda[] deudas = central.verDeuda(txtCi.Text.Trim(), txtPrimer.Text.Trim(), txtSegundo.Text.Trim(), txtNombres.Text.Trim());
            tabla.DataSource = deudas;
            lblEstado.Text = deudas.Length == 0 ? "No tiene deudas" : deudas.Length + " deuda(s)";
        }

        void Pagar()
        {
            if (tabla.CurrentRow == null) { MessageBox.Show("Elija una deuda"); return; }
            var deuda = (srPagos.Deuda)tabla.CurrentRow.DataBoundItem;
            var central = new srPagos.ServicioSoapClient();
            MessageBox.Show(central.PagarDeuda(deuda));
            Consultar();
        }
    }
}
