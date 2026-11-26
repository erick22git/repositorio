import java.net.*;

// CLIENTE UDP. El metodo enviar() se puede copiar dentro de un servidor RMI u otro programa.
public class ClienteUDP {

    public static void main(String[] args) {
        String mensaje = args.length > 0 ? args[0] : "hola";
        System.out.println("Respuesta: " + enviar("localhost", 6789, mensaje));
    }

    static String enviar(String host, int puerto, String mensaje) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);   // UDP no avisa si el servidor esta apagado
            byte[] salida = mensaje.getBytes();
            socket.send(new DatagramPacket(salida, salida.length, InetAddress.getByName(host), puerto));

            byte[] bufer = new byte[1000];
            DatagramPacket respuesta = new DatagramPacket(bufer, bufer.length);
            socket.receive(respuesta);
            return new String(respuesta.getData(), 0, respuesta.getLength());

        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
