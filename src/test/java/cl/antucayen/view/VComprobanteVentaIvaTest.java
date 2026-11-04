package cl.antucayen.view;

import cl.antucayen.model.entity.ComprobanteVenta;
import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.model.entity.Venta;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VComprobanteVentaIvaTest {
    private ComprobanteVenta ejemplo(int total) {
        Venta venta = new Venta();
        venta.setIdVenta(42);
        venta.setMontoTotal(total);
        venta.setEstado("PAGADA");
        venta.setNombreUsuario("Cajero");
        return new ComprobanteVenta(venta, List.of(), List.of(new PagoVenta("Efectivo", total)), total, 0);
    }

    @Test void reflejaNetoIvaYTotalEnVistaPrevia() {
        String resumen = VComprobanteVenta.construirResumen(ejemplo(11900));
        assertTrue(resumen.contains("MONTO NETO : $10.000"));
        assertTrue(resumen.contains("IVA (19%)  : $1.900"));
        assertTrue(resumen.contains("TOTAL      : $11.900"));
    }

    @Test void imprimeLosMismosMontosQueMuestraEnPantalla() {
        ComprobanteVenta venta = ejemplo(12345);
        String resumen = VComprobanteVenta.construirResumen(venta);
        String impresion = VComprobanteVenta.construirTextoImpresion(venta);
        assertTrue(impresion.contains(resumen));
        assertTrue(impresion.contains("MONTO NETO : $10.374"));
        assertTrue(impresion.contains("IVA (19%)  : $1.971"));
        assertTrue(impresion.contains("TOTAL      : $12.345"));
    }

    @Test void ceroNoGeneraImpuestoNiAlteraTotal() {
        String impresion = VComprobanteVenta.construirTextoImpresion(ejemplo(0));
        assertTrue(impresion.contains("MONTO NETO : $0"));
        assertTrue(impresion.contains("IVA (19%)  : $0"));
        assertTrue(impresion.contains("TOTAL      : $0"));
    }
}
