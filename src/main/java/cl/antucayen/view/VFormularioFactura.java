package cl.antucayen.view;

import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.model.service.ServicioExtraccionFacturaDigital.ItemExtraido;
import cl.antucayen.view.components.ComponentesSwing;
import cl.antucayen.view.components.SelectorFecha;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;

public class VFormularioFactura extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private JTextField txtNumero;
    private JTextField txtFecha;
    private JComboBox<ProveedorItem> cmbProveedor;
    private JLabel lblRutProveedor;
    private JTextField txtValorTotal;
    private JComboBox<String> cmbModalidad;
    private JTextField txtArchivo;
    private JButton btnSeleccionarArchivo;
    private JButton btnVistaPrevia;
    private JButton btnExtraerItems;
    private JTable tblItems;
    private DefaultTableModel modeloItems;
    private JButton btnAgregarProducto;
    private JButton btnQuitarProducto;
    private JButton btnGuardar;
    private JButton btnCancelar;
    private JLabel lblError;
    private File archivoSeleccionado;

    public VFormularioFactura(JFrame parent) {
        super(parent, "Registrar Factura", true);
        initComponents();
    }

    private void initComponents() {
        setSize(860, 720);
        setLocationRelativeTo(getParent());
        setResizable(true);
        setMinimumSize(new Dimension(780, 650));
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel("Registrar / Procesar Factura");
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

        gbc.gridx = 0; gbc.gridy = 0; form.add(crearLabel("Proveedor (nombre) *"), gbc);
        gbc.gridx = 1; form.add(crearLabel("RUT proveedor"), gbc);
        cmbProveedor = new JComboBox<>();
        cmbProveedor.setPreferredSize(new Dimension(0, 32));
        cmbProveedor.addActionListener(e -> actualizarRutProveedor());
        lblRutProveedor = crearValorEnmarcado();
        gbc.gridx = 0; gbc.gridy = 1; form.add(cmbProveedor, gbc);
        gbc.gridx = 1; form.add(lblRutProveedor, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(crearLabel("N° de factura *"), gbc);
        gbc.gridx = 1; form.add(crearLabel("Valor total de la factura *"), gbc);
        txtNumero = crearCampo();
        txtValorTotal = crearCampo();
        txtValorTotal.setToolTipText("Monto total, solo números enteros (ej: 45000)");
        gbc.gridx = 0; gbc.gridy = 3; form.add(txtNumero, gbc);
        gbc.gridx = 1; form.add(txtValorTotal, gbc);

        gbc.gridx = 0; gbc.gridy = 4; form.add(crearLabel("Fecha de emisión * (dd-mm-aaaa)"), gbc);
        gbc.gridx = 1; form.add(crearLabel("Modalidad de ingreso de ítems *"), gbc);

        txtFecha = crearCampo();
        txtFecha.setText(FORMATO_FECHA.format(LocalDate.now()));
        JButton btnCalendario = new JButton("Calendario");
        btnCalendario.setFont(new Font("Arial", Font.PLAIN, 12));
        btnCalendario.setFocusPainted(false);
        btnCalendario.addActionListener(e -> SelectorFecha.mostrar(this, txtFecha, FORMATO_FECHA));
        JPanel panelFecha = new JPanel(new BorderLayout(6, 0));
        panelFecha.setBackground(Color.WHITE);
        panelFecha.add(txtFecha, BorderLayout.CENTER);
        panelFecha.add(btnCalendario, BorderLayout.EAST);

        cmbModalidad = new JComboBox<>(new String[]{"Ingreso manual", "Archivo digital (PDF/JPG/PNG)"});
        cmbModalidad.setPreferredSize(new Dimension(0, 32));
        cmbModalidad.addActionListener(e -> actualizarModalidad());
        gbc.gridx = 0; gbc.gridy = 5; form.add(panelFecha, gbc);
        gbc.gridx = 1; form.add(cmbModalidad, gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy = 6; form.add(crearLabel("Archivo digital"), gbc);
        JPanel panelArchivo = new JPanel(new BorderLayout(6, 0));
        panelArchivo.setBackground(Color.WHITE);
        txtArchivo = crearCampo();
        txtArchivo.setEditable(false);
        txtArchivo.setText("Sin archivo adjunto");
        btnSeleccionarArchivo = new JButton("Seleccionar");
        btnVistaPrevia = new JButton("Vista previa");
        btnExtraerItems = new JButton("Extraer ítems");
        JPanel accionesArchivo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        accionesArchivo.setBackground(Color.WHITE);
        accionesArchivo.add(btnSeleccionarArchivo);
        accionesArchivo.add(btnVistaPrevia);
        accionesArchivo.add(btnExtraerItems);
        panelArchivo.add(txtArchivo, BorderLayout.CENTER);
        panelArchivo.add(accionesArchivo, BorderLayout.EAST);
        gbc.gridy = 7; form.add(panelArchivo, gbc);

        lblError = new JLabel(" ");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 38, 38));
        gbc.gridy = 8; form.add(lblError, gbc);

        JPanel panelItems = new JPanel(new BorderLayout(0, 6));
        panelItems.setBackground(Color.WHITE);
        panelItems.setBorder(BorderFactory.createEmptyBorder(0, 24, 8, 24));

        JPanel topItems = new JPanel(new BorderLayout());
        topItems.setBackground(Color.WHITE);
        JLabel lblItems = new JLabel("Ítems de la factura");
        lblItems.setFont(new Font("Arial", Font.BOLD, 13));
        lblItems.setForeground(new Color(17, 24, 39));

        JPanel botonesItems = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botonesItems.setBackground(Color.WHITE);
        btnAgregarProducto = ComponentesSwing.crearBoton("+ Agregar ítem", new Color(5, 150, 105));
        btnQuitarProducto = ComponentesSwing.crearBoton("Quitar", new Color(220, 38, 38));
        botonesItems.add(btnAgregarProducto);
        botonesItems.add(btnQuitarProducto);
        topItems.add(lblItems, BorderLayout.WEST);
        topItems.add(botonesItems, BorderLayout.EAST);

        String[] colsItems = {"Código proveedor", "Descripción", "Cantidad", "Estado"};
        modeloItems = new DefaultTableModel(colsItems, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column < 3;
            }
        };
        tblItems = ComponentesSwing.crearTabla(modeloItems);
        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.setPreferredSize(new Dimension(0, 260));
        panelItems.add(topItems, BorderLayout.NORTH);
        panelItems.add(scrollItems, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Color.WHITE);
        centro.add(form, BorderLayout.NORTH);
        centro.add(panelItems, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar = new JButton("Guardar factura");
        btnGuardar.setBackground(new Color(5, 150, 105));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        add(header, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        btnAgregarProducto.addActionListener(e ->
                modeloItems.addRow(new Object[]{"", "", "", "Observado"}));
        btnQuitarProducto.addActionListener(e -> {
            int fila = tblItems.getSelectedRow();
            if (fila >= 0) modeloItems.removeRow(fila);
        });
        actualizarModalidad();
    }

    private void actualizarModalidad() {
        boolean digital = esModalidadDigital();
        btnSeleccionarArchivo.setEnabled(digital);
        btnVistaPrevia.setEnabled(digital && archivoSeleccionado != null);
        btnExtraerItems.setEnabled(digital && archivoSeleccionado != null);
        if (!digital) {
            archivoSeleccionado = null;
            txtArchivo.setText("Sin archivo adjunto");
        }
    }

    public void setArchivoSeleccionado(File archivo) {
        this.archivoSeleccionado = archivo;
        txtArchivo.setText(archivo == null ? "Sin archivo adjunto" : archivo.getAbsolutePath());
        btnVistaPrevia.setEnabled(esModalidadDigital() && archivo != null);
        btnExtraerItems.setEnabled(esModalidadDigital() && archivo != null);
    }

    public void cargarItemsExtraidos(List<ItemExtraido> items) {
        modeloItems.setRowCount(0);
        for (ItemExtraido item : items) {
            modeloItems.addRow(new Object[]{
                    item.codigoInterno() == null ? "" : item.codigoInterno(),
                    item.descripcion() == null ? "" : item.descripcion(),
                    item.cantidad(),
                    item.estado()
            });
        }
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

    private JLabel crearValorEnmarcado() {
        JLabel l = new JLabel("—");
        l.setFont(new Font("Arial", Font.PLAIN, 13));
        l.setForeground(new Color(17, 24, 39));
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return l;
    }

    private JTextField crearCampo() {
        JTextField t = new JTextField();
        t.setFont(new Font("Arial", Font.PLAIN, 13));
        t.setPreferredSize(new Dimension(0, 32));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return t;
    }

    public void cargarProveedores(List<Proveedor> proveedores) {
        cmbProveedor.removeAllItems();
        for (Proveedor p : proveedores) {
            cmbProveedor.addItem(new ProveedorItem(p.getIdProveedor(), p.getNombre(), p.getRut()));
        }
        actualizarRutProveedor();
    }

    public String getNumero() { return txtNumero.getText().trim(); }
    public String getFechaTexto() { return txtFecha.getText().trim(); }
    public String getValorTotalTexto() { return txtValorTotal.getText().trim(); }
    public boolean esModalidadDigital() { return cmbModalidad.getSelectedIndex() == 1; }
    public File getArchivoSeleccionado() { return archivoSeleccionado; }

    public int getIdProveedorSeleccionado() {
        ProveedorItem item = (ProveedorItem) cmbProveedor.getSelectedItem();
        return item != null ? item.id : -1;
    }

    public DefaultTableModel getModeloItems() { return modeloItems; }
    public JButton getBtnGuardar() { return btnGuardar; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnSeleccionarArchivo() { return btnSeleccionarArchivo; }
    public JButton getBtnVistaPrevia() { return btnVistaPrevia; }
    public JButton getBtnExtraerItems() { return btnExtraerItems; }

    public void mostrarError(String msg) {
        lblError.setText(msg == null || msg.isBlank() ? " " : msg);
    }

    public void limpiarError() { lblError.setText(" "); }

    private static class ProveedorItem {
        final int id;
        final String nombre;
        final String rut;

        ProveedorItem(int id, String nombre, String rut) {
            this.id = id;
            this.nombre = nombre;
            this.rut = rut;
        }

        @Override
        public String toString() { return nombre; }
    }
}
