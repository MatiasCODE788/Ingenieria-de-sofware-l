package cl.antucayen.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlInactividadSesionTest {
    @Test void limiteExactoCuentaComoSesionExpirada() {
        long minuto = 60_000_000_000L;
        assertFalse(ControlInactividadSesion.vencio(1_000L, minuto, minuto));
        assertTrue(ControlInactividadSesion.vencio(1_000L, minuto + 1_000L, minuto));
    }

    @Test void duracionPredeterminadaYConfigurable() {
        String llave = "antucayen.sesion.inactividad.minutos";
        String previo = System.getProperty(llave);
        try {
            System.clearProperty(llave);
            assertEquals(30, ControlInactividadSesion.minutosConfigurados());
            System.setProperty(llave, "10");
            assertEquals(10, ControlInactividadSesion.minutosConfigurados());
            System.setProperty(llave, "0");
            assertEquals(30, ControlInactividadSesion.minutosConfigurados());
        } finally {
            if (previo == null) System.clearProperty(llave);
            else System.setProperty(llave, previo);
        }
    }
}
