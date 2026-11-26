import java.io.Serializable;

public class Pago implements Serializable {
    private static final long serialVersionUID = 1L;

    final boolean aprobado;
    final String detalle;

    public Pago(boolean aprobado, String detalle) {
        this.aprobado = aprobado;
        this.detalle = detalle;
    }
}
