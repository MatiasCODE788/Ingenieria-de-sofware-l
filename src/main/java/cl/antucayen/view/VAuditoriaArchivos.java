package cl.antucayen.view;

import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Vista administrativa de RF-37: bitácora de importaciones/exportaciones. */
public class VAuditoriaArchivos extends JPanel {

    private final JButton btnActualizar;
    private final JSpinner spnLimite;
    private final DefaultTableModel modelo;
    private final JTable tabla;

    public VAuditoriaArchivos() {
        setLayout(new BorderLayout(0, 12));
        setBackground(new Color(243, 244, 246));

        JPanel superior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        superior.setBackground(Color.WHITE);
        superior.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel lbl = new JLabel("Últimos registros:");
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        superior.add(lbl);

        spnLimite = new JSpinner(new SpinnerNumberModel(100, 10, 500, 10));
        spnLimite.setPreferredSize(new Dimension(80, 30));
        superior.add(spnLimite);

        btnActualizar = ComponentesSwing.crearBoton("Actualizar", new Color(37, 99, 235));
        btnActualizar.setPreferredSize(new Dimension(110, 30));
        superior.add(btnActualizar);

        String[] columnas = {
                "Fecha/Hora", "Usuario", "Operación", "Archivo",
                "Formato", "Resultado", "Detalle"
        };
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tabla = ComponentesSwing.crearTabla(modelo);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(75);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(85);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(320);

        add(superior, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public JButton getBtnActualizar() { return btnActualizar; }
    public int getLimite() { return (Integer) spnLimite.getValue(); }

    public void limpiar() { modelo.setRowCount(0); }
    public void agregarFila(Object[] fila) { modelo.addRow(fila); }
}
