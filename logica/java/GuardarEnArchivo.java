import java.io.IOException;
import java.nio.file.*;
import java.util.List;

// [EXTRA] Persistencia sin base de datos: una linea por registro.
public class GuardarEnArchivo {

    static final Path ARCHIVO = Path.of("datos.txt");

    static synchronized void agregar(String linea) throws IOException {
        Files.writeString(ARCHIVO, linea + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    static synchronized List<String> leer() throws IOException {
        return Files.exists(ARCHIVO) ? Files.readAllLines(ARCHIVO) : List.of();
    }

    public static void main(String[] args) throws IOException {
        agregar("A1;Ejemplo;100");
        for (String l : leer()) System.out.println(l.split(";")[1]);
    }
}
