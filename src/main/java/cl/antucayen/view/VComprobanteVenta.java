package cl.antucayen.view;

import cl.antucayen.model.entity.ComprobanteVenta;
import cl.antucayen.model.entity.ItemVenta;
import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.model.entity.Venta;
import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.time.format.DateTimeFormatter;

/**
 * RF-72: comprobante automático de venta, visualizable e imprimible.
 */
public class VComprobanteVenta extends JDialog {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ComprobanteVenta comprobante;
    private JTable tablaItems;
    private DefaultTableModel modeloItems;
    private JTextArea txtResumen;
    private JButton btnImprimir;
    private JButton btnCerrar;

    public VComprobanteVenta(Window owner, ComprobanteVenta comprobante) {
        super(owner, "Comprobante de venta #" + comprobante.venta().getIdVenta(), ModalityType.APPLICATION_MODAL);
        this.comprobante = comprobante;
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(680, 620);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel cabecera = new JPanel();
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("MINIMARKET ANTUCAYEN");
        titulo.setFont(new Font("Arial", Font.BOLD, 20));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitulo = new JLabel("Comprobante de venta");
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        cabecera.add(titulo);
        cabecera.add(Box.createVerticalStrut(4));
        cabecera.add(subtitulo);
        add(cabecera, BorderLayout.NORTH);

        String[] columnas = {"SKU", "Producto", "Cantidad", "P. unitario", "Subtotal"};
        modeloItems = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaItems = ComponentesSwing.crearTabla(modeloItems);
        JScrollPane scrollItems = new JScrollPane(tablaItems);

        txtResumen = new JTextArea();
        txtResumen.setEditable(false);
        txtResumen.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtResumen.setBackground(new Color(248, 250, 252));
        txtResumen.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtResumen.setRows(10);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.add(scrollItems, BorderLayout.CENTER);
        centro.add(new JScrollPane(txtResumen), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        btnImprimir = ComponentesSwing.crearBoton("Imprimir", new Color(37, 99, 235));
        btnCerrar = ComponentesSwing.crearBoton("Cerrar", new Color(107, 114, 128));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(btnImprimir);
        acciones.add(btnCerrar);
        add(acciones, BorderLayout.SOUTH);

        btnCerrar.addActionListener(e -> dispose());
        btnImprimir.addActionListener(e -> imprimir());
    }

    private void cargarDatos() {
        for (ItemVenta item : comprobante.items()) {
            modeloItems.addRow(new Object[]{
                    item.getSku(),
                    item.getNombreProducto(),
                    item.getCantidad(),
                    "$" + formatear(item.getPrecioUnitarioVenta()),
                    "$" + formatear(item.getSubtotal())
            });
        }

        Venta venta = comprobante.venta();
        StringBuilder pagos = new StringBuilder();
        for (PagoVenta pago : comprobante.pagosAplicados()) {
            if (!pagos.isEmpty()) pagos.append("\n");
            pagos.append("  ").append(pago.getMedioPago())
                    .append(": $").append(formatear(pago.getMonto()));
        }

        txtResumen.setText(
                "Venta      : #" + venta.getIdVenta() + "\n" +
                "Fecha/hora : " + (venta.getFechaHora() == null ? "" : venta.getFechaHora().format(FECHA_HORA)) + "\n" +
                "Cajero     : " + valor(venta.getNombreUsuario()) + "\n" +
                "Estado     : " + venta.getEstado() + "\n" +
                "----------------------------------------------\n" +
                "TOTAL      : $" + formatear(venta.getMontoTotal()) + "\n" +
                "RECIBIDO   : $" + formatear(comprobante.montoRecibido()) + "\n" +
                "VUELTO     : $" + formatear(comprobante.vuelto()) + "\n" +
                "Pagos aplicados:\n" + pagos + "\n" +
                "----------------------------------------------\n" +
                "Gracias por su compra.");
        txtResumen.setCaretPosition(0);
    }

    private void imprimir() {
        try {
            JTextArea impresion = new JTextArea(construirTextoImpresion());
            impresion.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            boolean enviado = impresion.print();
            if (!enviado) {
                JOptionPane.showMessageDialog(this, "Impresión cancelada.");
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo imprimir el comprobante: " + ex.getMessage(),
                    "Error de impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String construirTextoImpresion() {
        StringBuilder sb = new StringBuilder();
        sb.append("MINIMARKET ANTUCAYEN\n")
          .append("COMPROBANTE DE VENTA #").append(comprobante.venta().getIdVenta()).append("\n")
          .append(txtResumen.getText()).append("\n\nDETALLE\n");
        for (ItemVenta item : comprobante.items()) {
            sb.append(item.getSku()).append(" | ")
              .append(valor(item.getNombreProducto())).append(" | ")
              .append(item.getCantidad()).append(" x $")
              .append(formatear(item.getPrecioUnitarioVenta())).append(" = $")
              .append(formatear(item.getSubtotal())).append("\n");
        }
        return sb.toString();
    }

    private String formatear(int valor) {
        return String.format("%,d", valor).replace(',', '.');
    }

    private String valor(String texto) { return texto == null ? "" : texto; }
}
