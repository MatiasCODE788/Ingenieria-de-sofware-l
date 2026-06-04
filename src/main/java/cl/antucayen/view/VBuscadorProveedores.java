package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VBuscadorProveedores extends JPanel {

    private JTextField        txtBusqueda;
    private JButton           btnBuscar;
    private JButton           btnNuevo;
    private JTable            tblProveedores;
    private DefaultTableModel modeloTabla;

    public VBuscadorProveedores() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        txtBusqueda = new JTextField(25);
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 13));
        txtBusqueda.setPreferredSize(new Dimension(280, 32));

        btnBuscar = VBuscadorProductos.crearBoton("🔍 Buscar",       new Color(37, 99, 235));
        btnNuevo  = VBuscadorProductos.crearBoton("+ Nuevo proveedor", new Color(5, 150, 105));

        barraTop.add(new JLabel("Buscar:"));
        barraTop.add(txtBusqueda);
        barraTop.add(btnBuscar);
        barraTop.add(Box.createHorizontalStrut(16));
        barraTop.add(btnNuevo);

        String[] cols = {"ID", "RUT", "Nombre", "Teléfono", "Correo"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblProveedores = VBuscadorProductos.crearTabla(modeloTabla);
        tblProveedores.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblProveedores.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblProveedores.getColumnModel().getColumn(2).setPreferredWidth(220);
        tblProveedores.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblProveedores.getColumnModel().getColumn(4).setPreferredWidth(180);

        JScrollPane scroll = new JScrollPane(tblProveedores);
        JLabel lblAyuda = new JLabel("  💡 Doble clic para editar");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));

        add(barraTop, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);
        add(lblAyuda, BorderLayout.SOUTH);
    }

    public String  getTextoBusqueda()  { return txtBusqueda.getText().trim(); }
    public JButton getBtnBuscar()      { return btnBuscar; }
    public JButton getBtnNuevo()       { return btnNuevo; }
    public JTable  getTblProveedores() { return tblProveedores; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
}