package cl.antucayen.view;

import cl.antucayen.view.components.ComponentesSwing;
import cl.antucayen.view.components.EstiloAntucayen;
import cl.antucayen.view.components.SelectorFecha;
import cl.antucayen.security.SesionActual;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class VHistorial extends JPanel {

    private static final DateTimeFormatter FORMATO_FILTRO_FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private JTextField        txtSku;
    private JComboBox<String> cmbTipo;
    private JTextField        txtDesde;
    private JTextField        txtHasta;
    private JButton           btnFiltrar;
    private JButton           btnLimpiar;
    private JButton           btnExportar;
    private JButton           btnRevertirAjuste;
    private JTable            tblMovimientos;
    private DefaultTableModel modeloTabla;

    public VHistorial() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        filtros.setBackground(Color.WHITE);
        filtros.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        filtros.add(crearLabel("SKU:"));
        txtSku = new JTextField(10);
        txtSku.setFont(new Font("Arial", Font.PLAIN, 12));
        txtSku.setPreferredSize(new Dimension(100, 30));
        filtros.add(txtSku);

        filtros.add(crearLabel("Tipo:"));
        cmbTipo = new JComboBox<>(new String[]{
                "Todos", "Stock inicial", "Ingreso por compra", "Venta",
                "Ajuste positivo", "Ajuste negativo", "Reversión"
        });
        cmbTipo.setFont(new Font("Arial", Font.PLAIN, 12));
        cmbTipo.setPreferredSize(new Dimension(160, 30));
        filtros.add(cmbTipo);

        filtros.add(crearLabel("Desde:"));
        txtDesde = new JTextField("dd/mm/aaaa", 10);
        txtDesde.setFont(new Font("Arial", Font.PLAIN, 12));
        txtDesde.setPreferredSize(new Dimension(100, 30));
        filtros.add(SelectorFecha.crearCampoConCalendario(txtDesde, FORMATO_FILTRO_FECHA));

        filtros.add(crearLabel("Hasta:"));
        txtHasta = new JTextField("dd/mm/aaaa", 10);
        txtHasta.setFont(new Font("Arial", Font.PLAIN, 12));
        txtHasta.setPreferredSize(new Dimension(100, 30));
        filtros.add(SelectorFecha.crearCampoConCalendario(txtHasta, FORMATO_FILTRO_FECHA));

        btnFiltrar = ComponentesSwing.crearBoton("Filtrar", new Color(37, 99, 235));
        btnLimpiar = ComponentesSwing.crearBoton("Limpiar",  new Color(107, 114, 128));
        btnExportar = ComponentesSwing.crearBoton("Exportar CSV/Excel", new Color(5, 150, 105));
        btnRevertirAjuste = ComponentesSwing.crearBoton("Revertir ajuste", new Color(185, 28, 28));
        btnRevertirAjuste.setVisible(SesionActual.esAdministrador());
        btnFiltrar.setPreferredSize(new Dimension(100, 30));
        btnLimpiar.setPreferredSize(new Dimension(100, 30));
        btnExportar.setPreferredSize(new Dimension(165, 30));
        btnRevertirAjuste.setPreferredSize(new Dimension(140, 30));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        acciones.setBackground(Color.WHITE);
        acciones.add(btnFiltrar);
        acciones.add(btnLimpiar);
        acciones.add(btnExportar);
        acciones.add(btnRevertirAjuste);
        JPanel barra = new JPanel(new BorderLayout(0, 1));
        barra.add(filtros, BorderLayout.NORTH);
        barra.add(acciones, BorderLayout.SOUTH);

        String[] cols = {"Tipo", "Fecha/Hora", "SKU", "Producto",
                "Stock anterior", "Cantidad", "Stock resultante", "ID Usuario", "Usuario", "ID Ajuste"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblMovimientos = ComponentesSwing.crearTabla(modeloTabla);
        tblMovimientos.getColumnModel().getColumn(0).setPreferredWidth(120);
        tblMovimientos.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblMovimientos.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblMovimientos.getColumnModel().getColumn(3).setPreferredWidth(200);
        tblMovimientos.getColumnModel().getColumn(4).setPreferredWidth(90);
        tblMovimientos.getColumnModel().getColumn(5).setPreferredWidth(80);
        tblMovimientos.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblMovimientos.getColumnModel().getColumn(7).setPreferredWidth(75);
        tblMovimientos.getColumnModel().getColumn(8).setPreferredWidth(170);
        tblMovimientos.getColumnModel().getColumn(9).setPreferredWidth(70);

        JScrollPane scroll = new JScrollPane(tblMovimientos);
        add(barra, BorderLayout.NORTH);
        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, crearResumenMovimiento());
        divisor.setDividerSize(12);
        divisor.setBorder(null);
        divisor.setResizeWeight(0.72);
        SwingUtilities.invokeLater(() -> divisor.setDividerLocation(0.72));
        add(divisor, BorderLayout.CENTER);
    }

    /** El panel lateral describe únicamente datos ya cargados en la tabla de movimientos. */
    private JPanel crearResumenMovimiento() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloAntucayen.BORDER),
                BorderFactory.createEmptyBorder(15, 14, 15, 14)));
        JLabel titulo = new JLabel("Movimiento seleccionado");
        titulo.setFont(new Font("Dialog", Font.BOLD, 14));
        titulo.setForeground(EstiloAntucayen.TEXT);
        panel.add(titulo, BorderLayout.NORTH);
        String[] nombres = {"SKU", "Producto", "Tipo", "Fecha y hora", "Cantidad", "Stock resultante", "Usuario"};
        int[] columnas = {2,3,0,1,5,6,8};
        JLabel[] datos = new JLabel[nombres.length];
        JPanel campos = new JPanel(new GridLayout(0,1,0,7));
        campos.setBackground(Color.WHITE);
        for (int i=0;i<nombres.length;i++) {
            JPanel fila = new JPanel(new BorderLayout(0,3));
            fila.setBackground(Color.WHITE);
            JLabel keyLabel = new JLabel(nombres[i]);
            keyLabel.setFont(new Font("Dialog", Font.BOLD, 11));
            keyLabel.setForeground(EstiloAntucayen.TEXT);
            datos[i] = new JLabel("—");
            datos[i].setForeground(EstiloAntucayen.MUTED);
            datos[i].setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
            fila.add(keyLabel, BorderLayout.NORTH);
            fila.add(datos[i], BorderLayout.CENTER);
            campos.add(fila);
        }
        panel.add(new JScrollPane(campos), BorderLayout.CENTER);
        tblMovimientos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int fila = tblMovimientos.getSelectedRow();
            for (int i=0;i<datos.length;i++) {
                Object valor = fila >= 0 ? tblMovimientos.getValueAt(fila, columnas[i]) : null;
                datos[i].setText(valor == null ? "—" : String.valueOf(valor));
            }
        });
        return panel;
    }

    private JLabel crearLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        l.setForeground(new Color(71, 85, 105));
        return l;
    }

    public String  getSku()            { return txtSku.getText().trim(); }
    public String  getTipo()           { return (String) cmbTipo.getSelectedItem(); }
    public String  getDesde()          { return txtDesde.getText().trim(); }
    public String  getHasta()          { return txtHasta.getText().trim(); }
    public JButton getBtnFiltrar()     { return btnFiltrar; }
    public JButton getBtnLimpiar()     { return btnLimpiar; }
    public JButton getBtnExportar()    { return btnExportar; }
    public JButton getBtnRevertirAjuste() { return btnRevertirAjuste; }
    public JTable  getTblMovimientos() { return tblMovimientos; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
    public void limpiarFiltros() {
        txtSku.setText("");
        cmbTipo.setSelectedIndex(0);
        txtDesde.setText("dd/mm/aaaa");
        txtHasta.setText("dd/mm/aaaa");
    }
}
