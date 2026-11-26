# 12 · Cliente de escritorio en Java (Swing)

[← Plantillas](../README.md) · [Índice](../../README.md) · **[EXTRA]**

Ventana que consume REST y GraphQL. No necesita librerías ni diseñador.

## Pasos
1. Proyecto Java con `ClienteEscritorio.java`.
2. Cambia las URL y ejecuta.

## ClienteEscritorio.java
```java
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import javax.swing.*;

// Cliente de escritorio en Java (Swing). Sin librerias: HttpClient viene en el JDK.
public class ClienteEscritorio extends JFrame {

    // CAMBIAR por las direcciones de tus servicios
    static final String REST = "http://localhost:8000/api/recursos";
    static final String GRAPHQL = "http://localhost:4000/graphql";
    static String token = "";

    private final JTextField txtCodigo = new JTextField(20);
    private final JTextArea salida = new JTextArea(14, 45);

    public ClienteEscritorio() {
        super("Cliente");
        JPanel arriba = new JPanel();
        arriba.add(new JLabel("Codigo:"));
        arriba.add(txtCodigo);

        JButton rest = new JButton("REST");
        rest.addActionListener(e -> {
            // AQUI LOGICA
            String id = txtCodigo.getText().trim();
            salida.setText(llamar("GET", id.isEmpty() ? REST : REST + "/" + id, null));
        });
        arriba.add(rest);

        JButton gql = new JButton("GraphQL");
        gql.addActionListener(e -> salida.setText(
                llamar("POST", GRAPHQL, "{\"query\":\"{ recursos { codigo nombre estado } }\"}")));
        arriba.add(gql);

        salida.setLineWrap(true);
        add(arriba, BorderLayout.NORTH);
        add(new JScrollPane(salida), BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    // Sirve para REST y GraphQL. Se puede copiar a cualquier programa Java.
    static String llamar(String metodo, String url, String json) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
            if (!token.isEmpty()) b.header("Authorization", "Bearer " + token);
            b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                                          : HttpRequest.BodyPublishers.ofString(json));

            HttpResponse<String> r = HttpClient.newHttpClient()
                    .send(b.build(), HttpResponse.BodyHandlers.ofString());
            return "HTTP " + r.statusCode() + "\n" + r.body();

        } catch (IOException | InterruptedException e) {
            return "ERROR: no se pudo conectar (" + e + ")";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClienteEscritorio().setVisible(true));
    }
}
```

## Si te piden…
| Pedido | Qué hacer |
|---|---|
| Consumir SOAP desde Java | [SoapDesdeJava](../../logica/README.md#consumir-servicios-desde-cualquier-lenguaje) |
| Consumir un socket | Copia `ClienteTCP.enviar` ([plantilla 01](../01-socket-tcp-java/README.md)) |
| Consumir RMI | `LocateRegistry.getRegistry(...).lookup(...)` ([plantilla 03](../03-rmi-java/README.md)) |

## Cómo defenderlo
- "Swing arma la ventana; cada botón tiene un `ActionListener`."
- "`HttpClient` viene en Java; REST y GraphQL son peticiones HTTP."
