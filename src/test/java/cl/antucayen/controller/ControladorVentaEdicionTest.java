package cl.antucayen.controller;

import org.junit.jupiter.api.Test;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import static org.junit.jupiter.api.Assertions.*;

class ControladorVentaEdicionTest {
    @Test void cobraLaCantidadRealmenteEditada() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel modelo = new DefaultTableModel(
                    new Object[][] {{"SKU-01", "Arroz", 1, 500, 500}},
                    new String[] {"SKU", "Nombre", "Cantidad", "Precio", "Subtotal"}) {
                @Override public Class<?> getColumnClass(int columna) {
                    return columna == 2 ? Integer.class : Object.class;
                }
            };
            JTable tabla = new JTable(modelo);
            assertTrue(tabla.editCellAt(0, 2));
            ((JTextField) tabla.getEditorComponent()).setText("3");
            assertEquals(1, modelo.getValueAt(0, 2));
            assertTrue(ControladorVenta.confirmarEdicionCarrito(tabla));
            assertEquals(3, modelo.getValueAt(0, 2));
        });
    }
}
