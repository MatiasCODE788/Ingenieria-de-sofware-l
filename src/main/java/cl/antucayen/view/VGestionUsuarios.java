package cl.antucayen.view;

import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VGestionUsuarios extends JPanel {

    private JTable tblUsuarios;
    private DefaultTableModel modeloTabla;
    private JButton btnNuevo;
    private JButton btnEditar;
    private JButton btnDesactivar;

    public VGestionUsuarios() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        btnNuevo = ComponentesSwing.crearBoton("+ Nuevo usuario", new Color(5, 150, 105));
        btnEditar = ComponentesSwing.crearBoton("Editar", new Color(37, 99, 235));
        btnDesactivar = ComponentesSwing.crearBoton("Desactivar", new Color(220, 38, 38));

        barraTop.add(btnNuevo);
        barraTop.add(btnEditar);
        barraTop.add(btnDesactivar);

        String[] cols = {"ID", "Nombre completo", "Username", "Perfil", "Estado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblUsuarios = ComponentesSwing.crearTabla(modeloTabla);
        tblUsuarios.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblUsuarios.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblUsuarios.getColumnModel().getColumn(2).setPreferredWidth(160);
        tblUsuarios.getColumnModel().getColumn(3).setPreferredWidth(140);
        tblUsuarios.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(tblUsuarios);
        add(barraTop, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public JButton getBtnNuevo() { return btnNuevo; }
    public JButton getBtnEditar() { return btnEditar; }
    public JButton getBtnDesactivar() { return btnDesactivar; }
    public JTable getTblUsuarios() { return tblUsuarios; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public void limpiarTabla() { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
    public int getFilaSeleccionada() { return tblUsuarios.getSelectedRow(); }

    public Object getValorFila(int fila, int col) {
        return modeloTabla.getValueAt(fila, col);
    }
}
