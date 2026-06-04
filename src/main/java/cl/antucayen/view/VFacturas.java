package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VFacturas extends JPanel {

    private JComboBox<String> cmbFiltroEstado;
    private JButton           btnFiltrar;
    private JButton           btnNueva;
    private JTable            tblFacturas;
    private DefaultTableModel modeloTabla;

    public VFacturas() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        cmbFiltroEstado = new JComboBox<>(new String[]{"Todos", "Pendiente", "Procesada", "Observada"});
        cmbFiltroEstado.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbFiltroEstado.setPreferredSize(new Dimension(160, 32));

        btnFiltrar = VBuscadorProductos.crearBoton("🔍 Filtrar",     new Color(37, 99, 235));
        btnNueva   = VBuscadorProductos.crearBoton("+ Nueva factura", new Color(5, 150, 105));

        barraTop.add(new JLabel("Estado:"));
        barraTop.add(cmbFiltroEstado);
        barraTop.add(btnFiltrar);
        barraTop.add(Box.createHorizontalStrut(16));
        barraTop.add(btnNueva);

        String[] cols = {"ID", "Número", "Fecha", "Proveedor", "Estado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblFacturas = VBuscadorProductos.crearTabla(modeloTabla);
        tblFacturas.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblFacturas.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblFacturas.getColumnModel().getColumn(2).setPreferredWidth(100);
        tblFacturas.getColumnModel().getColumn(3).setPreferredWidth(240);
        tblFacturas.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(tblFacturas);
        JLabel lblAyuda = new JLabel("  💡 Doble clic para procesar una factura");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));

        add(barraTop, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);
        add(lblAyuda, BorderLayout.SOUTH);
    }

    public String  getFiltroEstado()  { return (String) cmbFiltroEstado.getSelectedItem(); }
    public JButton getBtnFiltrar()    { return btnFiltrar; }
    public JButton getBtnNueva()      { return btnNueva; }
    public JTable  getTblFacturas()   { return tblFacturas; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
}