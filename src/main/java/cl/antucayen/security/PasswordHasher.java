package cl.antucayen.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Locale;

/** Hash de contraseñas con compatibilidad transparente con el SHA-256 legado. */
public final class PasswordHasher {

    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int ITERACIONES = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERACIONES, HASH_BITS);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificar(String password, String almacenado) {
        if (password == null || almacenado == null) return false;
        if (almacenado.startsWith(PREFIJO + "$")) return verificarPbkdf2(password, almacenado);
        return verificarSha256Legado(password, almacenado);
    }

    public static boolean esLegado(String almacenado) {
        return almacenado != null && !almacenado.startsWith(PREFIJO + "$");
    }

    private static boolean verificarPbkdf2(String password, String almacenado) {
        try {
            String[] partes = almacenado.split("\\$", -1);
            if (partes.length != 4 || !PREFIJO.equals(partes[0])) return false;
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = pbkdf2(password.toCharArray(), salt, iteraciones, esperado.length * 8);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean verificarSha256Legado(String password, String almacenado) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) hex.append(String.format(Locale.ROOT, "%02x", b));
            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.US_ASCII),
                    almacenado.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM", ex);
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iteraciones, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 no está disponible en la JVM", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
