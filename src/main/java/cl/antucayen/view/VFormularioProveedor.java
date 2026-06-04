package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VFormularioProveedor extends JDialog {

    private JTextField        txtRut;
    private JTextField        txtNombre;
    private JTextField        txtTelefono;
    private JTextField        txtCorreo;
    private JTable            tblEquivalencias;
    private DefaultTableModel modeloEquiv;
    private JButton           btnAgregarEquiv;
    private JButton           btnEliminarEquiv;
    private JButton           btnGuardar;
    private JButton           btnCancelar;
    private JLabel            lblError;

    public VFormularioProveedor(JFrame parent, boolean modoEdicion) {
        super(parent, modoEdicion ? "Editar Proveedor" : "Nuevo Proveedor", true);
        initComponents(modoEdicion);
    }

    private void initComponents(boolean modoEdicion) {
        setSize(560, 520);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel(modoEdicion ? "✏️  Editar Proveedor" : "🏭  Nuevo Proveedor");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        header.add(lblTitulo);

        // Form datos
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 8);
        gbc.weightx = 0.5;

        gbc.gridx = 0; gbc.gridy = 0; form.add(crearLabel("RUT *"), gbc);
        gbc.gridx = 1;               form.add(crearLabel("Nombre *"), gbc);
        txtRut    = crearCampo(); if (modoEdicion) txtRut.setEditable(false);
        txtNombre = crearCampo();
        gbc.gridx = 0; gbc.gridy = 1; form.add(txtRut, gbc);
        gbc.gridx = 1;               form.add(txtNombre, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(crearLabel("Teléfono"), gbc);
        gbc.gridx = 1;               form.add(crearLabel("Correo electrónico"), gbc);
        txtTelefono = crearCampo();
        txtCorreo   = crearCampo();
        gbc.gridx = 0; gbc.gridy = 3; form.add(txtTelefono, gbc);
        gbc.gridx = 1;               form.add(txtCorreo, gbc);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 4;
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        form.add(lblError, gbc);

        // Equivalencias
        JPanel panelEquiv = new JPanel(new BorderLayout(0, 6));
        panelEquiv.setBackground(Color.WHITE);
        panelEquiv.setBorder(BorderFactory.createEmptyBorder(0, 24, 8, 24));

        JPanel topEquiv = new JPanel(new BorderLayout());
        topEquiv.setBackground(Color.WHITE);
        JLabel lblEquiv = new JLabel("Equivalencias de códigos");
        lblEquiv.setFont(new Font("Arial", Font.BOLD, 13));
        lblEquiv.setForeground(new Color(17, 24, 39));

        JPanel botonesEquiv = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botonesEquiv.setBackground(Color.WHITE);
        btnAgregarEquiv  = VBuscadorProductos.crearBoton("+ Agregar",  new Color(5, 150, 105));
        btnEliminarEquiv = VBuscadorProductos.crearBoton("🗑 Eliminar", new Color(220, 38, 38));
        btnAgregarEquiv.setPreferredSize(new Dimension(110, 28));
        btnEliminarEquiv.setPreferredSize(new Dimension(110, 28));
        botonesEquiv.add(btnAgregarEquiv);
        botonesEquiv.add(btnEliminarEquiv);

        topEquiv.add(lblEquiv,     BorderLayout.WEST);
        topEquiv.add(botonesEquiv, BorderLayout.EAST);

        String[] colsEquiv = {"Código proveedor", "SKU interno"};
        modeloEquiv = new DefaultTableModel(colsEquiv, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblEquivalencias = VBuscadorProductos.crearTabla(modeloEquiv);
        JScrollPane scrollEquiv = new JScrollPane(tblEquivalencias);
        scrollEquiv.setPreferredSize(new Dimension(0, 110));

        panelEquiv.add(topEquiv,    BorderLayout.NORTH);
        panelEquiv.add(scrollEquiv, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Color.WHITE);
        centro.add(form,       BorderLayout.NORTH);
        centro.add(panelEquiv, BorderLayout.CENTER);

        // Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setForeground(new Color(30, 41, 59));
        btnCancelar.setFocusPainted(false);
        btnCancelar.addActionListener(e -> dispose());

        btnGuardar = new JButton(modoEdicion ? "Guardar cambios" : "Registrar proveedor");
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

    public String getRut()     { return txtRut.getText().trim(); }
    public String getNombre()  { return txtNombre.getText().trim(); }
    public String getTelefono(){ return txtTelefono.getText().trim(); }
    public String getCorreo()  { return txtCorreo.getText().trim(); }

    public JButton getBtnGuardar()        { return btnGuardar; }
    public JButton getBtnCancelar()       { return btnCancelar; }
    public JButton getBtnAgregarEquiv()   { return btnAgregarEquiv; }
    public JButton getBtnEliminarEquiv()  { return btnEliminarEquiv; }
    public JTable  getTblEquivalencias()  { return tblEquivalencias; }
    public DefaultTableModel getModeloEquiv() { return modeloEquiv; }

    public void mostrarError(String msg) { lblError.setText("⚠ " + msg); lblError.setVisible(true); }
    public void limpiarError()           { lblError.setVisible(false); }

    public void setDatos(String rut, String nombre, String tel, String correo) {
        txtRut.setText(rut);
        txtNombre.setText(nombre);
        txtTelefono.setText(tel);
        txtCorreo.setText(correo);
    }

    public void agregarEquivalencia(String codigo, String sku) {
        modeloEquiv.addRow(new Object[]{codigo, sku});
    }

    public int getFilaEquivSeleccionada() {
        return tblEquivalencias.getSelectedRow();
    }
}