package cl.antucayen.view;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VVentasPagoTest {
    @Test void importaPesosCorrectamente() {
        assertEquals(0, VVentas.parseMonto(""));
        assertEquals(0, VVentas.parseMonto("  "));
        assertEquals(1500, VVentas.parseMonto("1500"));
        assertEquals(1500, VVentas.parseMonto("$ 1.500"));
        assertEquals(1500, VVentas.parseMonto("1.500"));
    }

    @Test void noConvierteNegativosNiTextosInvalidosEnPagosPositivos() {
        for (String invalido : new String[] {"-1500", "-$1.500", "hola1500",
                "1,500", "12.34", "1 500", "+100", "2147483648"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> VVentas.parseMonto(invalido), "Entrada: " + invalido);
        }
    }
}
