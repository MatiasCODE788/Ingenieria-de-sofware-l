package cl.antucayen.view;

import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class VVentasCarritoTest {

    @Test void quitarSeleccionadoMantieneLosDemasProductos() throws Exception {
        AtomicBoolean correcto = new AtomicBoolean(false);
        SwingUtilities.invokeAndWait(() -> {
            VVentas vista = new VVentas();
            vista.agregarFilaCarrito(new Object[]{"SKU-1", "Producto A", 3, 200, 600});
            vista.agregarFilaCarrito(new Object[]{"SKU-2", "Producto B", 1, 500, 500});
            vista.getTblCarrito().setRowSelectionInterval(0, 0);
            assertTrue(vista.quitarProductoSeleccionado());
            assertEquals(1, vista.getModeloCarrito().getRowCount());
            assertEquals("SKU-2", vista.getModeloCarrito().getValueAt(0, 0));
            assertEquals(500, vista.getModeloCarrito().getValueAt(0, 4));
            correcto.set(true);
        });
        assertTrue(correcto.get());
    }

    @Test void sinSeleccionNoEliminaProductos() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VVentas vista = new VVentas();
            vista.agregarFilaCarrito(new Object[]{"SKU-1", "Producto A", 1, 200, 200});
            assertFalse(vista.quitarProductoSeleccionado());
            assertEquals(1, vista.getModeloCarrito().getRowCount());
        });
    }
}
