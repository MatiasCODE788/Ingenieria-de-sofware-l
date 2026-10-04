package cl.antucayen.model.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioExtraccionFacturaDigitalTest {

    private final ServicioExtraccionFacturaDigital servicio =
            new ServicioExtraccionFacturaDigital();

    @Test
    void extraeCodigoCantidadDescripcionYPrecioDeTextoFactura() {
        String texto = """
                FACTURA ELECTRÓNICA N° 12345
                CODIGO DESCRIPCION CANTIDAD P.UNIT TOTAL
                CSP-00123 Arroz Tucapel 1 kg 30 1.190 35.700
                BEB-77 24 Bebida Cola 1.5 L
                MONTO TOTAL $ 35.700
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(2, items.size());
        assertEquals("CSP-00123", items.get(0).codigoInterno());
        assertEquals(30, items.get(0).cantidad());
        assertEquals(1190, items.get(0).precioUnitario());
        assertEquals("Arroz Tucapel 1 kg", items.get(0).descripcion());
        assertEquals("BEB-77", items.get(1).codigoInterno());
        assertEquals(24, items.get(1).cantidad());
        assertTrue(items.stream().allMatch(i -> "Observado".equals(i.estado())));
    }

    @Test
    void conservaItemIlegibleComoNoProcesado() {
        String texto = """
                CODIGO DESCRIPCION CANTIDAD
                ABC-99 Producto con cantidad ilegible XX
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(1, items.size());
        assertEquals("ABC-99", items.get(0).codigoInterno());
        assertEquals(0, items.get(0).cantidad());
        assertEquals("No Procesado", items.get(0).estado());
    }

    @Test
    void extraeFolioYTotalDeFactura() {
        String texto = """
                COMERCIAL SAN PEDRO LTDA.
                RUT 76.123.456-7
                FACTURA ELECTRÓNICA N° 0009876
                FECHA EMISION 01-10-2026
                CODIGO DESCRIPCION CANTIDAD P.UNIT TOTAL
                CSP-00123 Arroz Tucapel 1 kg 10 1.190 11.900
                NETO 10.000
                IVA 1.900
                TOTAL A PAGAR $11.900
                """;

        var resultado = servicio.analizarTexto(texto);

        assertEquals("0009876", resultado.numeroFactura());
        assertEquals(LocalDate.of(2026, 10, 1), resultado.fechaEmision());
        assertEquals(Integer.valueOf(11900), resultado.valorTotal());
        assertNotNull(resultado.items());
        assertEquals(1, resultado.items().size());
    }

    @Test
    void ignoraMetadatosComoSiFueranItems() {
        String texto = """
                FACTURA 4567
                RUT 76.123.456-7
                FECHA 01-10-2026
                TOTAL $25.000
                ABC-100 Producto de prueba 2 12.500 25.000
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(1, items.size());
        assertEquals("ABC-100", items.get(0).codigoInterno());
        assertEquals(2, items.get(0).cantidad());
        assertEquals(12500, items.get(0).precioUnitario());
    }
    @Test
    void noInterpretaRutRazonSocialNiTotalesComoItems() {
        String texto = """
                COMERCIAL SAN PEDRO LTDA.
                76.123.456-7 Comercial San Pedro Ltda.
                RUT 76.123.456-7
                DIRECCION AV. CENTRAL 1234
                FACTURA ELECTRÓNICA N° 0009876
                CODIGO DESCRIPCION CANTIDAD P.UNIT TOTAL
                CSP-00123 Arroz Tucapel 1 kg 10 1.190 11.900
                BEB-77 24 Bebida Cola 1.5 L
                NETO 10.000
                IVA 1.900
                TOTAL A PAGAR $11.900
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(2, items.size());
        assertEquals("CSP-00123", items.get(0).codigoInterno());
        assertEquals("BEB-77", items.get(1).codigoInterno());
        assertTrue(items.stream().noneMatch(i ->
                i.codigoInterno() != null && i.codigoInterno().contains("76.123.456")));
        assertTrue(items.stream().noneMatch(i ->
                i.descripcion() != null && i.descripcion().toUpperCase().contains("COMERCIAL SAN PEDRO")));
    }

    @Test
    void sinCabeceraExigeEstructuraFuerteYDescartaMetadatos() {
        String texto = """
                COMERCIAL SAN PEDRO LTDA 1234
                76123456-7 COMERCIAL SAN PEDRO LTDA 9999
                TOTAL 25.000
                ABC-100 Producto de prueba 2 12.500 25.000
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(1, items.size());
        assertEquals("ABC-100", items.get(0).codigoInterno());
        assertEquals(2, items.get(0).cantidad());
        assertEquals(12500, items.get(0).precioUnitario());
    }

    @Test
    void ignoraNotaSobreCodigosInternosDentroDelDetalle() {
        String texto = """
                FACTURA ELECTRÓNICA N° 0009876
                CODIGO DESCRIPCION CANTIDAD P.UNIT TOTAL
                CSP-00123 Arroz Tucapel 1 kg 10 1.190 11.900
                Códigos internos compatibles con las equivalencias precargadas del proveedor demo.
                NETO 10.000
                IVA 1.900
                TOTAL A PAGAR $11.900
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(1, items.size());
        assertEquals("CSP-00123", items.get(0).codigoInterno());
        assertTrue(items.stream().noneMatch(i ->
                i.descripcion() != null
                        && i.descripcion().toUpperCase().contains("EQUIVALENCIAS PRECARGADAS")));
    }

    @Test
    void priorizaFechaEmisionYNoConfundeVencimiento() {
        String texto = """
                FACTURA ELECTRÓNICA N° 98765
                FECHA DE EMISION: 03/10/2026
                FECHA VENCIMIENTO: 02/11/2026
                CODIGO DESCRIPCION CANTIDAD P.UNIT TOTAL
                ABC-100 Producto demo 2 1.000 2.000
                TOTAL A PAGAR $2.000
                """;

        var resultado = servicio.analizarTexto(texto);

        assertEquals(LocalDate.of(2026, 10, 3), resultado.fechaEmision());
    }

    @Test
    void aceptaFechaIsoYFechaGenericaComoFallback() {
        String texto = """
                FACTURA 999
                FECHA: 2026-10-03
                CODIGO DESCRIPCION CANTIDAD
                ABC-100 Producto demo 1
                """;

        var resultado = servicio.analizarTexto(texto);

        assertEquals(LocalDate.of(2026, 10, 3), resultado.fechaEmision());
    }

}
