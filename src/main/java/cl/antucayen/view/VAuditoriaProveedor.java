package cl.antucayen.view;

import cl.antucayen.model.entity.AuditoriaProveedor;
import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Vista de solo lectura de la trazabilidad de modificaciones de un proveedor. */
public class VAuditoriaProveedor extends JDialog {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Fecha/Hora", "Usuario", "Campo", "Valor anterior", "Valor nuevo"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tabla = ComponentesSwing.crearTabla(modelo);

    public VAuditoriaProveedor(Window owner, String nombreProveedor,
                               List<AuditoriaProveedor> auditorias) {
        super(owner, "Historial de proveedor", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(850, 420));
        setSize(980, 520);
        setLocationRelativeTo(owner);
        construir(nombreProveedor, auditorias);
    }

    private void construir(String nombreProveedor, List<AuditoriaProveedor> auditorias) {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        raiz.setBackground(new Color(243, 244, 246));

        JLabel titulo = new JLabel("Historial de cambios — " + nombreProveedor);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        raiz.add(titulo, BorderLayout.NORTH);

        tabla.setAutoCreateRowSorter(true);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(145);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(170);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(220);
        cargar(auditorias);
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JButton cerrar = ComponentesSwing.crearBoton("Cerrar", new Color(75, 85, 99));
        cerrar.addActionListener(e -> dispose());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pie.setOpaque(false);
        pie.add(cerrar);
        raiz.add(pie, BorderLayout.SOUTH);
        setContentPane(raiz);
    }

    private void cargar(List<AuditoriaProveedor> auditorias) {
        modelo.setRowCount(0);
        for (AuditoriaProveedor a : auditorias) {
            modelo.addRow(new Object[]{
                    a.getFechaHora() == null ? "-" : FECHA.format(a.getFechaHora()),
                    a.getNombreUsuario(),
                    a.getCampoModificado(),
                    a.getValorAnterior(),
                    a.getValorNuevo()
            });
        }
    }
}
