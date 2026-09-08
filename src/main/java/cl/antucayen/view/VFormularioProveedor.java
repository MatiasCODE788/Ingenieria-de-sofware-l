package cl.antucayen.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VFormularioProveedor extends JDialog {

    private JTextField        txtRut;
    private JTextField        txtNombre;
    private JTextField        txtTelefono;
    private JTextField        txtCorreo;
    private JTable            tblEquivalencias;
    private DefaultTableModel modeloEquiv;
    private JButton           btnAgregarEquiv;
    private JButton           btnEditarEquiv;
    private JButton           btnEliminarEquiv;
    private JButton           btnGuardar;
    private JButton           btnCancelar;
    private JLabel            lblError;

    public VFormularioProveedor(JFrame parent, boolean modoEdicion) {
        super(parent, modoEdicion ? "Editar Proveedor" : "Nuevo Proveedor", true);
        initComponents(modoEdicion);
    }

    private void initComponents(boolean modoEdicion) {
        setSize(560, 540);
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
        btnEditarEquiv   = VBuscadorProductos.crearBoton("✎ Editar",   new Color(37, 99, 235));
        btnEliminarEquiv = VBuscadorProductos.crearBoton("🗑 Eliminar", new Color(220, 38, 38));
        btnAgregarEquiv.setPreferredSize(new Dimension(100, 28));
        btnEditarEquiv.setPreferredSize(new Dimension(90, 28));
        btnEliminarEquiv.setPreferredSize(new Dimension(100, 28));
        botonesEquiv.add(btnAgregarEquiv);
        botonesEquiv.add(btnEditarEquiv);
        botonesEquiv.add(btnEliminarEquiv);

        topEquiv.add(lblEquiv,     BorderLayout.WEST);
        topEquiv.add(botonesEquiv, BorderLayout.EAST);

        String[] colsEquiv = {"Código proveedor", "SKU interno"};
        modeloEquiv = new DefaultTableModel(colsEquiv, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblEquivalencias = VBuscadorProductos.crearTabla(modeloEquiv);
        JScrollPane scrollEquiv = new JScrollPane(tblEquivalencias);
        scrollEquiv.setPreferredSize(new Dimension(0, 130));

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
    public JButton getBtnEditarEquiv()    { return btnEditarEquiv; }
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

    public void actualizarFilaEquivalencia(int fila, String codigo, String sku) {
        modeloEquiv.setValueAt(codigo, fila, 0);
        modeloEquiv.setValueAt(sku,    fila, 1);
    }

    public int getFilaEquivSeleccionada() {
        return tblEquivalencias.getSelectedRow();
    }

    /**
     * Abre un sub-diálogo modal para agregar o editar una equivalencia
     * (código interno + SKU), con validación visual en vivo de duplicados
     * contra las equivalencias ya cargadas en la tabla.
     *
     * @param codigoInicial código a precargar (edición) o null (nuevo)
     * @param skuInicial    SKU a precargar (edición) o null (nuevo)
     * @param codigosExistentes lista de códigos ya registrados (para detectar duplicado)
     * @return String[]{codigo, sku} si el usuario confirma, o null si cancela
     */
    public String[] mostrarDialogoEquivalencia(String codigoInicial, String skuInicial,
                                               List<String> codigosExistentes) {
        JTextField txtCodigo = crearCampo();
        JTextField txtSku    = crearCampo();
        if (codigoInicial != null) txtCodigo.setText(codigoInicial);
        if (skuInicial != null)    txtSku.setText(skuInicial);

        JLabel lblDuplicado = new JLabel(" ");
        lblDuplicado.setFont(new Font("Arial", Font.BOLD, 11));
        lblDuplicado.setForeground(new Color(220, 38, 38));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0; gbc.gridy = 0; panel.add(crearLabel("Código interno del proveedor *"), gbc);
        gbc.gridy = 1; panel.add(txtCodigo, gbc);
        gbc.gridy = 2; panel.add(crearLabel("SKU interno del sistema *"), gbc);
        gbc.gridy = 3; panel.add(txtSku, gbc);
        gbc.gridy = 4; panel.add(lblDuplicado, gbc);

        JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialogo = pane.createDialog(this,
                codigoInicial == null ? "Nueva equivalencia" : "Editar equivalencia");

        // Validación visual en vivo: código duplicado (ignorando el propio código en edición)
        DocumentListener validador = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { validar(); }
            public void removeUpdate(DocumentEvent e)  { validar(); }
            public void changedUpdate(DocumentEvent e) { validar(); }
            private void validar() {
                String codigoActual = txtCodigo.getText().trim();
                boolean duplicado = codigosExistentes != null && codigosExistentes.stream()
                        .anyMatch(c -> c.equalsIgnoreCase(codigoActual)
                                && !c.equalsIgnoreCase(codigoInicial == null ? "" : codigoInicial));
                if (duplicado) {
                    lblDuplicado.setText("⚠ Ya existe una equivalencia con ese código para este proveedor");
                    txtCodigo.setBorder(BorderFactory.createLineBorder(new Color(220, 38, 38), 2));
                } else {
                    lblDuplicado.setText(" ");
                    txtCodigo.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(203, 213, 225)),
                            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                }
            }
        };
        txtCodigo.getDocument().addDocumentListener(validador);

        dialogo.setVisible(true);

        Object seleccion = pane.getValue();
        if (seleccion == null || (int) seleccion != JOptionPane.OK_OPTION) return null;

        String codigo = txtCodigo.getText().trim();
        String sku    = txtSku.getText().trim();
        if (codigo.isEmpty() || sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Código y SKU son obligatorios");
            return null;
        }
        boolean duplicado = codigosExistentes != null && codigosExistentes.stream()
                .anyMatch(c -> c.equalsIgnoreCase(codigo)
                        && !c.equalsIgnoreCase(codigoInicial == null ? "" : codigoInicial));
        if (duplicado) {
            JOptionPane.showMessageDialog(this,
                    "Ya existe una equivalencia con ese código para este proveedor");
            return null;
        }
        return new String[]{codigo, sku};
    }
}
