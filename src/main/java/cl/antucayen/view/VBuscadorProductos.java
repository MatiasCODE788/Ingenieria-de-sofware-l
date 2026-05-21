package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VBuscadorProductos extends JPanel {

    private JTextField txtBusqueda;
    private JComboBox<String> cmbTipoBusqueda;
    private JButton btnBuscar;
    private JButton btnNuevoProducto;
    private JTable tblProductos;
    private DefaultTableModel modeloTabla;

    public VBuscadorProductos() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(240, 244, 248));

        // ── BARRA DE BÚSQUEDA ─────────────────────────────────────
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelBusqueda.setBackground(Color.WHITE);
        panelBusqueda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 227, 237)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setFont(new Font("Arial", Font.BOLD, 13));

        txtBusqueda = new JTextField(25);
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 13));
        txtBusqueda.setPreferredSize(new Dimension(280, 32));

        cmbTipoBusqueda = new JComboBox<>(new String[]{
                "Nombre", "SKU", "Código de barras"
        });
        cmbTipoBusqueda.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbTipoBusqueda.setPreferredSize(new Dimension(160, 32));

        btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.setFont(new Font("Arial", Font.BOLD, 13));
        btnBuscar.setBackground(new Color(37, 99, 235));
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setFocusPainted(false);
        btnBuscar.setBorderPainted(false);
        btnBuscar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscar.setPreferredSize(new Dimension(110, 32));

        btnNuevoProducto = new JButton("+ Nuevo producto");
        btnNuevoProducto.setFont(new Font("Arial", Font.BOLD, 13));
        btnNuevoProducto.setBackground(new Color(5, 150, 105));
        btnNuevoProducto.setForeground(Color.WHITE);
        btnNuevoProducto.setFocusPainted(false);
        btnNuevoProducto.setBorderPainted(false);
        btnNuevoProducto.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNuevoProducto.setPreferredSize(new Dimension(160, 32));

        panelBusqueda.add(lblBuscar);
        panelBusqueda.add(txtBusqueda);
        panelBusqueda.add(cmbTipoBusqueda);
        panelBusqueda.add(btnBuscar);
        panelBusqueda.add(Box.createHorizontalStrut(20));
        panelBusqueda.add(btnNuevoProducto);

        // ── TABLA DE RESULTADOS ───────────────────────────────────
        String[] columnas = {"SKU", "Nombre", "Código de barras", "Unidad", "Stock", "Estado"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // tabla solo lectura
            }
        };

        tblProductos = new JTable(modeloTabla);
        tblProductos.setFont(new Font("Arial", Font.PLAIN, 13));
        tblProductos.setRowHeight(36);
        tblProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblProductos.setGridColor(new Color(220, 227, 237));
        tblProductos.setSelectionBackground(new Color(219, 234, 254));
        tblProductos.setSelectionForeground(new Color(30, 41, 59));

        // Encabezado de la tabla
        tblProductos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblProductos.getTableHeader().setBackground(new Color(30, 48, 84));
        tblProductos.getTableHeader().setForeground(Color.WHITE);
        tblProductos.getTableHeader().setPreferredSize(new Dimension(0, 38));

        // Anchos de columna
        tblProductos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblProductos.getColumnModel().getColumn(1).setPreferredWidth(250);
        tblProductos.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblProductos.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblProductos.getColumnModel().getColumn(4).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(5).setPreferredWidth(80);

        JScrollPane scrollTabla = new JScrollPane(tblProductos);
        scrollTabla.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));
        scrollTabla.getViewport().setBackground(Color.WHITE);

        // Panel tabla con título
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));

        JPanel panelTituloTabla = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        panelTituloTabla.setBackground(Color.WHITE);
        JLabel lblTituloTabla = new JLabel("Resultados");
        lblTituloTabla.setFont(new Font("Arial", Font.BOLD, 14));
        lblTituloTabla.setForeground(new Color(30, 41, 59));
        panelTituloTabla.add(lblTituloTabla);

        panelTabla.add(panelTituloTabla, BorderLayout.NORTH);
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        // Label ayuda doble clic
        JLabel lblAyuda = new JLabel("  💡 Doble clic en una fila para ver o editar el producto");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(100, 116, 139));
        lblAyuda.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        add(panelBusqueda, BorderLayout.NORTH);
        add(panelTabla, BorderLayout.CENTER);
        add(lblAyuda, BorderLayout.SOUTH);

        // Datos de ejemplo para ver cómo se ve
        cargarDatosEjemplo();
    }

    private void cargarDatosEjemplo() {
        modeloTabla.addRow(new Object[]{"SKU-001", "Arroz Grado 1 x 1kg", "7801234567890", "kg", 150, "Activo"});
        modeloTabla.addRow(new Object[]{"SKU-002", "Aceite Vegetal x 1L", "7809876543210", "L", 45, "Activo"});
        modeloTabla.addRow(new Object[]{"SKU-003", "Azúcar Blanca x 1kg", "7801111111111", "kg", 0, "Inactivo"});
        modeloTabla.addRow(new Object[]{"SKU-004", "Fideos Spaghetti x 500g", "7802222222222", "unidad", 200, "Activo"});
        modeloTabla.addRow(new Object[]{"SKU-005", "Leche Entera x 1L", "7803333333333", "L", 30, "Activo"});
    }

    // ── GETTERS PARA EL CONTROLADOR ───────────────────────────────
    public String getTextoBusqueda() {
        return txtBusqueda.getText().trim();
    }

    public String getTipoBusqueda() {
        return (String) cmbTipoBusqueda.getSelectedItem();
    }

    public JButton getBtnBuscar()         { return btnBuscar; }
    public JButton getBtnNuevoProducto()  { return btnNuevoProducto; }
    public JTable getTblProductos()       { return tblProductos; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void limpiarTabla() {
        modeloTabla.setRowCount(0);
    }

    public void agregarFilaProducto(Object[] fila) {
        modeloTabla.addRow(fila);
    }
}