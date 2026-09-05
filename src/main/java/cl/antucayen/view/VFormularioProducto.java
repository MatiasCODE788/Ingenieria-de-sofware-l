package cl.antucayen.view;

import cl.antucayen.util.SesionActual;

import javax.swing.*;
import java.awt.*;

public class VFormularioProducto extends JDialog {

    private JTextField        txtSku;
    private JTextField        txtNombre;
    private JTextField        txtCodigoBarras;
    private JComboBox<String> cmbUnidad;
    private JTextField        txtPrecioVenta;
    private JTextField        txtStock;
    private JComboBox<String> cmbEstado;
    private JButton           btnGuardar;
    private JButton           btnCancelar;
    private JButton           btnInactivar;
    private JButton           btnEliminar;
    private JLabel            lblError;
    private boolean           modoEdicion;

    public VFormularioProducto(JFrame parent, boolean modoEdicion) {
        super(parent, modoEdicion ? "Editar Producto" : "Nuevo Producto", true);
        this.modoEdicion = modoEdicion;
        initComponents();
    }

    private void initComponents() {
        setSize(500, 520);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel(modoEdicion ? "✏️  Editar Producto" : "📦  Nuevo Producto");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        header.add(lblTitulo);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 8);
        gbc.weightx = 0.5;

        gbc.gridx = 0; gbc.gridy = 0; form.add(crearLabel("SKU *"), gbc);
        gbc.gridx = 1;               form.add(crearLabel("Código de barras *"), gbc);

        txtSku = crearCampo();
        if (modoEdicion) txtSku.setEditable(false);
        gbc.gridx = 0; gbc.gridy = 1; form.add(txtSku, gbc);
        txtCodigoBarras = crearCampo();
        gbc.gridx = 1;               form.add(txtCodigoBarras, gbc);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 2;
        form.add(crearLabel("Nombre del producto *"), gbc);
        txtNombre = crearCampo();
        gbc.gridy = 3;
        form.add(txtNombre, gbc);

        gbc.gridwidth = 1; gbc.gridy = 4;
        gbc.gridx = 0; form.add(crearLabel("Unidad de medida *"), gbc);
        gbc.gridx = 1; form.add(crearLabel("Precio de venta ($) *"), gbc);

        cmbUnidad = new JComboBox<>(new String[]{"un", "kg", "g", "L", "mL", "caja", "paquete"});
        cmbUnidad.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbUnidad.setPreferredSize(new Dimension(0, 32));
        gbc.gridx = 0; gbc.gridy = 5; form.add(cmbUnidad, gbc);
        txtPrecioVenta = crearCampo();
        txtPrecioVenta.setText("0");
        gbc.gridx = 1; form.add(txtPrecioVenta, gbc);

        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 6;
        form.add(crearLabel("Stock actual"), gbc);
        txtStock = crearCampo();
        txtStock.setText("0");
        gbc.gridy = 7; form.add(txtStock, gbc);

        if (modoEdicion) {
            gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 8;
            form.add(crearLabel("Estado"), gbc);
            cmbEstado = new JComboBox<>(new String[]{"Activo", "Inactivo"});
            cmbEstado.setFont(new Font("Arial", Font.PLAIN, 13));
            gbc.gridy = 9; form.add(cmbEstado, gbc);
        }

        gbc.gridwidth = 2; gbc.gridx = 0;
        gbc.gridy = modoEdicion ? 10 : 8;
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        form.add(lblError, gbc);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        boolean esAdmin = SesionActual.esAdministrador();

        if (modoEdicion && esAdmin) {
            btnEliminar = new JButton("Eliminar");
            btnEliminar.setFont(new Font("Arial", Font.BOLD, 12));
            btnEliminar.setBackground(new Color(220, 38, 38));
            btnEliminar.setForeground(Color.WHITE);
            btnEliminar.setFocusPainted(false);
            btnEliminar.setBorderPainted(false);
            btnEliminar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            botones.add(btnEliminar);
        }

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
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
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

        add(header,  BorderLayout.NORTH);
        add(form,    BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
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

    public String getSku()          { return txtSku.getText().trim(); }
    public String getNombre()       { return txtNombre.getText().trim(); }
    public String getCodigoBarras() { return txtCodigoBarras.getText().trim(); }
    public String getUnidad()       { return (String) cmbUnidad.getSelectedItem(); }
    public String getPrecioVenta()  { return txtPrecioVenta.getText().trim(); }
    public String getStock()        { return txtStock.getText().trim(); }
    public String getEstado()       { return cmbEstado != null ? (String) cmbEstado.getSelectedItem() : "Activo"; }

    public JButton getBtnGuardar()   { return btnGuardar; }
    public JButton getBtnCancelar()  { return btnCancelar; }
    public JButton getBtnInactivar() { return btnInactivar; }
    public JButton getBtnEliminar()  { return btnEliminar; }

    public void mostrarError(String msg) { lblError.setText("⚠ " + msg); lblError.setVisible(true); }
    public void limpiarError()           { lblError.setVisible(false); }

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
        if (cmbEstado != null) cmbEstado.setSelectedItem(estado);
    }
}