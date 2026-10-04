package cl.antucayen.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolSistemaTest {

    @Test
    void reconoceUnicamenteLosTresRolesVigentes() {
        assertEquals(RolSistema.ADMINISTRADOR, RolSistema.desdeNombre("Administrador"));
        assertEquals(RolSistema.BODEGUERO, RolSistema.desdeNombre(" Bodeguero "));
        assertEquals(RolSistema.CAJERO, RolSistema.desdeNombre("cajero"));

        assertFalse(RolSistema.esSoportado("Consultor"));
        assertFalse(RolSistema.esSoportado("Solo lectura"));
        assertNull(RolSistema.desdeNombre(null));
    }

    @Test
    void nombresPersistidosSeMantienenEstables() {
        assertEquals("Administrador", RolSistema.ADMINISTRADOR.getNombrePerfil());
        assertEquals("Bodeguero", RolSistema.BODEGUERO.getNombrePerfil());
        assertEquals("Cajero", RolSistema.CAJERO.getNombrePerfil());
        assertTrue(RolSistema.CAJERO.coincide("CAJERO"));
    }
}
