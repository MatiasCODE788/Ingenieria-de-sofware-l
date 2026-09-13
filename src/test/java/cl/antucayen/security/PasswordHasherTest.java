package cl.antucayen.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void generaYVerificaPbkdf2() {
        String hash = PasswordHasher.hash("prueba123");
        assertTrue(hash.startsWith("pbkdf2_sha256$"));
        assertTrue(PasswordHasher.verificar("prueba123", hash));
        assertFalse(PasswordHasher.verificar("incorrecta", hash));
    }

    @Test
    void mantieneCompatibilidadConHashSha256Legado() {
        String hashAdmin123 = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        assertTrue(PasswordHasher.esLegado(hashAdmin123));
        assertTrue(PasswordHasher.verificar("admin123", hashAdmin123));
        assertFalse(PasswordHasher.verificar("otra", hashAdmin123));
    }
}
