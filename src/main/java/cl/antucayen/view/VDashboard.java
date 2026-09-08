package cl.antucayen.view;

import cl.antucayen.model.dao.ItemVentaDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class VDashboard extends JPanel {

    private static final Color FONDO       = new Color(243, 244, 246);
    private static final Color BORDE       = new Color(229, 231, 235);
    private static final Color TEXTO_SUAVE = new Color(107, 114, 128);
    private static final Color TEXTO_FUERTE = new Color(17, 24, 39);

    private static final NumberFormat CLP =
            NumberFormat.getCurrencyInstance(Locale.of("es", "CL"));

    // Tarjetas "clásicas"
    private JLabel lblProductosActivos;
    private JLabel lblFacturasPendientes;
    private JLabel lblMovimientosHoy;

    // Ventas de hoy (total + desglose por medio de pago)
    private JLabel lblVentasHoyTotal;
    private JLabel lblVentasHoyEfectivo;
    private JLabel lblVentasHoyDebito;
    private JLabel lblVentasHoyCredito;

    // Ventas del mes
    private JLabel lblVentasMes;

    // Stock bajo
    private JLabel lblStockBajo;
    private JPanel cardStockBajo;

    // Top 5 productos más vendidos del mes
    private DefaultTableModel modeloTop5;

    public VDashboard() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ---------- Título ----------
        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelTitulo.setBackground(FONDO);
        JLabel lblTitulo = new JLabel("Panel principal");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitulo.setForeground(TEXTO_FUERTE);
        JLabel lblSub = new JLabel("  Resumen operativo del minimarket");
        lblSub.setFont(new Font("Arial", Font.PLAIN, 13));
        lblSub.setForeground(TEXTO_SUAVE);
        panelTitulo.add(lblTitulo);
        panelTitulo.add(lblSub);

        // ---------- Contenido central (varias filas apiladas) ----------
        JPanel panelContenido = new JPanel();
        panelContenido.setLayout(new BoxLayout(panelContenido, BoxLayout.Y_AXIS));
        panelContenido.setBackground(FONDO);

        // Fila 1: Ventas hoy / Ventas del mes / Stock bajo
        JPanel filaVentas = new JPanel(new GridLayout(1, 3, 16, 0));
        filaVentas.setBackground(FONDO);
        filaVentas.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblVentasHoyTotal    = new JLabel("—");
        lblVentasHoyEfectivo = new JLabel("—");
        lblVentasHoyDebito   = new JLabel("—");
        lblVentasHoyCredito  = new JLabel("—");
        filaVentas.add(crearTarjetaVentasHoy());

        lblVentasMes = new JLabel("—");
        filaVentas.add(crearTarjeta("VENTAS DEL MES", lblVentasMes, new Color(147, 51, 234), "\uD83D\uDCC8"));

        lblStockBajo = new JLabel("—");
        cardStockBajo = crearTarjeta("STOCK BAJO", lblStockBajo, new Color(220, 38, 38), "\u26A0\uFE0F");
        filaVentas.add(cardStockBajo);

        // Fila 2: tarjetas clásicas (Productos activos / Facturas pendientes / Movimientos hoy)
        JPanel filaClasica = new JPanel(new GridLayout(1, 3, 16, 0));
        filaClasica.setBackground(FONDO);
        filaClasica.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblProductosActivos   = new JLabel("—");
        lblFacturasPendientes = new JLabel("—");
        lblMovimientosHoy     = new JLabel("—");

        filaClasica.add(crearTarjeta("PRODUCTOS ACTIVOS",  lblProductosActivos,
                new Color(37, 99, 235),  "\uD83D\uDCE6"));
        filaClasica.add(crearTarjeta("FACTURAS PENDIENTES", lblFacturasPendientes,
                new Color(217, 119, 6),  "\uD83D\uDCC4"));
        filaClasica.add(crearTarjeta("MOVIMIENTOS HOY",    lblMovimientosHoy,
                new Color(5, 150, 105),  "\uD83D\uDCCB"));

        // Fila 3: Top 5 productos más vendidos del mes
        JPanel panelTop5 = crearPanelTop5();
        panelTop5.setAlignmentX(Component.LEFT_ALIGNMENT);

        filaVentas.setMaximumSize(new Dimension(Integer.MAX_VALUE, filaVentas.getPreferredSize().height));
        filaClasica.setMaximumSize(new Dimension(Integer.MAX_VALUE, filaClasica.getPreferredSize().height));

        panelContenido.add(filaVentas);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 16)));
        panelContenido.add(filaClasica);
        panelContenido.add(Box.createRigidArea(new Dimension(0, 16)));
        panelContenido.add(panelTop5);

        add(panelTitulo,   BorderLayout.NORTH);
        add(panelContenido, BorderLayout.CENTER);
    }

    // ---------- Tarjeta genérica (título + ícono + valor grande) ----------
    private JPanel crearTarjeta(String titulo, JLabel lblValor, Color color, String icono) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 11));
        lblTitulo.setForeground(TEXTO_SUAVE);

        lblValor.setFont(new Font("Arial", Font.BOLD, 32));
        lblValor.setForeground(color);

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Arial", Font.PLAIN, 28));
        lblIcono.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(lblTitulo, BorderLayout.WEST);
        top.add(lblIcono,  BorderLayout.EAST);

        card.add(top,      BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    // ---------- Tarjeta especial: Ventas de hoy (total + desglose por medio de pago) ----------
    private JPanel crearTarjetaVentasHoy() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblTitulo = new JLabel("VENTAS HOY");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 11));
        lblTitulo.setForeground(TEXTO_SUAVE);

        JLabel lblIcono = new JLabel("\uD83D\uDCB0");
        lblIcono.setFont(new Font("Arial", Font.PLAIN, 28));
        lblIcono.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(lblTitulo, BorderLayout.WEST);
        top.add(lblIcono,  BorderLayout.EAST);

        lblVentasHoyTotal.setFont(new Font("Arial", Font.BOLD, 30));
        lblVentasHoyTotal.setForeground(new Color(5, 150, 105));

        JPanel desglose = new JPanel(new GridLayout(3, 1, 0, 4));
        desglose.setBackground(Color.WHITE);
        desglose.add(crearLineaMedioPago("Efectivo", lblVentasHoyEfectivo, new Color(5, 150, 105)));
        desglose.add(crearLineaMedioPago("Débito",   lblVentasHoyDebito,   new Color(37, 99, 235)));
        desglose.add(crearLineaMedioPago("Crédito",  lblVentasHoyCredito,  new Color(147, 51, 234)));

        card.add(top,       BorderLayout.NORTH);
        card.add(lblVentasHoyTotal, BorderLayout.CENTER);
        card.add(desglose,  BorderLayout.SOUTH);
        return card;
    }

    private JPanel crearLineaMedioPago(String nombre, JLabel lblValor, Color color) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setBackground(Color.WHITE);

        JLabel lblPunto = new JLabel("\u25CF ");
        lblPunto.setForeground(color);
        JLabel lblNombre = new JLabel(nombre);
        lblNombre.setFont(new Font("Arial", Font.PLAIN, 12));
        lblNombre.setForeground(TEXTO_SUAVE);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        izquierda.setBackground(Color.WHITE);
        izquierda.add(lblPunto);
        izquierda.add(lblNombre);

        lblValor.setFont(new Font("Arial", Font.BOLD, 12));
        lblValor.setForeground(TEXTO_FUERTE);
        lblValor.setHorizontalAlignment(SwingConstants.RIGHT);

        fila.add(izquierda, BorderLayout.WEST);
        fila.add(lblValor,  BorderLayout.EAST);
        return fila;
    }

    // ---------- Panel Top 5 productos más vendidos del mes ----------
    private JPanel crearPanelTop5() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        JLabel lblTitulo = new JLabel("TOP 5 PRODUCTOS MÁS VENDIDOS DEL MES");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 12));
        lblTitulo.setForeground(TEXTO_SUAVE);

        modeloTop5 = new DefaultTableModel(
                new Object[]{"#", "SKU", "Producto", "Cantidad vendida", "Monto total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable tabla = new JTable(modeloTop5);
        tabla.setRowHeight(26);
        tabla.setFont(new Font("Arial", Font.PLAIN, 12));
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tabla.setEnabled(false);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(100, 190));

        panel.add(lblTitulo, BorderLayout.NORTH);
        panel.add(scroll,    BorderLayout.CENTER);
        return panel;
    }

    // ---------- Setters usados por el controlador ----------

    public void setProductosActivos(int n)   { lblProductosActivos.setText(String.valueOf(n)); }
    public void setFacturasPendientes(int n) { lblFacturasPendientes.setText(String.valueOf(n)); }
    public void setMovimientosHoy(int n)     { lblMovimientosHoy.setText(String.valueOf(n)); }

    /** Total y desglose por medio de pago de las ventas de hoy (montos en pesos, sin decimales). */
    public void setVentasHoy(int total, int efectivo, int debito, int credito) {
        lblVentasHoyTotal.setText(formatearMonto(total));
        lblVentasHoyEfectivo.setText(formatearMonto(efectivo));
        lblVentasHoyDebito.setText(formatearMonto(debito));
        lblVentasHoyCredito.setText(formatearMonto(credito));
    }

    public void setVentasMes(int total) {
        lblVentasMes.setText(formatearMonto(total));
    }

    /** Cantidad de productos activos con stock bajo. Resalta la tarjeta si hay al menos uno. */
    public void setStockBajo(int cantidad) {
        lblStockBajo.setText(String.valueOf(cantidad));
        Color colorFondo = cantidad > 0 ? new Color(254, 242, 242) : Color.WHITE;
        cardStockBajo.setBackground(colorFondo);
        for (Component c : cardStockBajo.getComponents()) {
            if (c instanceof JPanel p) p.setBackground(colorFondo);
        }
    }

    public void setTopProductos(List<ItemVentaDAO.ProductoVendido> productos) {
        modeloTop5.setRowCount(0);
        int puesto = 1;
        for (ItemVentaDAO.ProductoVendido p : productos) {
            modeloTop5.addRow(new Object[]{
                    puesto++,
                    p.sku(),
                    p.nombre(),
                    p.cantidadTotal(),
                    formatearMonto(p.montoTotal())
            });
        }
    }

    private String formatearMonto(int monto) {
        return CLP.format(monto);
    }
}