import java.io.Serializable;
import java.util.ArrayList;

public class RespuestaCuenta implements Serializable {
    private static final long serialVersionUID = 1L;

    boolean error = false;                          // true si algo fallo
    String mensaje = "ok";
    ArrayList<Cuenta> cuentas = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("error=" + error + " mensaje=" + mensaje);
        for (Cuenta c : cuentas) sb.append("\n  ").append(c);
        return sb.toString();
    }
}
