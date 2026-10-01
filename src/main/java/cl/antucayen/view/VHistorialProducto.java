package cl.antucayen.view;

import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** RF-56: stock actual y últimos diez movimientos de un producto. */
public class VHistorialProducto extends JDialog {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public VHistorialProducto(Window owner, Producto producto, List<MovimientoInventario> movimientos) {
        super(owner, "Stock e historial — " + producto.getSku(), ModalityType.APPLICATION_MODAL);
        setSize(780, 470);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel cabecera = new JPanel(new GridLayout(2, 1));
        JLabel nombre = new JLabel(producto.getSku() + " — " + producto.getNombre());
        nombre.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel stock = new JLabel("Stock actual: " + producto.getStockActual() + " " + producto.getUnidadMedida());
        stock.setFont(new Font("Arial", Font.BOLD, 14));
        stock.setForeground(new Color(5, 150, 105));
        cabecera.add(nombre);
        cabecera.add(stock);

        String[] columnas = {"Fecha/Hora", "Tipo", "Cantidad", "Stock anterior", "Stock resultante", "Usuario"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (MovimientoInventario m : movimientos) {
            String usuario = m.getNombreUsuario();
            if (usuario == null || usuario.isBlank()) usuario = m.getUsernameUsuario();
            modelo.addRow(new Object[]{
                    m.getFechaHora() == null ? "" : m.getFechaHora().format(FECHA_HORA),
                    m.getTipoMovimiento(), m.getCantidadAplicada(), m.getStockAnterior(),
                    m.getStockResultante(), usuario == null ? "" : usuario
            });
        }
        JTable tabla = ComponentesSwing.crearTabla(modelo);

        JButton cerrar = ComponentesSwing.crearBoton("Cerrar", new Color(107, 114, 128));
        cerrar.addActionListener(e -> dispose());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(cerrar);

        add(cabecera, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
    }
}
