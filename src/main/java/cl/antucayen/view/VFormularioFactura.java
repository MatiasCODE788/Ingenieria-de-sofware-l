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
    private JComboBox<String> cmbModalidadIngreso;
    private JTextField        txtRutaArchivo;
    private JTable            tblItems;
    private DefaultTableModel modeloItems;
    private JButton           btnAgregarItem;
    private JButton           btnQuitarItem;
    private JButton           btnGuardar;
    private JButton           btnCancelar;
    private JLabel            lblError;

    public VFormularioFactura(JFrame parent) {
        super(parent, "Nueva Factura", true);
        initComponents();
    }

    private void initComponents() {
        setSize(640, 560);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel("📄  Nueva Factura");
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

        gbc.gridx = 0; gbc.gridy = 0; form.add(crearLabel("Número de factura *"), gbc);
        gbc.gridx = 1;               form.add(crearLabel("Fecha de emisión * (aaaa-mm-dd)"), gbc);
        txtNumero = crearCampo();
        txtFecha  = crearCampo();
        gbc.gridx = 0; gbc.gridy = 1; form.add(txtNumero, gbc);
        gbc.gridx = 1;               form.add(txtFecha, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(crearLabel("Proveedor *"), gbc);
        gbc.gridx = 1;               form.add(crearLabel("Modalidad de ingreso de ítems"), gbc);
        cmbProveedor = new JComboBox<>();
        cmbProveedor.setPreferredSize(new Dimension(0, 32));
        cmbModalidadIngreso = new JComboBox<>(new String[]{
                "Manual", "Archivo Excel/CSV (módulo de importación)"});
        cmbModalidadIngreso.setPreferredSize(new Dimension(0, 32));
        cmbModalidadIngreso.addActionListener(e -> actualizarModoIngreso());
        gbc.gridx = 0; gbc.gridy = 3; form.add(cmbProveedor, gbc);
        gbc.gridx = 1;               form.add(cmbModalidadIngreso, gbc);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 4;
        form.add(crearLabel("Ruta de archivo digital adjunto (opcional)"), gbc);
        txtRutaArchivo = crearCampo();
        gbc.gridy = 5;
        form.add(txtRutaArchivo, gbc);

        gbc.gridy = 6;
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        form.add(lblError, gbc);

        // Ítems
        JPanel panelItems = new JPanel(new BorderLayout(0, 6));
        panelItems.setBackground(Color.WHITE);
        panelItems.setBorder(BorderFactory.createEmptyBorder(0, 24, 8, 24));

        JPanel topItems = new JPanel(new BorderLayout());
        topItems.setBackground(Color.WHITE);
        JLabel lblItems = new JLabel("Ítems de la factura (ingreso manual)");
        lblItems.setFont(new Font("Arial", Font.BOLD, 13));
        lblItems.setForeground(new Color(17, 24, 39));

        JPanel botonesItems = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botonesItems.setBackground(Color.WHITE);
        btnAgregarItem = VBuscadorProductos.crearBoton("+ Agregar",  new Color(5, 150, 105));
        btnQuitarItem  = VBuscadorProductos.crearBoton("🗑 Quitar",  new Color(220, 38, 38));
        btnAgregarItem.setPreferredSize(new Dimension(100, 28));
        btnQuitarItem.setPreferredSize(new Dimension(90, 28));
        botonesItems.add(btnAgregarItem);
        botonesItems.add(btnQuitarItem);

        topItems.add(lblItems,     BorderLayout.WEST);
        topItems.add(botonesItems, BorderLayout.EAST);

        String[] colsItems = {"Código proveedor", "SKU (si ya se conoce)", "Cantidad", "Precio unitario compra"};
        modeloItems = new DefaultTableModel(colsItems, 0);
        tblItems = VBuscadorProductos.crearTabla(modeloItems);
        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.setPreferredSize(new Dimension(0, 150));

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

        btnGuardar = new JButton("Registrar factura");
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

        btnAgregarItem.addActionListener(e -> modeloItems.addRow(new Object[]{"", "", "", ""}));
        btnQuitarItem.addActionListener(e -> {
            int fila = tblItems.getSelectedRow();
            if (fila >= 0) modeloItems.removeRow(fila);
        });
    }

    private void actualizarModoIngreso() {
        boolean manual = cmbModalidadIngreso.getSelectedIndex() == 0;
        btnAgregarItem.setEnabled(manual);
        btnQuitarItem.setEnabled(manual);
        tblItems.setEnabled(manual);
        if (!manual)
            mostrarError("La carga por archivo se realiza desde el módulo de Importación de Inventario.");
        else
            limpiarError();
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
        cmbProveedor.removeAllItems();
        for (Proveedor p : proveedores)
            cmbProveedor.addItem(new ProveedorItem(p.getIdProveedor(), p.getNombre()));
    }

    public String getNumero()          { return txtNumero.getText().trim(); }
    public String getFechaTexto()      { return txtFecha.getText().trim(); }
    public String getRutaArchivo()     { return txtRutaArchivo.getText().trim(); }
    public boolean esIngresoManual()   { return cmbModalidadIngreso.getSelectedIndex() == 0; }

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
        ProveedorItem(int id, String nombre) { this.id = id; this.nombre = nombre; }
        @Override public String toString() { return nombre; }
    }
}