package cl.antucayen.model.service;

import cl.antucayen.model.dto.ItemFacturaExtraido;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la reconstrucción de la tabla a partir de las palabras del OCR.
 * Cada prueba arma las palabras con su posición, como las entrega Tesseract
 * para una foto, sin depender del motor OCR.
 */
class ReconstructorTablaFacturaTest {

    private static final int ALTO = 22;
    private static final int PASO = 30;

    /** Fila de una factura con columnas de impuesto, cantidad, precio y total, como la de un proveedor de cigarrillos. */
    private record Fila(String codigo, String descripcion, String impuesto, String cantidad,
                        String precio, String total) {
    }

    private static final List<Fila> FILAS = List.of(
            new Fila("10206091", "5288 Marca A 20", "5.800", "1", "45.375", "45.375"),
            new Fila("10206059", "5332 Marca B 20", "5.800", "1", "45.375", "45.375"),
            new Fila("10215542", "5318 Marca C 20", "6.100", "0,6", "47.723,33", "28.634"),
            new Fila("10234321", "6384 Marca D 20", "5.300", "3", "41.782", "125.346"),
            new Fila("10209321", "8791 Marca E 20", "4.700", "2", "37.052", "74.104"),
            new Fila("10252809", "8043 Marca F XL", "4.100", "3", "32.404", "97.212"),
            new Fila("10252861", "1258 Marca G HL", "3.000", "4", "23.710", "94.840"),
            new Fila("10242375", "5059 Marca H XL", "5.000", "2", "39.517", "79.034"));

    /**
     * Arma la tabla. Los montos se imprimen corridos hacia arriba respecto del
     * código, más mientras más a la derecha y más abajo, como en una foto
     * inclinada de una impresión desalineada.
     */
    private static List<ReconstructorTablaFactura.Palabra> tabla(List<Fila> filas, double corrimiento) {
        List<ReconstructorTablaFactura.Palabra> palabras = new ArrayList<>();
        agregar(palabras, "CODIGO", 150, 260);
        agregar(palabras, "DESCRIPCION", 500, 260);
        agregar(palabras, "CANTIDAD", 1600, 260);
        agregar(palabras, "P.UNITARIO", 1850, 260);
        agregar(palabras, "VALOR", 2150, 260);
        for (int i = 0; i < filas.size(); i++) {
            Fila f = filas.get(i);
            int y = 320 + i * PASO;
            agregar(palabras, f.codigo(), 150, y);
            int x = 300;
            for (String palabra : f.descripcion().split(" ")) {
                agregar(palabras, palabra, x, y - (x - 300) / 60);
                x += palabra.length() * 16 + 18;
            }
            int desplazamiento = (int) Math.round(corrimiento * PASO * (0.5 + 0.5 * i / (filas.size() - 1.0)));
            agregarDerecha(palabras, f.impuesto(), 1370, y - desplazamiento / 2);
            if (f.cantidad() != null) agregarDerecha(palabras, f.cantidad(), 1690, y - desplazamiento);
            if (f.precio() != null) agregarDerecha(palabras, f.precio(), 1970, y - desplazamiento);
            if (f.total() != null) agregarDerecha(palabras, f.total(), 2250, y - desplazamiento);
        }
        int pie = 320 + filas.size() * PASO + 60;
        agregar(palabras, "SON:UN", 150, pie);
        agregar(palabras, "MILLON", 300, pie);
        agregar(palabras, "TOTAL", 1800, pie + 40);
        agregarDerecha(palabras, "1.707.283", 2250, pie + 40);
        return palabras;
    }

    private static void agregar(List<ReconstructorTablaFactura.Palabra> palabras, String texto, int x, int y) {
        palabras.add(new ReconstructorTablaFactura.Palabra(texto, x, y, Math.max(8, texto.length() * 16), ALTO));
    }

    private static void agregarDerecha(List<ReconstructorTablaFactura.Palabra> palabras, String texto, int derecha, int y) {
        int ancho = Math.max(8, texto.length() * 16);
        palabras.add(new ReconstructorTablaFactura.Palabra(texto, derecha - ancho, y, ancho, ALTO));
    }

    @Test
    void leeLaTablaAunqueLosMontosEstenCorridosUnaFila() {
        var resultado = ReconstructorTablaFactura.reconstruir(tabla(FILAS, 0.8));

        assertEquals(8, resultado.items().size());
        assertTrue(resultado.confiable());
        ItemFacturaExtraido primera = resultado.items().get(0);
        assertEquals("10206091", primera.codigoInterno());
        assertEquals("5288 Marca A 20", primera.descripcion());
        assertEquals(1, primera.cantidad());
        assertEquals(45375, primera.precioUnitario());
        assertEquals(3, resultado.items().get(3).cantidad());
        assertEquals(41782, resultado.items().get(3).precioUnitario());
        assertEquals(4, resultado.items().get(6).cantidad());
        assertEquals("10242375", resultado.items().get(7).codigoInterno());
        assertEquals(2, resultado.items().get(7).cantidad());
    }

    @Test
    void laCantidadFraccionariaQuedaNoProcesadaConSuPrecio() {
        var item = ReconstructorTablaFactura.reconstruir(tabla(FILAS, 0)).items().get(2);

        assertEquals("10215542", item.codigoInterno());
        assertEquals(0, item.cantidad());
        assertEquals(47723, item.precioUnitario());
        assertEquals("No Procesado", item.estado());
        assertTrue(item.descripcion().contains("0,6"));
    }

    @Test
    void recuperaLaCantidadIlegibleDividiendoSubtotalPorPrecio() {
        List<Fila> filas = new ArrayList<>(FILAS);
        filas.set(3, new Fila("10234321", "6384 Marca D 20", "5.300", "s", "41.782", "125.346"));
        filas.set(4, new Fila("10209321", "8791 Marca E 20", "4.700", "7", "37.052", "74.104"));

        var items = ReconstructorTablaFactura.reconstruir(tabla(filas, 0.5)).items();

        assertEquals(3, items.get(3).cantidad());
        assertEquals(2, items.get(4).cantidad());
        assertEquals("Observado", items.get(4).estado());
    }

    @Test
    void ubicaElValorQueFaltaPorAritmetica() {
        List<Fila> filas = new ArrayList<>(FILAS);
        filas.set(4, new Fila("10209321", "8791 Marca E 20", "4.700", "2", "37.052", null));

        var resultado = ReconstructorTablaFactura.reconstruir(tabla(filas, 0.8));

        assertEquals(8, resultado.items().size());
        assertEquals(2, resultado.items().get(4).cantidad());
        assertEquals(37052, resultado.items().get(4).precioUnitario());
        // Las filas de más abajo no se corren por el valor que falta.
        assertEquals(4, resultado.items().get(6).cantidad());
        assertEquals(23710, resultado.items().get(6).precioUnitario());
        assertEquals(2, resultado.items().get(7).cantidad());
        assertFalse(resultado.verificadas().get(4));
        assertTrue(resultado.verificadas().get(6));
    }

    @Test
    void elTimbreYElPieNoSeLeenComoProductos() {
        var palabras = tabla(FILAS, 0.3);
        int pie = 320 + FILAS.size() * PASO + 160;
        // El timbre electrónico, leído por el OCR como letras y números sueltos.
        for (int i = 0; i < 6; i++) {
            agregar(palabras, "A7X" + i + "Q9", 150, pie + i * PASO);
            agregar(palabras, "lI|" + i, 400, pie + i * PASO);
            agregarDerecha(palabras, i + "4", 1690, pie + i * PASO);
            agregarDerecha(palabras, "3" + i + ".118", 2250, pie + i * PASO);
        }

        var resultado = ReconstructorTablaFactura.reconstruir(palabras);

        assertEquals(8, resultado.items().size());
        assertTrue(resultado.items().stream().noneMatch(i -> i.codigoInterno() != null
                && i.codigoInterno().startsWith("A7X")));
    }

    @Test
    void aceptaFacturasSinColumnaDeCantidadLegible() {
        List<Fila> filas = new ArrayList<>();
        for (Fila f : FILAS) {
            filas.add(new Fila(f.codigo(), f.descripcion(), f.impuesto(), null, f.precio(), f.total()));
        }

        var items = ReconstructorTablaFactura.reconstruir(tabla(filas, 0)).items();

        assertEquals(8, items.size());
        assertEquals(1, items.get(0).cantidad());
        assertEquals(3, items.get(3).cantidad());
        assertEquals(4, items.get(6).cantidad());
    }

    @Test
    void separaCodigoYDescripcionUnidosPorElBordeDeLaTabla() {
        List<ReconstructorTablaFactura.Palabra> palabras = new ArrayList<>();
        for (ReconstructorTablaFactura.Palabra p : tabla(FILAS, 0)) {
            if (p.texto().equals("10206059")) {
                palabras.add(new ReconstructorTablaFactura.Palabra("10206059|5332", p.x(), p.y(), 13 * 16, ALTO));
            } else if (!(p.texto().equals("5332"))) {
                palabras.add(p);
            }
        }

        var item = ReconstructorTablaFactura.reconstruir(palabras).items().get(1);

        assertEquals("10206059", item.codigoInterno());
        assertTrue(item.descripcion().startsWith("5332"));
    }

    @Test
    void interpretaLosFormatosDeMontoDeUnaFactura() {
        assertEquals(45375.0, ReconstructorTablaFactura.interpretarNumero("45.375"));
        assertEquals(47723.33, ReconstructorTablaFactura.interpretarNumero("47.723,33"));
        assertEquals(26081.67, ReconstructorTablaFactura.interpretarNumero("26.081.67"));
        assertEquals(0.6, ReconstructorTablaFactura.interpretarNumero("0,6"));
        assertEquals(118551.0, ReconstructorTablaFactura.interpretarNumero("118 551"));
        assertEquals(3300.0, ReconstructorTablaFactura.interpretarNumero("3,300"));
        assertEquals(2.0, ReconstructorTablaFactura.interpretarNumero("z"));
        assertNull(ReconstructorTablaFactura.interpretarNumero("Epa"));
    }

    @Test
    void sinTablaReconociblesDevuelveResultadoVacio() {
        List<ReconstructorTablaFactura.Palabra> palabras = new ArrayList<>();
        agregar(palabras, "Estimado", 100, 100);
        agregar(palabras, "cliente:", 300, 100);
        agregar(palabras, "usted", 100, 140);
        agregar(palabras, "dispone", 250, 140);
        agregar(palabras, "de", 400, 140);
        agregar(palabras, "libertad", 450, 140);

        var resultado = ReconstructorTablaFactura.reconstruir(palabras);

        assertTrue(resultado.items().isEmpty());
        assertFalse(resultado.confiable());
    }
}
