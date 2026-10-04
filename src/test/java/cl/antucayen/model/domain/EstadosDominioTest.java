package cl.antucayen.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadosDominioTest {

    @Test
    void conservaExactamenteLosValoresPersistidos() {
        assertEquals("Activo", EstadoProducto.ACTIVO.valorDb());
        assertEquals("Inactivo", EstadoProducto.INACTIVO.valorDb());

        assertEquals("Pendiente", EstadoFactura.PENDIENTE.valorDb());
        assertEquals("Procesada", EstadoFactura.PROCESADA.valorDb());
        assertEquals("Observada", EstadoFactura.OBSERVADA.valorDb());

        assertEquals("Válido", EstadoItemFactura.VALIDO.valorDb());
        assertEquals("Observado", EstadoItemFactura.OBSERVADO.valorDb());
        assertEquals("No Procesado", EstadoItemFactura.NO_PROCESADO.valorDb());

        assertEquals("En curso", EstadoVenta.EN_CURSO.valorDb());
        assertEquals("Pagada", EstadoVenta.PAGADA.valorDb());
        assertEquals("Cancelada", EstadoVenta.CANCELADA.valorDb());
        assertEquals("Anulada", EstadoVenta.ANULADA.valorDb());

        assertEquals("Pendiente", EstadoAjusteInventario.PENDIENTE.valorDb());
        assertEquals("Aplicado", EstadoAjusteInventario.APLICADO.valorDb());
        assertEquals("Revertido", EstadoAjusteInventario.REVERTIDO.valorDb());
    }

    @Test
    void comparacionEsExactaYSeguraAnteNull() {
        assertTrue(EstadoFactura.PROCESADA.coincide("Procesada"));
        assertTrue(EstadoItemFactura.NO_PROCESADO.coincide("No Procesado"));
        assertFalse(EstadoProducto.ACTIVO.coincide("activo"));
        assertFalse(EstadoVenta.PAGADA.coincide(null));
    }
}
