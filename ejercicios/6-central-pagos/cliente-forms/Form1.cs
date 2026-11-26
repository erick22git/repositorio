// Cliente de la Central de Pagos (solo habla SOAP)
// ANTES DE PEGAR: agregar referencia de servicio con el nombre  srPagos
using System;
using System.Drawing;
using System.Windows.Forms;

namespace WindowsFormsApp1   // <-- dejar el nombre que tenga TU proyecto
{
    public partial class Form1 : Form
    {
        TextBox txtCi;
        DataGridView tabla;

        public Form1()
        {
            InitializeComponent();
            ArmarPantalla();
        }

        void ArmarPantalla()
        {
            Text = "Central de pagos";
            ClientSize = new Size(500, 320);

            txtCi = new TextBox { Text = "11021654", Location = new Point(10, 10), Width = 150 };

            var consultar = new Button { Text = "Consultar deudas", Location = new Point(170, 8), Width = 130 };
            consultar.Click += (s, e) => Ejecutar(Consultar);

            var pagar = new Button { Text = "Pagar seleccionada", Location = new Point(310, 8), Width = 140 };
            pagar.Click += (s, e) => Ejecutar(Pagar);

            tabla = new DataGridView
            {
                Location = new Point(10, 45), Size = new Size(480, 265), ReadOnly = true,
                AllowUserToAddRows = false, SelectionMode = DataGridViewSelectionMode.FullRowSelect
            };

            Controls.AddRange(new Control[] { txtCi, consultar, pagar, tabla });
        }

        // Si el servidor lanza SoapException, aqui se muestra el mensaje
        void Ejecutar(Action accion)
        {
            try { accion(); }
            catch (Exception ex) { MessageBox.Show(ex.Message); }
        }

        void Consultar()
        {
            var central = new srPagos.ServicioSoapClient();
            tabla.DataSource = central.consultarDeudas(txtCi.Text.Trim());
        }

        void Pagar()
        {
            if (tabla.CurrentRow == null) return;
            var deuda = (srPagos.Deuda)tabla.CurrentRow.DataBoundItem;

            var central = new srPagos.ServicioSoapClient();
            MessageBox.Show(central.pagar(deuda.empresa, deuda.idFactura));
            Consultar();   // refrescar la lista
        }
    }
}
