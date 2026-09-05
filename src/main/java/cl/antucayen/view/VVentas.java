package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VVentas extends JPanel {

    private JTextField        txtBusqueda;
    private JButton           btnAgregar;
    private JTable            tblCarrito;
    private DefaultTableModel modeloCarrito;
    private JLabel            lblTotal;
    private JButton           btnCobrar;
    private JButton           btnLimpiarCarrito;

    // Pago (único o dividido entre varios medios)
    private JTextField        txtPagoEfectivo;
    private JTextField        txtPagoDebito;
    private JTextField        txtPagoCredito;
    private JButton            btnChipEfectivo;
    private JButton            btnChipDebito;
    private JButton            btnChipCredito;
    private JButton            btnLimpiarPagos;
    private JLabel             lblRestante;

    private JTable            tblVentasDia;
    private DefaultTableModel modeloVentasDia;
    private JLabel            lblResumenDia;

    public VVentas() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(243, 244, 246));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.68);
        split.setBorder(null);
        split.setDividerSize(6);

        split.setTopComponent(crearPanelVenta());
        split.setBottomComponent(crearPanelVentasDia());

        add(split, BorderLayout.CENTER);
    }

    private JPanel crearPanelVenta() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JPanel barraBusqueda = new JPanel(new BorderLayout(8, 0));
        barraBusqueda.setBackground(Color.WHITE);

        txtBusqueda = new JTextField();
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 15));
        txtBusqueda.setPreferredSize(new Dimension(0, 40));
        txtBusqueda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        txtBusqueda.setToolTipText("Escanea o escribe SKU, código de barras o nombre del producto");

        btnAgregar = VBuscadorProductos.crearBoton("+ Agregar", new Color(5, 150, 105));
        btnAgregar.setPreferredSize(new Dimension(130, 40));

        JLabel lblTitulo = new JLabel("🛒  Punto de Venta");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitulo.setForeground(new Color(17, 24, 39));

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setBackground(Color.WHITE);
        norte.add(lblTitulo, BorderLayout.NORTH);

        barraBusqueda.add(txtBusqueda, BorderLayout.CENTER);
        barraBusqueda.add(btnAgregar,  BorderLayout.EAST);
        norte.add(barraBusqueda, BorderLayout.CENTER);

        String[] cols = {"SKU", "Producto", "Precio unit.", "Cantidad", "Subtotal"};
        modeloCarrito = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 3; }
            public Class<?> getColumnClass(int c) { return c == 3 ? Integer.class : Object.class; }
        };
        tblCarrito = new JTable(modeloCarrito);
        tblCarrito.setFont(new Font("Arial", Font.PLAIN, 14));
        tblCarrito.setRowHeight(34);
        tblCarrito.setGridColor(new Color(229, 231, 235));
        tblCarrito.setSelectionBackground(new Color(219, 234, 254));
        tblCarrito.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblCarrito.getTableHeader().setBackground(new Color(17, 24, 39));
        tblCarrito.getTableHeader().setForeground(Color.WHITE);
        tblCarrito.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblCarrito.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblCarrito.getColumnModel().getColumn(1).setPreferredWidth(280);
        tblCarrito.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblCarrito.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblCarrito.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scrollCarrito = new JScrollPane(tblCarrito);
        scrollCarrito.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));

        JPanel panelCobro = new JPanel();
        panelCobro.setLayout(new BoxLayout(panelCobro, BoxLayout.Y_AXIS));
        panelCobro.setBackground(Color.WHITE);
        panelCobro.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        // Fila 1: Vaciar carrito ................................ Total: $X
        JPanel filaTotal = new JPanel(new BorderLayout());
        filaTotal.setBackground(Color.WHITE);

        JPanel izqCobro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        izqCobro.setBackground(Color.WHITE);
        btnLimpiarCarrito = VBuscadorProductos.crearBoton("🗑 Vaciar carrito", new Color(107, 114, 128));
        izqCobro.add(btnLimpiarCarrito);

        JPanel derTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        derTotal.setBackground(Color.WHITE);
        lblTotal = new JLabel("Total: $0");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 24));
        lblTotal.setForeground(new Color(5, 150, 105));
        derTotal.add(lblTotal);

        filaTotal.add(izqCobro,  BorderLayout.WEST);
        filaTotal.add(derTotal,  BorderLayout.EAST);

        // Fila 2: panel de pago (único o dividido)
        JPanel panelPago = crearPanelPago();

        panelCobro.add(filaTotal);
        panelCobro.add(Box.createRigidArea(new Dimension(0, 8)));
        panelCobro.add(panelPago);

        p.add(norte, BorderLayout.NORTH);
        p.add(scrollCarrito, BorderLayout.CENTER);
        p.add(panelCobro, BorderLayout.SOUTH);

        return p;
    }

    /** Panel de pago: permite pagar la totalidad con un solo medio o dividir el pago entre varios. */
    private JPanel crearPanelPago() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(249, 250, 251));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel lblTituloPago = new JLabel("Método de pago");
        lblTituloPago.setFont(new Font("Arial", Font.BOLD, 12));
        lblTituloPago.setForeground(new Color(107, 114, 128));

        // Botones rápidos: pagar el 100% con un único medio
        JPanel filaChips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filaChips.setBackground(new Color(249, 250, 251));
        JLabel lblAyudaChips = new JLabel("Pago único: ");
        lblAyudaChips.setFont(new Font("Arial", Font.PLAIN, 12));
        lblAyudaChips.setForeground(new Color(107, 114, 128));
        btnChipEfectivo = VBuscadorProductos.crearBoton("Efectivo", new Color(5, 150, 105));
        btnChipDebito   = VBuscadorProductos.crearBoton("Débito",   new Color(37, 99, 235));
        btnChipCredito  = VBuscadorProductos.crearBoton("Crédito",  new Color(147, 51, 234));
        btnLimpiarPagos = VBuscadorProductos.crearBoton("Limpiar", new Color(156, 163, 175));
        filaChips.add(lblAyudaChips);
        filaChips.add(btnChipEfectivo);
        filaChips.add(btnChipDebito);
        filaChips.add(btnChipCredito);
        filaChips.add(btnLimpiarPagos);

        // Montos editables: permite dividir el pago entre varios medios
        JPanel filaMontos = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        filaMontos.setBackground(new Color(249, 250, 251));
        txtPagoEfectivo = crearCampoMonto();
        txtPagoDebito   = crearCampoMonto();
        txtPagoCredito  = crearCampoMonto();
        filaMontos.add(crearEtiquetaMonto("Efectivo $", txtPagoEfectivo));
        filaMontos.add(crearEtiquetaMonto("Débito $",   txtPagoDebito));
        filaMontos.add(crearEtiquetaMonto("Crédito $",  txtPagoCredito));

        lblRestante = new JLabel("—");
        lblRestante.setFont(new Font("Arial", Font.BOLD, 13));
        lblRestante.setForeground(new Color(107, 114, 128));

        JPanel filaEstado = new JPanel(new BorderLayout());
        filaEstado.setBackground(new Color(249, 250, 251));
        filaEstado.add(lblRestante, BorderLayout.WEST);

        btnCobrar = new JButton("💳  Cobrar");
        btnCobrar.setFont(new Font("Arial", Font.BOLD, 15));
        btnCobrar.setBackground(new Color(5, 150, 105));
        btnCobrar.setForeground(Color.WHITE);
        btnCobrar.setFocusPainted(false);
        btnCobrar.setBorderPainted(false);
        btnCobrar.setPreferredSize(new Dimension(140, 42));
        btnCobrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCobrar.setEnabled(false);

        JPanel derEstado = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        derEstado.setBackground(new Color(249, 250, 251));
        derEstado.add(btnCobrar);
        filaEstado.add(derEstado, BorderLayout.EAST);

        panel.add(lblTituloPago, BorderLayout.NORTH);
        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBackground(new Color(249, 250, 251));
        centro.add(filaChips);
        centro.add(filaMontos);
        panel.add(centro, BorderLayout.CENTER);
        panel.add(filaEstado, BorderLayout.SOUTH);
        return panel;
    }

    private JTextField crearCampoMonto() {
        JTextField txt = new JTextField("0", 8);
        txt.setFont(new Font("Arial", Font.PLAIN, 14));
        txt.setHorizontalAlignment(SwingConstants.RIGHT);
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return txt;
    }

    private JPanel crearEtiquetaMonto(String texto, JTextField campo) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(new Color(249, 250, 251));
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.PLAIN, 13));
        lbl.setForeground(new Color(55, 65, 81));
        p.add(lbl);
        p.add(campo);
        return p;
    }

    private JPanel crearPanelVentasDia() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(10, 20, 16, 20));

        JPanel topVentas = new JPanel(new BorderLayout());
        topVentas.setBackground(Color.WHITE);
        JLabel lbl = new JLabel("📋  Ventas de hoy");
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(new Color(17, 24, 39));
        lblResumenDia = new JLabel("");
        lblResumenDia.setFont(new Font("Arial", Font.PLAIN, 12));
        lblResumenDia.setForeground(new Color(107, 114, 128));
        topVentas.add(lbl, BorderLayout.WEST);
        topVentas.add(lblResumenDia, BorderLayout.EAST);

        String[] cols = {"ID", "Hora", "Vendedor", "Medio de pago", "Total", "Estado"};
        modeloVentasDia = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblVentasDia = VBuscadorProductos.crearTabla(modeloVentasDia);
        JScrollPane scroll = new JScrollPane(tblVentasDia);

        JLabel ayuda = new JLabel("  💡 Doble clic en una venta 'Pagada' para anularla (solo Administrador)");
        ayuda.setFont(new Font("Arial", Font.ITALIC, 11));
        ayuda.setForeground(new Color(107, 114, 128));

        p.add(topVentas, BorderLayout.NORTH);
        p.add(scroll,    BorderLayout.CENTER);
        p.add(ayuda,     BorderLayout.SOUTH);
        return p;
    }

    public String getTextoBusqueda() { return txtBusqueda.getText().trim(); }
    public void   limpiarBusqueda()  { txtBusqueda.setText(""); txtBusqueda.requestFocus(); }

    public JTextField getTxtBusqueda()      { return txtBusqueda; }
    public JButton    getBtnAgregar()       { return btnAgregar; }
    public JButton    getBtnCobrar()        { return btnCobrar; }
    public JButton    getBtnLimpiarCarrito(){ return btnLimpiarCarrito; }
    public JTable      getTblCarrito()      { return tblCarrito; }
    public DefaultTableModel getModeloCarrito() { return modeloCarrito; }

    // --- Pago (único o dividido) ---
    public JTextField getTxtPagoEfectivo() { return txtPagoEfectivo; }
    public JTextField getTxtPagoDebito()   { return txtPagoDebito; }
    public JTextField getTxtPagoCredito()  { return txtPagoCredito; }
    public JButton    getBtnChipEfectivo() { return btnChipEfectivo; }
    public JButton    getBtnChipDebito()   { return btnChipDebito; }
    public JButton    getBtnChipCredito()  { return btnChipCredito; }
    public JButton    getBtnLimpiarPagos() { return btnLimpiarPagos; }

    public int getMontoEfectivo() { return parseMonto(txtPagoEfectivo.getText()); }
    public int getMontoDebito()   { return parseMonto(txtPagoDebito.getText()); }
    public int getMontoCredito()  { return parseMonto(txtPagoCredito.getText()); }

    public void setMontoEfectivo(int monto) { txtPagoEfectivo.setText(String.valueOf(monto)); }
    public void setMontoDebito(int monto)   { txtPagoDebito.setText(String.valueOf(monto)); }
    public void setMontoCredito(int monto)  { txtPagoCredito.setText(String.valueOf(monto)); }

    public void limpiarPagos() {
        txtPagoEfectivo.setText("0");
        txtPagoDebito.setText("0");
        txtPagoCredito.setText("0");
    }

    public void setCobrarHabilitado(boolean habilitado) { btnCobrar.setEnabled(habilitado); }

    /** Actualiza el indicador de "cuánto falta / sobra" respecto del total del carrito. */
    public void setEstadoPago(int restante, int total) {
        if (total <= 0) {
            lblRestante.setText("—");
            lblRestante.setForeground(new Color(107, 114, 128));
        } else if (restante == 0) {
            lblRestante.setText("✓ Pago completo");
            lblRestante.setForeground(new Color(5, 150, 105));
        } else if (restante > 0) {
            lblRestante.setText("Falta $" + formatearMonto(restante) + " por pagar");
            lblRestante.setForeground(new Color(217, 119, 6));
        } else {
            lblRestante.setText("Sobran $" + formatearMonto(-restante) + " en los pagos ingresados");
            lblRestante.setForeground(new Color(220, 38, 38));
        }
    }

    private int parseMonto(String texto) {
        String limpio = texto.replaceAll("[^0-9]", "");
        if (limpio.isEmpty()) return 0;
        try {
            return Integer.parseInt(limpio);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String formatearMonto(int monto) {
        return String.format("%,d", monto).replace(',', '.');
    }

    public void agregarFilaCarrito(Object[] fila) { modeloCarrito.addRow(fila); }
    public void limpiarCarrito()                  { modeloCarrito.setRowCount(0); actualizarTotal(0); }

    public void actualizarTotal(int total) {
        lblTotal.setText("Total: $" + formatearMonto(total));
    }

    public JTable getTblVentasDia() { return tblVentasDia; }
    public DefaultTableModel getModeloVentasDia() { return modeloVentasDia; }
    public void limpiarVentasDia() { modeloVentasDia.setRowCount(0); }
    public void agregarFilaVentaDia(Object[] fila) { modeloVentasDia.addRow(fila); }
    public void setResumenDia(String texto) { lblResumenDia.setText(texto); }
}