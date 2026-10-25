package cl.antucayen.view;

import cl.antucayen.view.components.ComponentesSwing;
import cl.antucayen.view.components.EstiloAntucayen;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VBuscadorProveedores extends JPanel {

    private JTextField        txtBusqueda;
    private JButton           btnBuscar;
    private JButton           btnNuevo;
    private JButton           btnHistorial;
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

        btnBuscar    = ComponentesSwing.crearBoton("Buscar", new Color(37, 99, 235));
        btnNuevo     = ComponentesSwing.crearBoton("+ Nuevo proveedor", new Color(5, 150, 105));
        btnHistorial = ComponentesSwing.crearBoton("Historial de cambios", new Color(75, 85, 99));

        barraTop.add(new JLabel("Buscar:"));
        barraTop.add(txtBusqueda);
        barraTop.add(btnBuscar);
        barraTop.add(Box.createHorizontalStrut(16));
        barraTop.add(btnNuevo);
        barraTop.add(btnHistorial);

        String[] cols = {"ID", "RUT", "Nombre", "Teléfono", "Correo"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblProveedores = ComponentesSwing.crearTabla(modeloTabla);
        tblProveedores.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblProveedores.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblProveedores.getColumnModel().getColumn(2).setPreferredWidth(220);
        tblProveedores.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblProveedores.getColumnModel().getColumn(4).setPreferredWidth(180);

        JScrollPane scroll = new JScrollPane(tblProveedores);
        JLabel lblAyuda = new JLabel("  Doble clic para editar");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));

        JPanel lista = new JPanel(new BorderLayout(0, 8));
        lista.setOpaque(false);
        lista.add(scroll, BorderLayout.CENTER);
        lista.add(lblAyuda, BorderLayout.SOUTH);
        JPanel equivalencias = new JPanel(new BorderLayout(0, 16));
        equivalencias.setBackground(Color.WHITE);
        equivalencias.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloAntucayen.BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JLabel titulo = new JLabel("Equivalencias de productos");
        titulo.setFont(new Font("Dialog", Font.BOLD, 15));
        titulo.setForeground(EstiloAntucayen.TEXT);
        equivalencias.add(titulo, BorderLayout.NORTH);
        JTextArea descripcion = new JTextArea("Consulta y administra la relación entre los códigos internos "
                + "de cada proveedor y el SKU del inventario. Selecciona un proveedor en la lista "
                + "o abre el módulo de equivalencias para registrar, modificar y consultar sus códigos.");
        descripcion.setFont(new Font("Dialog", Font.PLAIN, 13));
        descripcion.setForeground(EstiloAntucayen.MUTED);
        descripcion.setWrapStyleWord(true);
        descripcion.setLineWrap(true);
        descripcion.setEditable(false);
        descripcion.setBackground(Color.WHITE);
        equivalencias.add(descripcion, BorderLayout.CENTER);
        JButton abrir = ComponentesSwing.crearBoton("Abrir equivalencias", EstiloAntucayen.primario());
        abrir.addActionListener(e -> {
            Window ventana = SwingUtilities.getWindowAncestor(this);
            if (ventana instanceof VPrincipal principal && principal.getBtnEquivalencias().isVisible())
                principal.getBtnEquivalencias().doClick();
        });
        equivalencias.add(abrir, BorderLayout.SOUTH);
        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lista, equivalencias);
        divisor.setResizeWeight(0.62);
        divisor.setDividerSize(12);
        divisor.setBorder(null);
        SwingUtilities.invokeLater(() -> divisor.setDividerLocation(0.62));
        add(barraTop, BorderLayout.NORTH);
        add(divisor, BorderLayout.CENTER);
    }

    public String  getTextoBusqueda()  { return txtBusqueda.getText().trim(); }
    public JButton getBtnBuscar()      { return btnBuscar; }
    public JButton getBtnNuevo()       { return btnNuevo; }
    public JButton getBtnHistorial()   { return btnHistorial; }
    public JTable  getTblProveedores() { return tblProveedores; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
}