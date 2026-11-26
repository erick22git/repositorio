import java.io.Serializable;

public class Voucher implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean ok;
    private final String mensaje;
    private final double totalUSD;

    public Voucher(boolean ok, String mensaje, double totalUSD) {
        this.ok = ok;
        this.mensaje = mensaje;
        this.totalUSD = totalUSD;
    }

    @Override
    public String toString() {
        return "Voucher{ok=" + ok + ", mensaje=" + mensaje + ", totalUSD=" + totalUSD + "}";
    }
}
