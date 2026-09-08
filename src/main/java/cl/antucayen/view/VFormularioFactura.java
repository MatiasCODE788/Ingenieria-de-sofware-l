package cl.antucayen.view;

import cl.antucayen.model.entity.Proveedor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VFormularioFactura extends JDialog {

    private JTextField        txtNumero;
    private JTextField        txtFecha; // formato yyyy-MM-dd
    private JComboBox<ProveedorItem> cmbProveedor;
    private JLabel             lblRutProveedor;
    private JTextField        txtValorTotal;
    private JTable            tblItems;
    private DefaultTableModel modeloItems;
    private JButton           btnAgregarProducto;
    private JButton           btnQuitarProducto;
    private JButton           btnGuardar;
    private JButton           btnCancelar;
    private JLabel            lblError;

    private List<Proveedor> proveedores;

    public VFormularioFactura(JFrame parent) {
        super(parent, "Procesar Factura", true);
        initComponents();
    }

    private void initComponents() {
        setSize(680, 600);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel("📄  Procesar Factura");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        header.add(lblTitulo);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(14, 24, 6, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 8);
        gbc.weightx = 0.5;

        // Fila 1: Proveedor (nombre) + RUT (se muestra solo, según el proveedor elegido)
        gbc.gridx = 0; gbc.gridy = 0; form.add(crearLabel("Proveedor (nombre) *"), gbc);
        gbc.gridx = 1;               form.add(crearLabel("RUT proveedor"), gbc);
        cmbProveedor = new JComboBox<>();
        cmbProveedor.setPreferredSize(new Dimension(0, 32));
        cmbProveedor.addActionListener(e -> actualizarRutProveedor());
        lblRutProveedor = new JLabel("—");
        lblRutProveedor.setFont(new Font("Arial", Font.PLAIN, 13));
        lblRutProveedor.setForeground(new Color(17, 24, 39));
        lblRutProveedor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        gbc.gridx = 0; gbc.gridy = 1; form.add(cmbProveedor, gbc);
        gbc.gridx = 1;               form.add(lblRutProveedor, gbc);

        // Fila 2: Número de factura + Valor total
        gbc.gridx = 0; gbc.gridy = 2; form.add(crearLabel("N° de factura *"), gbc);
        gbc.gridx = 1;               form.add(crearLabel("Valor total de la factura *"), gbc);
        txtNumero     = crearCampo();
        txtValorTotal = crearCampo();
        txtValorTotal.setToolTipText("Monto total, solo números (ej: 45000)");
        gbc.gridx = 0; gbc.gridy = 3; form.add(txtNumero, gbc);
        gbc.gridx = 1;               form.add(txtValorTotal, gbc);

        // Fila 3: Fecha de emisión
        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 4;
        form.add(crearLabel("Fecha de emisión * (aaaa-mm-dd)"), gbc);
        txtFecha = crearCampo();
        gbc.gridy = 5;
        form.add(txtFecha, gbc);

        gbc.gridy = 6;
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        form.add(lblError, gbc);

        // Productos
        JPanel panelItems = new JPanel(new BorderLayout(0, 6));
        panelItems.setBackground(Color.WHITE);
        panelItems.setBorder(BorderFactory.createEmptyBorder(0, 24, 8, 24));

        JPanel topItems = new JPanel(new BorderLayout());
        topItems.setBackground(Color.WHITE);
        JLabel lblItems = new JLabel("Productos de la factura");
        lblItems.setFont(new Font("Arial", Font.BOLD, 13));
        lblItems.setForeground(new Color(17, 24, 39));

        JPanel botonesItems = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botonesItems.setBackground(Color.WHITE);
        btnAgregarProducto = VBuscadorProductos.crearBoton("+ Agregar producto", new Color(5, 150, 105));
        btnQuitarProducto  = VBuscadorProductos.crearBoton("🗑 Quitar", new Color(220, 38, 38));
        btnAgregarProducto.setPreferredSize(new Dimension(150, 28));
        btnQuitarProducto.setPreferredSize(new Dimension(90, 28));
        botonesItems.add(btnAgregarProducto);
        botonesItems.add(btnQuitarProducto);

        topItems.add(lblItems,     BorderLayout.WEST);
        topItems.add(botonesItems, BorderLayout.EAST);

        String[] colsItems = {"Descripción", "Cantidad"};
        modeloItems = new DefaultTableModel(colsItems, 0);
        tblItems = VBuscadorProductos.crearTabla(modeloItems);
        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.setPreferredSize(new Dimension(0, 220));

        panelItems.add(topItems,    BorderLayout.NORTH);
        panelItems.add(scrollItems, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Color.WHITE);
        centro.add(form,       BorderLayout.NORTH);
        centro.add(panelItems, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setForeground(new Color(30, 41, 59));
        btnCancelar.setFocusPainted(false);
        btnCancelar.addActionListener(e -> dispose());

        btnGuardar = new JButton("Guardar factura");
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 12));
        btnGuardar.setBackground(new Color(5, 150, 105));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        botones.add(btnCancelar);
        botones.add(btnGuardar);

        add(header, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        // Cada clic en "+ Agregar producto" agrega una fila vacía y editable
        // para que el usuario complete Descripción y Cantidad.
        btnAgregarProducto.addActionListener(e -> modeloItems.addRow(new Object[]{"", ""}));
        btnQuitarProducto.addActionListener(e -> {
            int fila = tblItems.getSelectedRow();
            if (fila >= 0) modeloItems.removeRow(fila);
        });
    }

    private void actualizarRutProveedor() {
        ProveedorItem item = (ProveedorItem) cmbProveedor.getSelectedItem();
        lblRutProveedor.setText(item != null ? item.rut : "—");
    }

    private JLabel crearLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        l.setForeground(new Color(71, 85, 105));
        return l;
    }

    private JTextField crearCampo() {
        JTextField t = new JTextField();
        t.setFont(new Font("Arial", Font.PLAIN, 13));
        t.setPreferredSize(new Dimension(0, 32));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return t;
    }

    public void cargarProveedores(List<Proveedor> proveedores) {
        this.proveedores = proveedores;
        cmbProveedor.removeAllItems();
        for (Proveedor p : proveedores)
            cmbProveedor.addItem(new ProveedorItem(p.getIdProveedor(), p.getNombre(), p.getRut()));
        actualizarRutProveedor();
    }

    public String getNumero()          { return txtNumero.getText().trim(); }
    public String getFechaTexto()      { return txtFecha.getText().trim(); }
    public String getValorTotalTexto() { return txtValorTotal.getText().trim(); }

    public int getIdProveedorSeleccionado() {
        ProveedorItem item = (ProveedorItem) cmbProveedor.getSelectedItem();
        return item != null ? item.id : -1;
    }

    public DefaultTableModel getModeloItems() { return modeloItems; }
    public JButton getBtnGuardar()   { return btnGuardar; }
    public JButton getBtnCancelar()  { return btnCancelar; }

    public void mostrarError(String msg) { lblError.setText("⚠ " + msg); lblError.setVisible(true); }
    public void limpiarError()           { lblError.setVisible(false); }

    private static class ProveedorItem {
        final int id;
        final String nombre;
        final String rut;
        ProveedorItem(int id, String nombre, String rut) { this.id = id; this.nombre = nombre; this.rut = rut; }
        @Override public String toString() { return nombre; }
    }
}