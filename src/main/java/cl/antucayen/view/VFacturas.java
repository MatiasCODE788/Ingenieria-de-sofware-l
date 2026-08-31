package cl.antucayen.view;

import cl.antucayen.model.entity.Proveedor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VFacturas extends JPanel {

    private JTextField        txtFiltroNumero;
    private JComboBox<ProveedorItem> cmbFiltroProveedor;
    private JTextField        txtFiltroDesde;
    private JTextField        txtFiltroHasta;
    private JComboBox<String> cmbFiltroEstado;
    private JButton           btnBuscar;
    private JButton           btnLimpiar;
    private JButton           btnNueva;
    private JTable            tblFacturas;
    private DefaultTableModel modeloTabla;

    public VFacturas() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 10));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        txtFiltroNumero = crearCampoFiltro(110);
        cmbFiltroProveedor = new JComboBox<>();
        cmbFiltroProveedor.setPreferredSize(new Dimension(160, 32));
        txtFiltroDesde = crearCampoFiltro(100);
        txtFiltroDesde.setToolTipText("aaaa-mm-dd");
        txtFiltroHasta = crearCampoFiltro(100);
        txtFiltroHasta.setToolTipText("aaaa-mm-dd");
        cmbFiltroEstado = new JComboBox<>(new String[]{"Todos", "Pendiente", "Procesada", "Observada"});
        cmbFiltroEstado.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbFiltroEstado.setPreferredSize(new Dimension(120, 32));

        btnBuscar  = VBuscadorProductos.crearBoton("🔍 Buscar",     new Color(37, 99, 235));
        btnLimpiar = VBuscadorProductos.crearBoton("✕ Limpiar",    new Color(107, 114, 128));
        btnNueva   = VBuscadorProductos.crearBoton("+ Nueva factura", new Color(5, 150, 105));
        btnBuscar.setPreferredSize(new Dimension(90, 32));
        btnLimpiar.setPreferredSize(new Dimension(90, 32));

        barraTop.add(new JLabel("Número:"));
        barraTop.add(txtFiltroNumero);
        barraTop.add(new JLabel("Proveedor:"));
        barraTop.add(cmbFiltroProveedor);
        barraTop.add(new JLabel("Desde:"));
        barraTop.add(txtFiltroDesde);
        barraTop.add(new JLabel("Hasta:"));
        barraTop.add(txtFiltroHasta);
        barraTop.add(new JLabel("Estado:"));
        barraTop.add(cmbFiltroEstado);
        barraTop.add(btnBuscar);
        barraTop.add(btnLimpiar);
        barraTop.add(Box.createHorizontalStrut(16));
        barraTop.add(btnNueva);

        String[] cols = {"ID", "Número", "Fecha", "Proveedor", "Estado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblFacturas = VBuscadorProductos.crearTabla(modeloTabla);
        tblFacturas.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblFacturas.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblFacturas.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblFacturas.getColumnModel().getColumn(3).setPreferredWidth(240);
        tblFacturas.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(tblFacturas);
        JLabel lblAyuda = new JLabel("  💡 Doble clic sobre una factura para ver su detalle y procesarla");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));

        add(barraTop, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);
        add(lblAyuda, BorderLayout.SOUTH);
    }

    private JTextField crearCampoFiltro(int ancho) {
        JTextField t = new JTextField();
        t.setFont(new Font("Arial", Font.PLAIN, 13));
        t.setPreferredSize(new Dimension(ancho, 32));
        return t;
    }

    public void cargarProveedores(List<Proveedor> proveedores) {
        cmbFiltroProveedor.removeAllItems();
        cmbFiltroProveedor.addItem(new ProveedorItem(-1, "Todos"));
        for (Proveedor p : proveedores)
            cmbFiltroProveedor.addItem(new ProveedorItem(p.getIdProveedor(), p.getNombre()));
    }

    public String  getFiltroNumero()  { return txtFiltroNumero.getText().trim(); }
    public String  getFiltroDesde()   { return txtFiltroDesde.getText().trim(); }
    public String  getFiltroHasta()   { return txtFiltroHasta.getText().trim(); }
    public String  getFiltroEstado()  { return (String) cmbFiltroEstado.getSelectedItem(); }

    public int getFiltroIdProveedor() {
        ProveedorItem item = (ProveedorItem) cmbFiltroProveedor.getSelectedItem();
        return item != null ? item.id : -1;
    }

    public void limpiarFiltros() {
        txtFiltroNumero.setText("");
        txtFiltroDesde.setText("");
        txtFiltroHasta.setText("");
        cmbFiltroEstado.setSelectedIndex(0);
        cmbFiltroProveedor.setSelectedIndex(0);
    }

    public JButton getBtnBuscar()     { return btnBuscar; }
    public JButton getBtnLimpiar()    { return btnLimpiar; }
    public JButton getBtnNueva()      { return btnNueva; }
    public JTable  getTblFacturas()   { return tblFacturas; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }

    private static class ProveedorItem {
        final int id;
        final String nombre;
        ProveedorItem(int id, String nombre) { this.id = id; this.nombre = nombre; }
        @Override public String toString() { return nombre; }
    }
}