import java.io.Serializable;

// Objeto que viaja por la red: por eso implementa Serializable.
public class Respuesta implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean ok;
    private final String mensaje;

    public Respuesta(boolean ok, String mensaje) {
        this.ok = ok;
        this.mensaje = mensaje;
    }

    public boolean isOk() { return ok; }
    public String getMensaje() { return mensaje; }

    @Override
    public String toString() {
        return "Respuesta{ok=" + ok + ", mensaje=" + mensaje + "}";
    }
}
