package cl.antucayen.model.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void leeFacturaConColumnaUnidadYCodigoConSufijo() {
        // Texto OCR real de una factura escaneada con columnas CODIGO, DETALLE,
        // CANTIDAD, UNIDAD, VALOR UNITARIO, DESCUENTO y VALOR.
        String texto = """
                COMERCIALIZADORA Y DISTRIBUIDORA JAMA SPA
                R.UT.: 76.361.671 - 5
                FACTURA ELECTRONICA
                N? 1316157
                Fecha emisión: 09 de OCTUBRE de 2026 Ruta: 1856 Patente:
                RUT: 76.384.579 - 1 Condición de pago: CONTADO Efectivo
                Fono: Pague hasta: 09-10-2026
                CODIGO DETALLE V.NTIDAD | UNIDAD | VALOR UNITARIO | DESCUENTO VALOR
                3587 1 PK 3 Estuches Capuccino 15% (B2B) 1 PACK 7.179 1.077 6.102
                4669 24 PK 6 Picada Carne Verde 3x62 grs 5% DCTO B2B ay PACK 7.174 1.794 5.380
                2086 0 Chocolate SAHNE NUSS Barra 8x30x3g) il DSPL 17.113 17.113
                2120 Café NESCAFE Tradición tarro 24 x 170 GRS i TARR 6.741 19.223
                549 0 MAGGI Sopa Pollo Arroz Sobre 20x70g CL: 1 SOBR 502 2.510
                888 0 MOROCHA Family Galleta 22x120g CL Ñ POTE 688 3.715
                Fecha Vencimiento; 09 de OCTUBRE de 2026
                NETO 124.257
                1.V.A, (19%) 23.609
                TOTAL 147.866
                """;

        var resultado = servicio.analizarTexto(texto);

        assertEquals("1316157", resultado.numeroFactura());
        assertEquals(LocalDate.of(2026, 10, 9), resultado.fechaEmision());
        assertEquals(Integer.valueOf(147866), resultado.valorTotal());
        var items = resultado.items();
        assertEquals(6, items.size());
        assertEquals("35871", items.get(0).codigoInterno());
        assertEquals("PK 3 Estuches Capuccino 15% (B2B)", items.get(0).descripcion());
        assertEquals(1, items.get(0).cantidad());
        assertEquals(7179, items.get(0).precioUnitario());
        assertEquals("466924", items.get(1).codigoInterno());
        assertEquals("20860", items.get(2).codigoInterno());
        assertEquals("Chocolate SAHNE NUSS Barra 8x30x3g)", items.get(2).descripcion());
        assertEquals(17113, items.get(2).precioUnitario());
        assertEquals("2120", items.get(3).codigoInterno());
        assertEquals("5490", items.get(4).codigoInterno());
        assertEquals("MAGGI Sopa Pollo Arroz Sobre 20x70g CL", items.get(4).descripcion());
        assertEquals("8880", items.get(5).codigoInterno());
        assertTrue(items.stream().noneMatch(i -> i.codigoInterno().toUpperCase().contains("VENC")
                || i.codigoInterno().equalsIgnoreCase("Fecha")));
    }

    @Test
    void unePrefijoDeCodigoAunqueLaCantidadSeaIlegible() {
        String texto = """
                CODIGO DETALLE CANTIDAD UNIDAD VALOR UNITARIO DESCUENTO VALOR
                888 0 MOROCHA Family Galleta 22x120g CL a POTE 688 3.715
                888 0 MOROCHA Family Galleta 22x120g CL ‘J POTE 688 3.715
                888 0 MOROCHA Family Galleta 22x120g CL fi POTE 688 3.715
                12345 3 Arroz grado uno UN 1.000 3.000
                NETO 10.000
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(2, items.size());
        assertEquals("8880", items.get(0).codigoInterno());
        assertEquals("MOROCHA Family Galleta 22x120g CL", items.get(0).descripcion());
        assertEquals(1, items.get(0).cantidad());
        assertEquals(688, items.get(0).precioUnitario());
        assertEquals("12345", items.get(1).codigoInterno());
        assertEquals(3, items.get(1).cantidad());
        assertEquals("Arroz grado uno", items.get(1).descripcion());
        assertEquals(1000, items.get(1).precioUnitario());
    }

    @Test
    void usaEspanolCuandoLaCarpetaTessdataLoIncluye() throws Exception {
        Path carpeta = Files.createTempDirectory("tessdata");
        try {
            assertEquals("eng", ServicioExtraccionFacturaDigital.idiomaPorDefecto(carpeta));
            Files.writeString(carpeta.resolve("spa.traineddata"), "modelo");
            assertEquals("spa", ServicioExtraccionFacturaDigital.idiomaPorDefecto(carpeta));
            assertEquals("eng", ServicioExtraccionFacturaDigital.idiomaPorDefecto(null));
        } finally {
            Files.deleteIfExists(carpeta.resolve("spa.traineddata"));
            Files.deleteIfExists(carpeta);
        }
    }

    @Test
    void eligeLaCarpetaTessdataConfiguradaOLaPrimeraExistente() throws Exception {
        Path existente = Files.createTempDirectory("tessdata");
        Path inexistente = existente.resolve("no-existe");
        try {
            // La variable de entorno y la propiedad tienen prioridad, en ese orden.
            assertEquals(inexistente.toAbsolutePath().normalize(),
                    ServicioExtraccionFacturaDigital.elegirCarpetaTessdata(
                            inexistente.toString(), existente.toString(), List.of(existente)));
            assertEquals(existente.toAbsolutePath().normalize(),
                    ServicioExtraccionFacturaDigital.elegirCarpetaTessdata(
                            " ", existente.toString(), List.of()));
            // Sin configuración se usa la primera ubicación automática que exista.
            assertEquals(existente.toAbsolutePath().normalize(),
                    ServicioExtraccionFacturaDigital.elegirCarpetaTessdata(
                            null, null, List.of(inexistente, existente)));
            assertNull(ServicioExtraccionFacturaDigital.elegirCarpetaTessdata(
                    null, "", List.of(inexistente)));
        } finally {
            Files.deleteIfExists(existente);
        }
    }


    @Test
    void elFolioDeLaOrdenDeCompraNoEsElNumeroDeFactura() {
        String texto = """
                BAT Chile S.A. FACTURA ELECTRONICA
                Giro: Venta al por mayor NRO: 17464361
                DOCUMENTO DE REFERENCIA FOLIO REFERENCIA FECHA REFERENCIA
                ORDEN DE COMPRA 2610201014409734 20-10-2026
                """;

        assertEquals("17464361", servicio.extraerNumeroFactura(texto));
    }

    @Test
    void leeLaFechaDeEmisionAunqueNoInicieLaLinea() {
        String texto = """
                SEÑOR(ES): COMERCIALIZADORA EJEMPLO LIMITADA FECHA EMISION: 21-10-2026
                DIRECCION: AV CENTRAL 273 FECHA VENCIMIENTO: 28-10-2026
                """;

        assertEquals(LocalDate.of(2026, 10, 21), servicio.extraerFechaEmision(texto));
    }

    @Test
    void laFechaDeVencimientoNoSeTomaComoEmision() {
        String texto = """
                FECHA EMISION:
                FECHA VENCIMIENTO: 28-10-2026
                """;

        assertNull(servicio.extraerFechaEmision(texto));
    }

    @Test
    void elTotalDeUnidadesNoEsElTotalDeLaFactura() {
        String texto = """
                TOTAL UNIDADES 37.4 VALOR NETO 1.419.504
                I.V.A.(19 %) 269.707
                TOTAL 1.707.283
                """;

        assertEquals(1707283, servicio.extraerValorTotal(texto));
        assertEquals(1419504, servicio.extraerValorNeto(texto));
    }

    @Test
    void unaCantidadFraccionariaQuedaNoProcesadaConSuPrecio() {
        String texto = """
                CODIGO DESCRIPCION CANTIDAD P.UNITARIO VALOR A PAGAR
                10215542 5318 Kent Boost Up 20s 6.100 0,6 47.723,33 28.634
                10206091 5288 Kent White UP 20 LEP 5.800 1 45.375 45.375
                SON: UN MILLON
                """;

        var items = servicio.parsearLineas(texto);

        assertEquals(2, items.size());
        assertEquals("No Procesado", items.get(0).estado());
        assertEquals(0, items.get(0).cantidad());
        assertEquals(47723, items.get(0).precioUnitario());
        assertTrue(items.get(0).descripcion().contains("0,6"));
        assertEquals(1, items.get(1).cantidad());
        assertEquals(45375, items.get(1).precioUnitario());
    }

    @Test
    void enUnaFotoSinCabeceraElTimbreNoSeConvierteEnProducto() {
        String texto = """
                PALIO AT IA eL DA e e a somtemanto de Ibeas pora vender
                AE7 te ASA ad $ 4 An, e mo encontrandose facultado 20
                MATA de AVABY A SA if Para 972800 36 44 44
                10206091 5288 Kent White UP 20 LEP 5.800 1 45.375 45.375
                """;

        var items = servicio.parsearLineas(texto, true);

        assertEquals(1, items.size());
        assertEquals("10206091", items.get(0).codigoInterno());
    }

    @Test
    void marcaParaRevisarLasFilasQueNoCuadranSalvoQueLaSumaCoincidaConElNeto() {
        var tabla = new ReconstructorTablaFactura.Resultado(List.of(
                new cl.antucayen.model.dto.ItemFacturaExtraido("A-1", "Producto uno", 2, 100, "Observado"),
                new cl.antucayen.model.dto.ItemFacturaExtraido("B-2", "Producto dos", 7, 50, "Observado")),
                List.of(true, false), 1, 550);

        var conNetoDistinto = ServicioExtraccionFacturaDigital.marcarPorRevisar(tabla, 999);
        var conNetoIgual = ServicioExtraccionFacturaDigital.marcarPorRevisar(tabla, 550);

        assertEquals("Producto uno", conNetoDistinto.get(0).descripcion());
        assertTrue(conNetoDistinto.get(1).descripcion().contains("revisar"));
        assertEquals("Producto dos", conNetoIgual.get(1).descripcion());
    }

    @Test
    void armaLineasDeTextoConLasPalabrasDelOcr() {
        var palabras = List.of(
                new ReconstructorTablaFactura.Palabra("21-10-2026", 900, 102, 160, 22),
                new ReconstructorTablaFactura.Palabra("FECHA", 500, 100, 90, 22),
                new ReconstructorTablaFactura.Palabra("EMISION:", 600, 101, 130, 22),
                new ReconstructorTablaFactura.Palabra("TOTAL", 500, 300, 90, 22),
                new ReconstructorTablaFactura.Palabra("67.687", 900, 303, 100, 22));

        String texto = ServicioExtraccionFacturaDigital.lineasDesdePalabras(palabras);

        assertEquals("FECHA EMISION: 21-10-2026\nTOTAL 67.687\n", texto);
    }

}
