package cl.antucayen.view;

import cl.antucayen.view.components.ComponentesSwing;
import cl.antucayen.view.components.EstiloAntucayen;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VBuscadorProductos extends JPanel {

    private JTextField        txtBusqueda;
    private JComboBox<String> cmbTipo;
    private JButton           btnBuscar;
    private JButton           btnNuevo;
    private JButton           btnExportar;
    private JButton           btnHistorialProducto;
    private JTable            tblProductos;
    private DefaultTableModel modeloTabla;

    public VBuscadorProductos() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        txtBusqueda = new JTextField(25);
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 13));
        txtBusqueda.setPreferredSize(new Dimension(280, 32));

        cmbTipo = new JComboBox<>(new String[]{"Nombre", "SKU", "Código de barras"});
        cmbTipo.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbTipo.setPreferredSize(new Dimension(160, 32));

        btnBuscar = ComponentesSwing.crearBoton("Buscar", new Color(37, 99, 235));
        btnNuevo  = ComponentesSwing.crearBoton("+ Nuevo producto", new Color(5, 150, 105));
        btnExportar = ComponentesSwing.crearBoton("Exportar CSV/Excel", new Color(2, 132, 199));
        btnHistorialProducto = ComponentesSwing.crearBoton("Stock + últimos 10", new Color(124, 58, 237));
        btnExportar.setPreferredSize(new Dimension(165, 32));
        btnHistorialProducto.setPreferredSize(new Dimension(155, 32));

        barraTop.setLayout(new BorderLayout(0, 2));
        JPanel filtrosBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filtrosBusqueda.setBackground(Color.WHITE);
        filtrosBusqueda.add(new JLabel("Buscar:"));
        filtrosBusqueda.add(txtBusqueda);
        filtrosBusqueda.add(cmbTipo);
        filtrosBusqueda.add(btnBuscar);
        JPanel accionesBusqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        accionesBusqueda.setBackground(Color.WHITE);
        accionesBusqueda.add(btnNuevo);
        accionesBusqueda.add(btnExportar);
        accionesBusqueda.add(btnHistorialProducto);
        barraTop.add(filtrosBusqueda, BorderLayout.NORTH);
        barraTop.add(accionesBusqueda, BorderLayout.SOUTH);

        String[] cols = {"SKU", "Nombre", "Código de barras", "Unidad", "Precio unitario", "Stock", "Estado"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblProductos = ComponentesSwing.crearTabla(modeloTabla);
        tblProductos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblProductos.getColumnModel().getColumn(1).setPreferredWidth(260);
        tblProductos.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblProductos.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblProductos.getColumnModel().getColumn(5).setPreferredWidth(70);
        tblProductos.getColumnModel().getColumn(6).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(tblProductos);

        JLabel lblAyuda = new JLabel("  Doble clic en una fila para abrir la ficha del producto");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));
        lblAyuda.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JPanel lista = new JPanel(new BorderLayout(0, 8));
        lista.setOpaque(false);
        lista.add(scroll, BorderLayout.CENTER);
        lista.add(lblAyuda, BorderLayout.SOUTH);
        JPanel detalle = crearFichaVistaPrevia();
        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lista, detalle);
        divisor.setResizeWeight(0.60);
        divisor.setDividerSize(12);
        divisor.setBorder(null);
        SwingUtilities.invokeLater(() -> divisor.setDividerLocation(0.60));
        add(barraTop, BorderLayout.NORTH);
        add(divisor, BorderLayout.CENTER);
    }

    /** Ficha consultiva sincronizada con la fila: la edición sigue en el diálogo original. */
    private JPanel crearFichaVistaPrevia() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloAntucayen.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel titulo = new JLabel("Ficha del producto");
        titulo.setFont(new Font("Dialog", Font.BOLD, 14));
        titulo.setForeground(EstiloAntucayen.TEXT);
        panel.add(titulo, BorderLayout.NORTH);
        String[] etiquetas = {"SKU", "Nombre del producto", "Código de barras", "Unidad de medida",
                "Precio de venta", "Stock actual", "Estado"};
        JLabel[] valores = new JLabel[etiquetas.length];
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 5));
        campos.setBackground(Color.WHITE);
        for (int i=0;i<etiquetas.length;i++) {
            JPanel campo = new JPanel(new BorderLayout(0, 2));
            campo.setBackground(Color.WHITE);
            JLabel etiqueta = new JLabel(etiquetas[i]);
            etiqueta.setFont(new Font("Dialog", Font.BOLD, 11));
            etiqueta.setForeground(EstiloAntucayen.TEXT);
            valores[i] = new JLabel("—");
            valores[i].setFont(new Font("Dialog", Font.PLAIN, 12));
            valores[i].setForeground(EstiloAntucayen.MUTED);
            valores[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(EstiloAntucayen.BORDER),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            campo.add(etiqueta, BorderLayout.NORTH);
            campo.add(valores[i], BorderLayout.CENTER);
            campos.add(campo);
        }
        panel.add(new JScrollPane(campos, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        JButton abrir = ComponentesSwing.crearBoton("Ver / editar ficha", EstiloAntucayen.primario());
        abrir.setToolTipText("Abre la ficha existente del producto seleccionado");
        abrir.addActionListener(e -> {
            int fila = tblProductos.getSelectedRow();
            if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto de la lista."); return; }
            java.awt.event.MouseEvent doble = new java.awt.event.MouseEvent(tblProductos,
                    java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    5, tblProductos.getCellRect(fila, 0, true).y + 5, 2, false);
            for (java.awt.event.MouseListener listener : tblProductos.getMouseListeners())
                listener.mouseClicked(doble);
        });
        tblProductos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int fila = tblProductos.getSelectedRow();
            for (int i=0;i<valores.length;i++) {
                Object valor = fila >= 0 ? tblProductos.getValueAt(fila, i) : null;
                valores[i].setText(valor == null ? "—" : String.valueOf(valor));
                valores[i].setToolTipText(valor == null ? null : String.valueOf(valor));
            }
        });
        panel.add(abrir, BorderLayout.SOUTH);
        return panel;
    }

    public String  getTextoBusqueda() { return txtBusqueda.getText().trim(); }
    public String  getTipoBusqueda()  { return (String) cmbTipo.getSelectedItem(); }
    public JButton getBtnBuscar()     { return btnBuscar; }
    public JButton getBtnNuevo()      { return btnNuevo; }
    public JButton getBtnExportar()   { return btnExportar; }
    public JButton getBtnHistorialProducto() { return btnHistorialProducto; }
    public JTable  getTblProductos()  { return tblProductos; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
}
