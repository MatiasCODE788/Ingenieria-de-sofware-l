package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VFormularioProveedor extends JDialog {

    private JTextField txtNombre;
    private JTextField txtTelefono;
    private JTextField txtCorreo;
    private JTable tblEquivalencias;
    private DefaultTableModel modeloEquivalencias;
    private JButton btnAgregarEquivalencia;
    private JButton btnEditarEquivalencia;
    private JButton btnEliminarEquivalencia;
    private JButton btnGuardar;
    private JButton btnCancelar;
    private JLabel lblError;

    public VFormularioProveedor(JFrame parent, boolean modoEdicion) {
        super(parent, modoEdicion ? "Editar Proveedor" : "Nuevo Proveedor", true);
        initComponents(modoEdicion);
    }

    private void initComponents(boolean modoEdicion) {
        setSize(560, 540);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        // ── HEADER ────────────────────────────────────────────────
        JPanel panelHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        panelHeader.setBackground(new Color(30, 48, 84));
        JLabel lblTitulo = new JLabel(modoEdicion ? "✏️  Editar Proveedor" : "🏭  Nuevo Proveedor");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        panelHeader.add(lblTitulo);

        // ── FORMULARIO DATOS PROVEEDOR ────────────────────────────
        JPanel panelDatos = new JPanel(new GridBagLayout());
        panelDatos.setBackground(Color.WHITE);
        panelDatos.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 8);

        // Nombre (ocupa toda la fila)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panelDatos.add(crearLabel("Nombre del proveedor *"), gbc);
        gbc.gridy = 1;
        txtNombre = crearCampo();
        panelDatos.add(txtNombre, gbc);

        // Teléfono y Correo en la misma fila
        gbc.gridwidth = 1; gbc.weightx = 0.5;
        gbc.gridx = 0; gbc.gridy = 2;
        panelDatos.add(crearLabel("Teléfono"), gbc);
        gbc.gridx = 1;
        panelDatos.add(crearLabel("Correo electrónico"), gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        txtTelefono = crearCampo();
        panelDatos.add(txtTelefono, gbc);
        gbc.gridx = 1;
        txtCorreo = crearCampo();
        panelDatos.add(txtCorreo, gbc);

        // Label error
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        panelDatos.add(lblError, gbc);

        // ── SECCIÓN EQUIVALENCIAS ─────────────────────────────────
        JPanel panelEquiv = new JPanel(new BorderLayout(0, 6));
        panelEquiv.setBackground(Color.WHITE);
        panelEquiv.setBorder(BorderFactory.createEmptyBorder(0, 24, 8, 24));

        JLabel lblEquiv = new JLabel("Equivalencias de códigos");
        lblEquiv.setFont(new Font("Arial", Font.BOLD, 13));
        lblEquiv.setForeground(new Color(30, 48, 84));

        // Botones equivalencias
        JPanel panelBotonesEquiv = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panelBotonesEquiv.setBackground(Color.WHITE);

        btnAgregarEquivalencia  = crearBotonSmall("+ Agregar",  new Color(5, 150, 105));
        btnEditarEquivalencia   = crearBotonSmall("✏️ Editar",   new Color(37, 99, 235));
        btnEliminarEquivalencia = crearBotonSmall("🗑 Eliminar", new Color(220, 38, 38));

        panelBotonesEquiv.add(btnAgregarEquivalencia);
        panelBotonesEquiv.add(btnEditarEquivalencia);
        panelBotonesEquiv.add(btnEliminarEquivalencia);

        JPanel panelHeaderEquiv = new JPanel(new BorderLayout());
        panelHeaderEquiv.setBackground(Color.WHITE);
        panelHeaderEquiv.add(lblEquiv, BorderLayout.WEST);
        panelHeaderEquiv.add(panelBotonesEquiv, BorderLayout.EAST);

        // Tabla equivalencias
        String[] columnas = {"Código interno proveedor", "SKU sistema"};
        modeloEquivalencias = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblEquivalencias = new JTable(modeloEquivalencias);
        tblEquivalencias.setFont(new Font("Arial", Font.PLAIN, 12));
        tblEquivalencias.setRowHeight(30);
        tblEquivalencias.setGridColor(new Color(220, 227, 237));
        tblEquivalencias.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));
        tblEquivalencias.getTableHeader().setBackground(new Color(30, 48, 84));
        tblEquivalencias.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollEquiv = new JScrollPane(tblEquivalencias);
        scrollEquiv.setPreferredSize(new Dimension(0, 130));
        scrollEquiv.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));

        panelEquiv.add(panelHeaderEquiv, BorderLayout.NORTH);
        panelEquiv.add(scrollEquiv, BorderLayout.CENTER);

        // ── PANEL CENTRAL (datos + equivalencias) ─────────────────
        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setBackground(Color.WHITE);
        panelCentro.add(panelDatos, BorderLayout.NORTH);
        panelCentro.add(panelEquiv, BorderLayout.CENTER);

        // ── BOTONES ───────────────────────────────────────────────
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        panelBotones.setBackground(new Color(248, 250, 252));
        panelBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 227, 237)));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setForeground(new Color(30, 41, 59));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());

        btnGuardar = new JButton(modoEdicion ? "Guardar cambios" : "Registrar proveedor");
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 12));
        btnGuardar.setBackground(new Color(5, 150, 105));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);

        add(panelHeader, BorderLayout.NORTH);
        add(panelCentro, BorderLayout.CENTER);
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

    private JButton crearBotonSmall(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 28));
        return btn;
    }

    public String getNombre()   { return txtNombre.getText().trim(); }
    public String getTelefono() { return txtTelefono.getText().trim(); }
    public String getCorreo()   { return txtCorreo.getText().trim(); }

    public JButton getBtnGuardar()              { return btnGuardar; }
    public JButton getBtnCancelar()             { return btnCancelar; }
    public JButton getBtnAgregarEquivalencia()  { return btnAgregarEquivalencia; }
    public JButton getBtnEditarEquivalencia()   { return btnEditarEquivalencia; }
    public JButton getBtnEliminarEquivalencia() { return btnEliminarEquivalencia; }
    public JTable getTblEquivalencias()         { return tblEquivalencias; }
    public DefaultTableModel getModeloEquivalencias() { return modeloEquivalencias; }

    public void mostrarError(String msg) {
        lblError.setText("⚠ " + msg);
        lblError.setVisible(true);
    }

    public void limpiarError() { lblError.setVisible(false); }

    public void agregarEquivalencia(String codigo, String sku) {
        modeloEquivalencias.addRow(new Object[]{codigo, sku});
    }

    public void setDatos(String nombre, String telefono, String correo) {
        txtNombre.setText(nombre);
        txtTelefono.setText(telefono);
        txtCorreo.setText(correo);
    }
}