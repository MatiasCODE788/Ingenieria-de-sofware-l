package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VHistorial extends JPanel {

    private JTextField        txtSku;
    private JComboBox<String> cmbTipo;
    private JTextField        txtDesde;
    private JTextField        txtHasta;
    private JButton           btnFiltrar;
    private JButton           btnLimpiar;
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
                "Todos", "Ingreso por compra", "Salida por venta",
                "Ajuste positivo", "Ajuste negativo", "Reversión"
        });
        cmbTipo.setFont(new Font("Arial", Font.PLAIN, 12));
        cmbTipo.setPreferredSize(new Dimension(160, 30));
        filtros.add(cmbTipo);

        filtros.add(crearLabel("Desde:"));
        txtDesde = new JTextField("dd/mm/aaaa", 10);
        txtDesde.setFont(new Font("Arial", Font.PLAIN, 12));
        txtDesde.setPreferredSize(new Dimension(100, 30));
        filtros.add(txtDesde);

        filtros.add(crearLabel("Hasta:"));
        txtHasta = new JTextField("dd/mm/aaaa", 10);
        txtHasta.setFont(new Font("Arial", Font.PLAIN, 12));
        txtHasta.setPreferredSize(new Dimension(100, 30));
        filtros.add(txtHasta);

        btnFiltrar = VBuscadorProductos.crearBoton("🔍 Filtrar", new Color(37, 99, 235));
        btnLimpiar = VBuscadorProductos.crearBoton("✖ Limpiar",  new Color(107, 114, 128));
        btnFiltrar.setPreferredSize(new Dimension(100, 30));
        btnLimpiar.setPreferredSize(new Dimension(100, 30));
        filtros.add(btnFiltrar);
        filtros.add(btnLimpiar);

        String[] cols = {"Tipo", "Fecha/Hora", "SKU", "Producto",
                "Stock anterior", "Cantidad", "Stock resultante", "Usuario"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblMovimientos = VBuscadorProductos.crearTabla(modeloTabla);
        tblMovimientos.getColumnModel().getColumn(0).setPreferredWidth(120);
        tblMovimientos.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblMovimientos.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblMovimientos.getColumnModel().getColumn(3).setPreferredWidth(200);
        tblMovimientos.getColumnModel().getColumn(4).setPreferredWidth(90);
        tblMovimientos.getColumnModel().getColumn(5).setPreferredWidth(80);
        tblMovimientos.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblMovimientos.getColumnModel().getColumn(7).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(tblMovimientos);
        add(filtros, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
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