package cl.antucayen.view;

import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VBuscadorProductos extends JPanel {

    private JTextField        txtBusqueda;
    private JComboBox<String> cmbTipo;
    private JButton           btnBuscar;
    private JButton           btnNuevo;
    private JButton           btnExportar;
    private JTable            tblProductos;
    private DefaultTableModel modeloTabla;

    public VBuscadorProductos() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        txtBusqueda = new JTextField(25);
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 13));
        txtBusqueda.setPreferredSize(new Dimension(280, 32));

        cmbTipo = new JComboBox<>(new String[]{"Nombre", "SKU", "Código de barras"});
        cmbTipo.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbTipo.setPreferredSize(new Dimension(160, 32));

        btnBuscar = ComponentesSwing.crearBoton("Buscar", new Color(37, 99, 235));
        btnNuevo  = ComponentesSwing.crearBoton("+ Nuevo producto", new Color(5, 150, 105));
        btnExportar = ComponentesSwing.crearBoton("Exportar CSV/Excel", new Color(2, 132, 199));
        btnExportar.setPreferredSize(new Dimension(165, 32));

        barraTop.add(new JLabel("Buscar:"));
        barraTop.add(txtBusqueda);
        barraTop.add(cmbTipo);
        barraTop.add(btnBuscar);
        barraTop.add(Box.createHorizontalStrut(16));
        barraTop.add(btnNuevo);
        barraTop.add(btnExportar);

        String[] cols = {"SKU", "Nombre", "Código de barras", "Unidad", "Precio unitario", "Stock", "Estado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblProductos = ComponentesSwing.crearTabla(modeloTabla);
        tblProductos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblProductos.getColumnModel().getColumn(1).setPreferredWidth(260);
        tblProductos.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblProductos.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblProductos.getColumnModel().getColumn(5).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(6).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(tblProductos);

        JLabel lblAyuda = new JLabel("  Doble clic en una fila para abrir la ficha del producto");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));
        lblAyuda.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        add(barraTop, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);
        add(lblAyuda, BorderLayout.SOUTH);
    }

    public String  getTextoBusqueda() { return txtBusqueda.getText().trim(); }
    public String  getTipoBusqueda()  { return (String) cmbTipo.getSelectedItem(); }
    public JButton getBtnBuscar()     { return btnBuscar; }
    public JButton getBtnNuevo()      { return btnNuevo; }
    public JButton getBtnExportar()   { return btnExportar; }
    public JTable  getTblProductos()  { return tblProductos; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
}
