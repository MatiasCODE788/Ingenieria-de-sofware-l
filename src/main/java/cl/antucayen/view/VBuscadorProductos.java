package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VBuscadorProductos extends JPanel {

    private JTextField        txtBusqueda;
    private JComboBox<String> cmbTipo;
    private JButton           btnBuscar;
    private JButton           btnNuevo;
    private JTable            tblProductos;
    private DefaultTableModel modeloTabla;

    public VBuscadorProductos() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(243, 244, 246));

        // Barra superior
        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        txtBusqueda = new JTextField(25);
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 13));
        txtBusqueda.setPreferredSize(new Dimension(280, 32));

        cmbTipo = new JComboBox<>(new String[]{"Nombre", "SKU", "Código de barras"});
        cmbTipo.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbTipo.setPreferredSize(new Dimension(160, 32));

        btnBuscar = crearBoton("🔍 Buscar",          new Color(37, 99, 235));
        btnNuevo  = crearBoton("+ Nuevo producto",    new Color(5, 150, 105));

        barraTop.add(new JLabel("Buscar:"));
        barraTop.add(txtBusqueda);
        barraTop.add(cmbTipo);
        barraTop.add(btnBuscar);
        barraTop.add(Box.createHorizontalStrut(16));
        barraTop.add(btnNuevo);

        // Tabla
        String[] cols = {"SKU", "Nombre", "Código de barras", "Unidad", "Stock", "Estado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblProductos = crearTabla(modeloTabla);
        tblProductos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblProductos.getColumnModel().getColumn(1).setPreferredWidth(260);
        tblProductos.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblProductos.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(4).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(5).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(tblProductos);

        JLabel lblAyuda = new JLabel("  💡 Doble clic en una fila para editar");
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
    public JTable  getTblProductos()  { return tblProductos; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void limpiarTabla()              { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila)  { modeloTabla.addRow(fila); }

    static JTable crearTabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setFont(new Font("Arial", Font.PLAIN, 13));
        t.setRowHeight(36);
        t.setGridColor(new Color(229, 231, 235));
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(new Color(17, 24, 39));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(17, 24, 39));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 38));
        return t;
    }

    static JButton crearBoton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 32));
        return btn;
    }
}