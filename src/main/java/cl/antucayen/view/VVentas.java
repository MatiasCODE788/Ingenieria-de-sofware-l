package cl.antucayen.view;

import cl.antucayen.model.entity.Producto;
import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VVentas extends JPanel {

    private JTextField        txtBusqueda;
    private JButton           btnAgregar;
    private JPopupMenu         popupSugerencias;
    private JList<Producto>    lstSugerencias;
    private DefaultListModel<Producto> modeloSugerencias;
    private JTable            tblCarrito;
    private DefaultTableModel modeloCarrito;
    private JLabel            lblTotal;
    private JLabel            lblEstadoVenta;
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
    private JButton           btnVerComprobante;

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

        modeloSugerencias = new DefaultListModel<>();
        lstSugerencias = new JList<>(modeloSugerencias);
        lstSugerencias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstSugerencias.setFixedCellHeight(32);
        lstSugerencias.setFont(new Font("Arial", Font.PLAIN, 13));
        // El listado funciona como una ayuda visual del campo de búsqueda. No
        // debe capturar el foco del teclado: las flechas/Enter se gestionan
        // desde txtBusqueda para que el cajero pueda seguir escribiendo sin
        // volver a hacer clic después de cada carácter.
        lstSugerencias.setFocusable(false);
        lstSugerencias.setRequestFocusEnabled(false);
        lstSugerencias.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof Producto producto) {
                    label.setText(producto.getSku() + " — " + producto.getNombre());
                    label.setToolTipText("Stock: " + producto.getStockActual()
                            + " | Precio: $" + formatearMonto(producto.getPrecioVenta()));
                    label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                }
                return label;
            }
        });

        JScrollPane scrollSugerencias = new JScrollPane(lstSugerencias);
        scrollSugerencias.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
        scrollSugerencias.setPreferredSize(new Dimension(520, 165));
        scrollSugerencias.setFocusable(false);
        scrollSugerencias.setRequestFocusEnabled(false);
        scrollSugerencias.getViewport().setFocusable(false);

        popupSugerencias = new JPopupMenu();
        popupSugerencias.setBorder(BorderFactory.createEmptyBorder());
        popupSugerencias.setFocusable(false);
        popupSugerencias.setRequestFocusEnabled(false);
        popupSugerencias.setLightWeightPopupEnabled(true);
        popupSugerencias.add(scrollSugerencias);

        btnAgregar = ComponentesSwing.crearBoton("+ Agregar", new Color(5, 150, 105));
        btnAgregar.setPreferredSize(new Dimension(130, 40));

        JLabel lblTitulo = new JLabel("Punto de Venta");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitulo.setForeground(new Color(17, 24, 39));
        lblEstadoVenta = new JLabel("Nueva venta — N° asignado al cobrar");
        lblEstadoVenta.setFont(new Font("Arial", Font.BOLD, 12));
        lblEstadoVenta.setForeground(new Color(37, 99, 235));

        JPanel encabezadoVenta = new JPanel(new BorderLayout());
        encabezadoVenta.setBackground(Color.WHITE);
        encabezadoVenta.add(lblTitulo, BorderLayout.WEST);
        encabezadoVenta.add(lblEstadoVenta, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setBackground(Color.WHITE);
        norte.add(encabezadoVenta, BorderLayout.NORTH);

        barraBusqueda.add(txtBusqueda, BorderLayout.CENTER);
        barraBusqueda.add(btnAgregar,  BorderLayout.EAST);
        norte.add(barraBusqueda, BorderLayout.CENTER);

        String[] cols = {"SKU", "Producto", "Cantidad", "Precio unit.", "Subtotal"};
        modeloCarrito = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 2; }
            public Class<?> getColumnClass(int c) { return c == 2 ? Integer.class : Object.class; }
        };
        tblCarrito = new JTable(modeloCarrito);
        tblCarrito.setFont(new Font("Arial", Font.PLAIN, 14));
        tblCarrito.setRowHeight(34);
        tblCarrito.setGridColor(new Color(229, 231, 235));

        // La fila seleccionada usa un fondo celeste claro. Algunos Look & Feel
        // (por ejemplo FlatLaf) asignan por defecto texto blanco a la selección,
        // lo que reduce mucho el contraste sobre este fondo. Fijamos ambos
        // colores explícitamente para que SKU, producto, cantidad y montos
        // permanezcan legibles al seleccionar o editar una fila.
        Color textoCarrito = new Color(17, 24, 39);
        tblCarrito.setForeground(textoCarrito);
        tblCarrito.setBackground(Color.WHITE);
        tblCarrito.setSelectionBackground(new Color(219, 234, 254));
        tblCarrito.setSelectionForeground(textoCarrito);
        tblCarrito.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblCarrito.getTableHeader().setBackground(new Color(17, 24, 39));
        tblCarrito.getTableHeader().setForeground(Color.WHITE);
        tblCarrito.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblCarrito.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblCarrito.getColumnModel().getColumn(1).setPreferredWidth(280);
        tblCarrito.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblCarrito.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblCarrito.getColumnModel().getColumn(4).setPreferredWidth(100);

        DefaultTableCellRenderer rendererMoneda = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Number numero) {
                    setText("$" + formatearMonto(numero.intValue()));
                } else {
                    setText(value == null ? "" : String.valueOf(value));
                }
            }
        };
        rendererMoneda.setHorizontalAlignment(SwingConstants.RIGHT);
        tblCarrito.getColumnModel().getColumn(3).setCellRenderer(rendererMoneda);
        tblCarrito.getColumnModel().getColumn(4).setCellRenderer(rendererMoneda);

        DefaultTableCellRenderer rendererCantidad = new DefaultTableCellRenderer();
        rendererCantidad.setHorizontalAlignment(SwingConstants.CENTER);
        tblCarrito.getColumnModel().getColumn(2).setCellRenderer(rendererCantidad);

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
        btnLimpiarCarrito = ComponentesSwing.crearBoton("Vaciar carrito", new Color(107, 114, 128));
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
        btnChipEfectivo = ComponentesSwing.crearBoton("Efectivo", new Color(5, 150, 105));
        btnChipDebito   = ComponentesSwing.crearBoton("Débito",   new Color(37, 99, 235));
        btnChipCredito  = ComponentesSwing.crearBoton("Crédito",  new Color(147, 51, 234));
        btnLimpiarPagos = ComponentesSwing.crearBoton("Limpiar", new Color(156, 163, 175));
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

        btnCobrar = new JButton("Cobrar");
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
        JLabel lbl = new JLabel("Ventas de hoy");
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(new Color(17, 24, 39));
        lblResumenDia = new JLabel("");
        lblResumenDia.setFont(new Font("Arial", Font.PLAIN, 12));
        lblResumenDia.setForeground(new Color(107, 114, 128));
        topVentas.add(lbl, BorderLayout.WEST);
        topVentas.add(lblResumenDia, BorderLayout.EAST);

        String[] cols = {"ID", "Hora", "Usuario", "Medio de pago", "Total", "Estado"};
        modeloVentasDia = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblVentasDia = ComponentesSwing.crearTabla(modeloVentasDia);
        JScrollPane scroll = new JScrollPane(tblVentasDia);

        JLabel ayuda = new JLabel("Doble clic en una venta 'Pagada' para anularla (solo Administrador)");
        ayuda.setFont(new Font("Arial", Font.ITALIC, 11));
        ayuda.setForeground(new Color(107, 114, 128));

        btnVerComprobante = ComponentesSwing.crearBoton(
                "Ver comprobante", new Color(37, 99, 235));
        btnVerComprobante.setPreferredSize(new Dimension(150, 30));

        JPanel pie = new JPanel(new BorderLayout(10, 0));
        pie.setBackground(Color.WHITE);
        pie.add(ayuda, BorderLayout.WEST);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setBackground(Color.WHITE);
        acciones.add(btnVerComprobante);
        pie.add(acciones, BorderLayout.EAST);

        p.add(topVentas, BorderLayout.NORTH);
        p.add(scroll,    BorderLayout.CENTER);
        p.add(pie,       BorderLayout.SOUTH);
        return p;
    }


    public void setVentaPendiente() {
        lblEstadoVenta.setText("Nueva venta — N° asignado al cobrar");
        lblEstadoVenta.setForeground(new Color(37, 99, 235));
    }

    public String getTextoBusqueda() { return txtBusqueda.getText().trim(); }
    public void limpiarBusqueda() {
        txtBusqueda.setText("");
        ocultarSugerencias();
        txtBusqueda.requestFocusInWindow();
    }

    public void mostrarSugerencias(List<Producto> productos) {
        modeloSugerencias.clear();
        if (productos == null || productos.isEmpty() || !txtBusqueda.isShowing()) {
            ocultarSugerencias();
            return;
        }

        // Guardamos la posición del cursor antes de abrir/refrescar el popup.
        // Algunos Look & Feel pueden transferir momentáneamente el foco a un
        // popup recién mostrado; restaurarlo en el siguiente ciclo del EDT
        // evita que la escritura se interrumpa.
        final int posicionCaret = txtBusqueda.getCaretPosition();

        for (Producto producto : productos) modeloSugerencias.addElement(producto);
        lstSugerencias.setSelectedIndex(0);

        int ancho = Math.max(txtBusqueda.getWidth(), 520);
        int alto = Math.min(165, productos.size() * 32 + 8);
        Component componente = popupSugerencias.getComponent(0);
        componente.setPreferredSize(new Dimension(ancho, alto));
        popupSugerencias.setPopupSize(new Dimension(ancho, alto));

        if (!popupSugerencias.isVisible()) {
            popupSugerencias.show(txtBusqueda, 0, txtBusqueda.getHeight());
        } else {
            // Al cambiar el número de coincidencias, refresca el tamaño sin
            // cerrar/reabrir el popup (evita parpadeos y pérdida de foco).
            popupSugerencias.revalidate();
            popupSugerencias.repaint();
        }

        SwingUtilities.invokeLater(() -> {
            if (!txtBusqueda.isShowing()) return;
            txtBusqueda.requestFocusInWindow();
            int largo = txtBusqueda.getDocument().getLength();
            txtBusqueda.setCaretPosition(Math.max(0, Math.min(posicionCaret, largo)));
        });
    }

    public void ocultarSugerencias() {
        if (popupSugerencias != null) popupSugerencias.setVisible(false);
    }

    public boolean isSugerenciasVisible() {
        return popupSugerencias != null && popupSugerencias.isVisible();
    }

    public Producto getProductoSugeridoSeleccionado() {
        return lstSugerencias == null ? null : lstSugerencias.getSelectedValue();
    }

    public void moverSeleccionSugerencia(int desplazamiento) {
        if (modeloSugerencias == null || modeloSugerencias.isEmpty()) return;
        int actual = lstSugerencias.getSelectedIndex();
        if (actual < 0) actual = 0;
        int nuevo = Math.max(0, Math.min(modeloSugerencias.size() - 1, actual + desplazamiento));
        lstSugerencias.setSelectedIndex(nuevo);
        lstSugerencias.ensureIndexIsVisible(nuevo);
    }

    public JTextField getTxtBusqueda()      { return txtBusqueda; }
    public JList<Producto> getLstSugerencias() { return lstSugerencias; }
    public JButton    getBtnAgregar()       { return btnAgregar; }
    public JButton    getBtnCobrar()        { return btnCobrar; }
    public JButton    getBtnLimpiarCarrito(){ return btnLimpiarCarrito; }
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

    /**
     * Actualiza en tiempo real el estado del pago. Cuando el efectivo recibido
     * supera lo necesario, muestra el vuelto como un estado válido de cobro.
     */
    public void setEstadoPago(int faltante, int vuelto, int total) {
        if (total <= 0) {
            lblRestante.setText("—");
            lblRestante.setForeground(new Color(107, 114, 128));
        } else if (faltante > 0) {
            lblRestante.setText("Falta $" + formatearMonto(faltante) + " por pagar");
            lblRestante.setForeground(new Color(217, 119, 6));
        } else if (vuelto > 0) {
            lblRestante.setText("✓ Pago completo — Vuelto: $" + formatearMonto(vuelto));
            lblRestante.setForeground(new Color(5, 150, 105));
        } else {
            lblRestante.setText("✓ Pago completo — Vuelto: $0");
            lblRestante.setForeground(new Color(5, 150, 105));
        }
    }

    /** Muestra una combinación de pagos inválida y mantiene el cobro bloqueado. */
    public void setEstadoPagoInvalido(String mensaje) {
        lblRestante.setText(mensaje);
        lblRestante.setForeground(new Color(220, 38, 38));
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
    public JButton getBtnVerComprobante() { return btnVerComprobante; }
    public DefaultTableModel getModeloVentasDia() { return modeloVentasDia; }
    public void limpiarVentasDia() { modeloVentasDia.setRowCount(0); }
    public void agregarFilaVentaDia(Object[] fila) { modeloVentasDia.addRow(fila); }
    public void setResumenDia(String texto) { lblResumenDia.setText(texto); }
}