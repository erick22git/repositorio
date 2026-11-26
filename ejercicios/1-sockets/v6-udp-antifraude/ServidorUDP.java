import java.net.*;

// SERVIDOR UDP: recibe un datagrama y responde a quien lo mando.
public class ServidorUDP {

    static final int PUERTO = 6789;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            System.out.println("Servidor UDP escuchando en el puerto " + PUERTO);
            byte[] bufer = new byte[1000];
            while (true) {
                DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
                socket.receive(peticion);   // se queda esperando

                // IMPORTANTE: usar getLength(), si no llega basura del bufer
                String recibido = new String(peticion.getData(), 0, peticion.getLength());
                System.out.println("Llego: " + recibido);

                byte[] salida = procesarSolicitud(recibido).getBytes();
                socket.send(new DatagramPacket(salida, salida.length,
                        peticion.getAddress(), peticion.getPort()));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Protocolo:  1500  (monto)  ->  alto | bajo
    //             123:1500       ->  tambien vale, toma el ultimo dato
    static String procesarSolicitud(String mensaje) {
        try {
            String[] partes = mensaje.trim().split(":");
            double monto = Double.parseDouble(partes[partes.length - 1]);
            return monto > 1000 ? "alto" : "bajo";     // CAMBIAR el limite
        } catch (NumberFormatException e) {
            return "error:monto invalido";
        }
    }
}
