package cl.antucayen.view;

import cl.antucayen.model.entity.Proveedor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VFormularioProducto extends JDialog {

    private JTextField txtSku;
    private JTextField txtNombre;
    private JTextField txtCodigoBarras;
    private JComboBox<String> cmbUnidad;
    private JTextField txtPrecioVenta;
    private JTextField txtStock;
    private JComboBox<String> cmbEstado;
    private JList<ProveedorItem> lstProveedores;
    private DefaultListModel<ProveedorItem> modeloProveedores;
    private JButton btnGuardar;
    private JButton btnCancelar;
    private JButton btnInactivar;
    private JLabel lblError;
    private final boolean modoEdicion;

    public VFormularioProducto(JFrame parent, boolean modoEdicion) {
        super(parent, modoEdicion ? "Editar Producto" : "Nuevo Producto", true);
        this.modoEdicion = modoEdicion;
        initComponents();
    }

    private void initComponents() {
        setSize(560, 690);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel(modoEdicion ? "Editar Producto" : "Nuevo Producto");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        header.add(lblTitulo);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 8);
        gbc.weightx = 0.5;

        gbc.gridx = 0; gbc.gridy = 0; form.add(crearLabel("SKU *"), gbc);
        gbc.gridx = 1; form.add(crearLabel("Código de barras *"), gbc);
        txtSku = crearCampo();
        if (modoEdicion) txtSku.setEditable(false);
        txtCodigoBarras = crearCampo();
        gbc.gridx = 0; gbc.gridy = 1; form.add(txtSku, gbc);
        gbc.gridx = 1; form.add(txtCodigoBarras, gbc);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 2;
        form.add(crearLabel("Nombre del producto *"), gbc);
        txtNombre = crearCampo();
        gbc.gridy = 3; form.add(txtNombre, gbc);

        gbc.gridwidth = 1; gbc.gridy = 4;
        gbc.gridx = 0; form.add(crearLabel("Unidad de medida *"), gbc);
        gbc.gridx = 1; form.add(crearLabel("Precio de venta ($) *"), gbc);
        cmbUnidad = new JComboBox<>(new String[]{"un", "kg", "g", "L", "mL", "caja", "paquete"});
        cmbUnidad.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbUnidad.setPreferredSize(new Dimension(0, 32));
        txtPrecioVenta = crearCampo();
        txtPrecioVenta.setText("0");
        gbc.gridx = 0; gbc.gridy = 5; form.add(cmbUnidad, gbc);
        gbc.gridx = 1; form.add(txtPrecioVenta, gbc);

        gbc.gridx = 0; gbc.gridy = 6; form.add(crearLabel(modoEdicion ? "Stock actual (solo lectura)" : "Stock inicial"), gbc);
        gbc.gridx = 1; form.add(crearLabel("Estado"), gbc);
        txtStock = crearCampo();
        txtStock.setText("0");
        txtStock.setEditable(!modoEdicion);
        cmbEstado = new JComboBox<>(new String[]{"Activo", "Inactivo"});
        cmbEstado.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbEstado.setEnabled(modoEdicion);
        gbc.gridx = 0; gbc.gridy = 7; form.add(txtStock, gbc);
        gbc.gridx = 1; form.add(cmbEstado, gbc);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 8;
        form.add(crearLabel("Proveedores asociados (Ctrl/Cmd para selección múltiple)"), gbc);
        modeloProveedores = new DefaultListModel<>();
        lstProveedores = new JList<>(modeloProveedores);
        lstProveedores.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstProveedores.setVisibleRowCount(5);
        lstProveedores.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane scrollProveedores = new JScrollPane(lstProveedores);
        scrollProveedores.setPreferredSize(new Dimension(0, 100));
        gbc.gridy = 9; form.add(scrollProveedores, gbc);

        gbc.gridy = 10;
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        form.add(lblError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        if (modoEdicion) {
            btnInactivar = new JButton("Inactivar");
            btnInactivar.setFont(new Font("Arial", Font.BOLD, 12));
            btnInactivar.setBackground(new Color(234, 88, 12));
            btnInactivar.setForeground(Color.WHITE);
            btnInactivar.setFocusPainted(false);
            btnInactivar.setBorderPainted(false);
            btnInactivar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            botones.add(btnInactivar);
        }

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setForeground(new Color(30, 41, 59));
        btnCancelar.setFocusPainted(false);
        btnCancelar.addActionListener(e -> dispose());

        btnGuardar = new JButton(modoEdicion ? "Guardar cambios" : "Registrar");
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 12));
        btnGuardar.setBackground(new Color(5, 150, 105));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        botones.add(btnCancelar);
        botones.add(btnGuardar);

        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    private JLabel crearLabel(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(new Color(71, 85, 105));
        return label;
    }

    private JTextField crearCampo() {
        JTextField campo = new JTextField();
        campo.setFont(new Font("Arial", Font.PLAIN, 13));
        campo.setPreferredSize(new Dimension(0, 32));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return campo;
    }

    public void cargarProveedores(List<Proveedor> proveedores, List<Integer> idsSeleccionados) {
        modeloProveedores.clear();
        Set<Integer> seleccionados = new HashSet<>(idsSeleccionados == null ? List.of() : idsSeleccionados);
        List<Integer> indices = new ArrayList<>();
        for (Proveedor proveedor : proveedores) {
            int indice = modeloProveedores.size();
            modeloProveedores.addElement(new ProveedorItem(proveedor.getIdProveedor(), proveedor.getNombre()));
            if (seleccionados.contains(proveedor.getIdProveedor())) indices.add(indice);
        }
        lstProveedores.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
    }

    public List<Integer> getIdsProveedoresSeleccionados() {
        return lstProveedores.getSelectedValuesList().stream().map(ProveedorItem::id).toList();
    }

    public String getSku() { return txtSku.getText().trim(); }
    public String getNombre() { return txtNombre.getText().trim(); }
    public String getCodigoBarras() { return txtCodigoBarras.getText().trim(); }
    public String getUnidad() { return (String) cmbUnidad.getSelectedItem(); }
    public String getPrecioVenta() { return txtPrecioVenta.getText().trim(); }
    public String getStock() { return txtStock.getText().trim(); }
    public String getEstado() { return (String) cmbEstado.getSelectedItem(); }

    public void configurarSoloLectura() {
        setTitle("Detalle Producto / Stock");
        txtSku.setEditable(false);
        txtNombre.setEditable(false);
        txtCodigoBarras.setEditable(false);
        txtPrecioVenta.setEditable(false);
        txtStock.setEditable(false);
        cmbUnidad.setEnabled(false);
        cmbEstado.setEnabled(false);
        lstProveedores.setEnabled(false);
        btnGuardar.setVisible(false);
        if (btnInactivar != null) btnInactivar.setVisible(false);
        btnCancelar.setText("Cerrar");
    }

    public JButton getBtnGuardar() { return btnGuardar; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnInactivar() { return btnInactivar; }

    public void mostrarError(String msg) {
        lblError.setText(msg == null ? "" : msg);
        lblError.setVisible(true);
    }

    public void limpiarError() { lblError.setVisible(false); }

    public void marcarSkuError() {
        txtSku.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
    }

    public void setDatos(String sku, String nombre, String cb, String unidad,
                         int precioVenta, int stock, String estado) {
        txtSku.setText(sku);
        txtNombre.setText(nombre);
        txtCodigoBarras.setText(cb);
        cmbUnidad.setSelectedItem(unidad);
        txtPrecioVenta.setText(String.valueOf(precioVenta));
        txtStock.setText(String.valueOf(stock));
        cmbEstado.setSelectedItem(estado);
    }

    private record ProveedorItem(int id, String nombre) {
        @Override public String toString() { return nombre; }
    }
}
