package cl.antucayen.model.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraImpuestosTest {
    @Test void ivaIncluidoConMontoExacto() {
        var d = CalculadoraImpuestos.desglosar(1190);
        assertEquals(1000, d.neto());
        assertEquals(190, d.iva());
        assertEquals(1190, d.total());
    }

    @Test void precioFinalNuncaCambiaPorImpuestos() {
        for (int total = 0; total < 100_000; total++) {
            var d = CalculadoraImpuestos.desglosar(total);
            assertEquals(total, d.total());
            assertEquals(total, d.neto() + d.iva());
            assertTrue(d.neto() >= 0);
            assertTrue(d.iva() >= 0);
        }
    }

    @Test void noDesbordaConMontosGrandes() {
        var d = CalculadoraImpuestos.desglosar(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, d.neto() + d.iva());
    }

    @Test void rechazaTotalNegativo() {
        assertThrows(IllegalArgumentException.class, () -> CalculadoraImpuestos.desglosar(-1));
    }
}
