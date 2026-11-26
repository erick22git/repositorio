import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// JWT HS256 sin librerias. Compatible con JwtSimple.php y con firebase/php-jwt
// siempre que todos usen EL MISMO SECRETO.
public class JwtJava {

    static final String SECRETO = "clave_secreta_del_examen_2026_de_32_letras_o_mas";

    static String crear(String usuario) {
        long ahora = System.currentTimeMillis() / 1000;
        String h = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String p = b64(("{\"iat\":" + ahora + ",\"exp\":" + (ahora + 3600)
                + ",\"data\":{\"user\":\"" + usuario + "\"}}").getBytes(StandardCharsets.UTF_8));
        return h + "." + p + "." + firmar(h + "." + p);
    }

    // true si la firma es correcta y no vencio
    static boolean verificar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) return false;
            if (!firmar(partes[0] + "." + partes[1]).equals(partes[2])) return false;

            String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)").matcher(payload);
            return m.find() && Long.parseLong(m.group(1)) > System.currentTimeMillis() / 1000;
        } catch (Exception e) {
            return false;
        }
    }

    private static String firmar(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return b64(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }
}
