import java.io.Serializable;

public class Cuenta implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Banco banco;
    private final String nrocuenta, ci, nombres, apellidos;
    private final double saldo;

    public Cuenta(Banco banco, String nrocuenta, String ci, String nombres, String apellidos, double saldo) {
        this.banco = banco;
        this.nrocuenta = nrocuenta;
        this.ci = ci;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.saldo = saldo;
    }

    public Banco getBanco() { return banco; }
    public String getNrocuenta() { return nrocuenta; }
    public double getSaldo() { return saldo; }

    @Override
    public String toString() {
        return banco + " | cuenta " + nrocuenta + " | " + nombres + " " + apellidos + " (CI " + ci + ") | saldo " + saldo;
    }
}
