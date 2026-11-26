import java.io.Serializable;

// PROTOCOLO. CREAR y PUJAR son PROPUESTAS (van solo al coordinador).
// NUEVA, ACEPTADA y CIERRE son DECISIONES (van a todos). RECHAZADA vuelve al que pidio.
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    enum Tipo { CREAR, PUJAR, NUEVA, ACEPTADA, RECHAZADA, CIERRE }

    final Tipo tipo;
    final String articulo, texto;
    final double monto;
    final long dato;          // CREAR: segundos   NUEVA: instante de cierre

    Mensaje(Tipo tipo, String articulo, double monto, long dato, String texto) {
        this.tipo = tipo;
        this.articulo = articulo;
        this.monto = monto;
        this.dato = dato;
        this.texto = texto;
    }
}
