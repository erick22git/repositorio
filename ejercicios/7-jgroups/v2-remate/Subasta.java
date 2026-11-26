import java.io.Serializable;
import java.util.ArrayList;

public class Subasta implements Serializable {
    private static final long serialVersionUID = 1L;

    final String articulo;
    final long cierraEn;                // instante absoluto (sobrevive a la transferencia de estado)
    double mejor;
    String postor = "nadie";
    boolean abierta = true;
    final ArrayList<String> historial = new ArrayList<>();

    Subasta(String articulo, double base, long cierraEn) {
        this.articulo = articulo;
        this.mejor = base;
        this.cierraEn = cierraEn;
    }

    @Override
    public String toString() {
        long quedan = Math.max(0, (cierraEn - System.currentTimeMillis()) / 1000);
        return articulo + " | mejor " + mejor + " (" + postor + ") | "
                + (abierta ? "quedan " + quedan + " s" : "CERRADA");
    }
}
