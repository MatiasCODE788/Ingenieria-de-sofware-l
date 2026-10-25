package cl.antucayen.view;

import cl.antucayen.model.domain.EstadoFactura;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.view.components.ComponentesSwing;
import cl.antucayen.view.components.EstiloAntucayen;
import cl.antucayen.view.components.SelectorFecha;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.util.List;
import java.time.format.DateTimeFormatter;

public class VFacturas extends JPanel {

    private static final DateTimeFormatter FORMATO_FILTRO_FECHA = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private JTextField        txtFiltroNumero;
    private JComboBox<ProveedorItem> cmbFiltroProveedor;
    private JTextField        txtFiltroDesde;
    private JTextField        txtFiltroHasta;
    private JComboBox<String> cmbFiltroEstado;
    private JButton           btnBuscar;
    private JButton           btnLimpiar;
    private JButton           btnNueva;
    private JTable            tblFacturas;
    private DefaultTableModel modeloTabla;

    public VFacturas() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 10));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        txtFiltroNumero = crearCampoFiltro(110);
        cmbFiltroProveedor = new JComboBox<>();
        cmbFiltroProveedor.setPreferredSize(new Dimension(160, 32));
        txtFiltroDesde = crearCampoFiltro(100);
        txtFiltroDesde.setToolTipText("dd-mm-aaaa");
        txtFiltroHasta = crearCampoFiltro(100);
        txtFiltroHasta.setToolTipText("dd-mm-aaaa");
        cmbFiltroEstado = new JComboBox<>(new String[]{
                "Todos",
                EstadoFactura.PENDIENTE.valorDb(),
                EstadoFactura.PROCESADA.valorDb(),
                EstadoFactura.OBSERVADA.valorDb()
        });
        cmbFiltroEstado.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbFiltroEstado.setPreferredSize(new Dimension(120, 32));

        btnBuscar  = ComponentesSwing.crearBoton("Buscar",     new Color(37, 99, 235));
        btnLimpiar = ComponentesSwing.crearBoton("Limpiar",    new Color(107, 114, 128));
        btnNueva   = ComponentesSwing.crearBoton("+ Nueva factura", new Color(5, 150, 105));
        btnBuscar.setPreferredSize(new Dimension(90, 32));
        btnLimpiar.setPreferredSize(new Dimension(90, 32));

        barraTop.setLayout(new BorderLayout(0, 1));
        JPanel campos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        campos.setBackground(Color.WHITE);
        campos.add(new JLabel("Número:")); campos.add(txtFiltroNumero);
        campos.add(new JLabel("Proveedor:")); campos.add(cmbFiltroProveedor);
        campos.add(new JLabel("Desde:"));
        campos.add(SelectorFecha.crearCampoConCalendario(txtFiltroDesde, FORMATO_FILTRO_FECHA));
        campos.add(new JLabel("Hasta:"));
        campos.add(SelectorFecha.crearCampoConCalendario(txtFiltroHasta, FORMATO_FILTRO_FECHA));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        acciones.setBackground(Color.WHITE);
        acciones.add(new JLabel("Estado:")); acciones.add(cmbFiltroEstado);
        acciones.add(btnBuscar); acciones.add(btnLimpiar); acciones.add(btnNueva);
        barraTop.add(campos, BorderLayout.NORTH);
        barraTop.add(acciones, BorderLayout.SOUTH);

        String[] cols = {"ID", "Número", "Fecha", "Proveedor", "Estado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblFacturas = ComponentesSwing.crearTabla(modeloTabla);
        tblFacturas.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblFacturas.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblFacturas.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblFacturas.getColumnModel().getColumn(3).setPreferredWidth(240);
        tblFacturas.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(tblFacturas);
        JLabel lblAyuda = new JLabel("  Doble clic sobre una factura para ver su detalle y procesarla");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));

        JPanel listado = new JPanel(new BorderLayout(0, 8));
        listado.setOpaque(false);
        listado.add(scroll, BorderLayout.CENTER);
        listado.add(lblAyuda, BorderLayout.SOUTH);
        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listado, crearResumenFactura());
        divisor.setResizeWeight(0.62);
        divisor.setDividerSize(12);
        divisor.setBorder(null);
        SwingUtilities.invokeLater(() -> divisor.setDividerLocation(0.62));
        add(barraTop, BorderLayout.NORTH);
        add(divisor, BorderLayout.CENTER);
    }

    private JPanel crearResumenFactura() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloAntucayen.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JLabel titulo = new JLabel("Detalle de la factura");
        titulo.setFont(new Font("Dialog", Font.BOLD, 14));
        titulo.setForeground(EstiloAntucayen.TEXT);
        panel.add(titulo, BorderLayout.NORTH);
        String[] campos = {"Número de factura", "Fecha de emisión", "Proveedor", "Estado"};
        JLabel[] valores = new JLabel[4];
        JPanel resumen = new JPanel(new GridLayout(0, 1, 0, 10));
        resumen.setBackground(Color.WHITE);
        for (int i=0;i<campos.length;i++) {
            JPanel bloque = new JPanel(new BorderLayout(0, 4));
            bloque.setBackground(Color.WHITE);
            JLabel etiqueta = new JLabel(campos[i]);
            etiqueta.setForeground(EstiloAntucayen.TEXT);
            etiqueta.setFont(new Font("Dialog", Font.BOLD, 11));
            valores[i] = new JLabel("—");
            valores[i].setForeground(EstiloAntucayen.MUTED);
            valores[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(EstiloAntucayen.BORDER),
                    BorderFactory.createEmptyBorder(8, 9, 8, 9)));
            bloque.add(etiqueta, BorderLayout.NORTH);
            bloque.add(valores[i], BorderLayout.CENTER);
            resumen.add(bloque);
        }
        panel.add(resumen, BorderLayout.CENTER);
        JButton detalle = ComponentesSwing.crearBoton("Ver factura completa", EstiloAntucayen.primario());
        detalle.addActionListener(e -> {
            int fila = tblFacturas.getSelectedRow();
            if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona una factura de la lista."); return; }
            java.awt.event.MouseEvent doble = new java.awt.event.MouseEvent(tblFacturas,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    5, tblFacturas.getCellRect(fila, 0, true).y + 5, 2, false);
            for (java.awt.event.MouseListener listener : tblFacturas.getMouseListeners())
                listener.mouseClicked(doble);
        });
        tblFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int fila = tblFacturas.getSelectedRow();
            int[] columnas = {1, 2, 3, 4};
            for (int i=0;i<valores.length;i++) {
                Object v = fila >= 0 ? tblFacturas.getValueAt(fila, columnas[i]) : null;
                valores[i].setText(v == null ? "—" : String.valueOf(v));
            }
        });
        panel.add(detalle, BorderLayout.SOUTH);
        return panel;
    }

    private JTextField crearCampoFiltro(int ancho) {
        JTextField t = new JTextField();
        t.setFont(new Font("Arial", Font.PLAIN, 13));
        t.setPreferredSize(new Dimension(ancho, 32));
        return t;
    }

    public void cargarProveedores(List<Proveedor> proveedores) {
        cmbFiltroProveedor.removeAllItems();
        cmbFiltroProveedor.addItem(new ProveedorItem(-1, "Todos"));
        for (Proveedor p : proveedores)
            cmbFiltroProveedor.addItem(new ProveedorItem(p.getIdProveedor(), p.getNombre()));
    }

    public String  getFiltroNumero()  { return txtFiltroNumero.getText().trim(); }
    public String  getFiltroDesde()   { return txtFiltroDesde.getText().trim(); }
    public String  getFiltroHasta()   { return txtFiltroHasta.getText().trim(); }
    public String  getFiltroEstado()  { return (String) cmbFiltroEstado.getSelectedItem(); }

    public int getFiltroIdProveedor() {
        ProveedorItem item = (ProveedorItem) cmbFiltroProveedor.getSelectedItem();
        return item != null ? item.id : -1;
    }

    public void limpiarFiltros() {
        txtFiltroNumero.setText("");
        txtFiltroDesde.setText("");
        txtFiltroHasta.setText("");
        cmbFiltroEstado.setSelectedIndex(0);
        cmbFiltroProveedor.setSelectedIndex(0);
    }

    public JButton getBtnBuscar()     { return btnBuscar; }
    public JButton getBtnLimpiar()    { return btnLimpiar; }
    public JButton getBtnNueva()      { return btnNueva; }
    public JTable  getTblFacturas()   { return tblFacturas; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }

    private static class ProveedorItem {
        final int id;
        final String nombre;
        ProveedorItem(int id, String nombre) { this.id = id; this.nombre = nombre; }
        @Override public String toString() { return nombre; }
    }
}
