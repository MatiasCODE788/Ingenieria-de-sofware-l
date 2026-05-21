package cl.antucayen.view;

import javax.swing.*;
import java.awt.*;

public class VFormularioProducto extends JDialog {

    private JTextField txtNombre;
    private JTextField txtSKU;
    private JTextField txtCodigoBarras;
    private JComboBox<String> cmbUnidadMedida;
    private JTextField txtStock;
    private JComboBox<String> cmbEstado;
    private JButton btnGuardar;
    private JButton btnCancelar;
    private JButton btnInactivar;
    private JLabel lblError;

    private boolean modoEdicion;

    public VFormularioProducto(JFrame parent, boolean modoEdicion) {
        super(parent, modoEdicion ? "Editar Producto" : "Nuevo Producto", true);
        this.modoEdicion = modoEdicion;
        initComponents();
    }

    private void initComponents() {
        setSize(480, 480);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        // ── HEADER ────────────────────────────────────────────────
        JPanel panelHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        panelHeader.setBackground(new Color(30, 48, 84));

        JLabel lblTitulo = new JLabel(modoEdicion ? "✏️  Editar Producto" : "📦  Nuevo Producto");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        panelHeader.add(lblTitulo);

        // ── FORMULARIO ────────────────────────────────────────────
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBackground(Color.WHITE);
        panelForm.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.gridwidth = 2;

        // Nombre
        gbc.gridx = 0; gbc.gridy = 0;
        panelForm.add(crearLabel("Nombre del producto *"), gbc);
        txtNombre = crearCampo();
        gbc.gridy = 1;
        panelForm.add(txtNombre, gbc);

        // SKU y Código de barras en la misma fila
        gbc.gridwidth = 1; gbc.gridy = 2;
        gbc.gridx = 0; gbc.weightx = 0.5;
        panelForm.add(crearLabel("SKU *"), gbc);
        gbc.gridx = 1;
        panelForm.add(crearLabel("Código de barras"), gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        txtSKU = crearCampo();
        if (modoEdicion) txtSKU.setEditable(false); // SKU no editable en modo edición
        panelForm.add(txtSKU, gbc);
        gbc.gridx = 1;
        txtCodigoBarras = crearCampo();
        panelForm.add(txtCodigoBarras, gbc);

        // Unidad de medida y Stock en la misma fila
        gbc.gridy = 4;
        gbc.gridx = 0;
        panelForm.add(crearLabel("Unidad de medida *"), gbc);
        gbc.gridx = 1;
        panelForm.add(crearLabel("Stock actual *"), gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        cmbUnidadMedida = new JComboBox<>(new String[]{
                "unidad", "kg", "g", "L", "mL", "caja", "paquete", "bolsa"
        });
        cmbUnidadMedida.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbUnidadMedida.setPreferredSize(new Dimension(0, 32));
        panelForm.add(cmbUnidadMedida, gbc);

        gbc.gridx = 1;
        txtStock = crearCampo();
        txtStock.setText("0");
        panelForm.add(txtStock, gbc);

        // Estado (solo en modo edición)
        if (modoEdicion) {
            gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 6;
            panelForm.add(crearLabel("Estado"), gbc);
            gbc.gridy = 7;
            cmbEstado = new JComboBox<>(new String[]{"Activo", "Inactivo"});
            cmbEstado.setFont(new Font("Arial", Font.PLAIN, 13));
            cmbEstado.setPreferredSize(new Dimension(0, 32));
            panelForm.add(cmbEstado, gbc);
        }

        // Label error
        gbc.gridy = modoEdicion ? 8 : 6;
        gbc.gridwidth = 2; gbc.gridx = 0;
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        panelForm.add(lblError, gbc);

        // ── BOTONES ───────────────────────────────────────────────
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        panelBotones.setBackground(new Color(248, 250, 252));
        panelBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 227, 237)));

        if (modoEdicion) {
            btnInactivar = new JButton("Inactivar producto");
            btnInactivar.setFont(new Font("Arial", Font.BOLD, 12));
            btnInactivar.setBackground(new Color(234, 88, 12));
            btnInactivar.setForeground(Color.WHITE);
            btnInactivar.setFocusPainted(false);
            btnInactivar.setBorderPainted(false);
            btnInactivar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            panelBotones.add(btnInactivar);
        }

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setForeground(new Color(30, 41, 59));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());

        btnGuardar = new JButton(modoEdicion ? "Guardar cambios" : "Registrar producto");
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 12));
        btnGuardar.setBackground(new Color(5, 150, 105));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);

        add(panelHeader, BorderLayout.NORTH);
        add(panelForm, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(new Color(71, 85, 105));
        return lbl;
    }

    private JTextField crearCampo() {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Arial", Font.PLAIN, 13));
        txt.setPreferredSize(new Dimension(0, 32));
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return txt;
    }

    // ── GETTERS PARA EL CONTROLADOR ───────────────────────────────
    public String getNombre()        { return txtNombre.getText().trim(); }
    public String getSKU()           { return txtSKU.getText().trim(); }
    public String getCodigoBarras()  { return txtCodigoBarras.getText().trim(); }
    public String getUnidadMedida()  { return (String) cmbUnidadMedida.getSelectedItem(); }
    public String getStock()         { return txtStock.getText().trim(); }
    public String getEstado()        { return cmbEstado != null ? (String) cmbEstado.getSelectedItem() : "Activo"; }

    public JButton getBtnGuardar()   { return btnGuardar; }
    public JButton getBtnCancelar()  { return btnCancelar; }
    public JButton getBtnInactivar() { return btnInactivar; }

    public void mostrarError(String mensaje) {
        lblError.setText("⚠ " + mensaje);
        lblError.setVisible(true);
    }

    public void limpiarError() {
        lblError.setVisible(false);
    }

    // Para cargar datos en modo edición
    public void setDatos(String nombre, String sku, String codBarras,
                         String unidad, int stock, String estado) {
        txtNombre.setText(nombre);
        txtSKU.setText(sku);
        txtCodigoBarras.setText(codBarras);
        cmbUnidadMedida.setSelectedItem(unidad);
        txtStock.setText(String.valueOf(stock));
        if (cmbEstado != null) cmbEstado.setSelectedItem(estado);
    }
}