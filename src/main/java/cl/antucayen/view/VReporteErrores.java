package cl.antucayen.view;

import cl.antucayen.model.entity.ErrorImportacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Componente reutilizable para mostrar una lista de errores de importación
 * o procesamiento: número de ítem/fila, código o SKU involucrado, y
 * descripción del error. Se usa desde VAjusteInventario (importación) y
 * puede reutilizarse desde cualquier otro flujo similar.
 */
public class VReporteErrores extends JPanel {

    private JLabel lblTitulo;
    private JTable tblErrores;
    private DefaultTableModel modelo;

    public VReporteErrores() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout(0, 4));
        setBackground(Color.WHITE);

        lblTitulo = new JLabel("⚠ Errores detectados");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 13));
        lblTitulo.setForeground(new Color(185, 28, 28));

        String[] cols = {"Fila / Ítem", "Código / SKU", "Descripción"};
        modelo = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblErrores = VBuscadorProductos.crearTabla(modelo);
        tblErrores.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblErrores.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblErrores.getColumnModel().getColumn(2).setPreferredWidth(380);

        JScrollPane scroll = new JScrollPane(tblErrores);
        scroll.setPreferredSize(new Dimension(0, 130));

        add(lblTitulo, BorderLayout.NORTH);
        add(scroll,    BorderLayout.CENTER);
        setVisible(false); // oculto hasta que haya errores que mostrar
    }

    public void cargarErrores(List<ErrorImportacion> errores) {
        modelo.setRowCount(0);
        for (ErrorImportacion e : errores) {
            modelo.addRow(new Object[]{
                    e.getFila() == 0 ? "-" : String.valueOf(e.getFila()),
                    e.getColumna(),
                    e.getDescripcion()
            });
        }
        lblTitulo.setText("⚠ " + errores.size() + " error(es) detectado(s)");
        setVisible(!errores.isEmpty());
    }

    public void limpiar() {
        modelo.setRowCount(0);
        setVisible(false);
    }
}
