import java.io.Serializable;
// mensaje que viaja dentro de un ObjectMessage de JGroups
public class MensajeAforo implements Serializable {
    private TipoMensaje tipo;
    private String puerta;
    private int personas;
    public MensajeAforo(TipoMensaje tipo, String puerta, int personas) {
        this.tipo = tipo;
        this.puerta = puerta;
        this.personas = personas;
    }
    public TipoMensaje getTipo() {
        return tipo;
    }
    public String getPuerta() {
        return puerta;
    }
    public int getPersonas() {
        return personas;
    }
    @Override
    public String toString() {
        return tipo + " " + puerta + " " + personas;
    }
}
