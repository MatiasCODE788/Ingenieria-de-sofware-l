package cl.antucayen.model.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioExtraccionFacturaDigitalTest {

    private final ServicioExtraccionFacturaDigital servicio =
            new ServicioExtraccionFacturaDigital();

    @Test
    void extraeCodigoCantidadYDescripcionDeTextoFactura() {
        String texto = """
                FACTURA 12345
                CODIGO DESCRIPCION CANTIDAD
                CSP-00123 Arroz Tucapel 1 kg 30 1190 35700
                BEB-77 24 Bebida Cola 1.5 L
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(2, items.size());
        assertEquals("CSP-00123", items.get(0).codigoInterno());
        assertEquals(30, items.get(0).cantidad());
        assertEquals("BEB-77", items.get(1).codigoInterno());
        assertEquals(24, items.get(1).cantidad());
        assertTrue(items.stream().allMatch(i -> "Observado".equals(i.estado())));
    }
}
