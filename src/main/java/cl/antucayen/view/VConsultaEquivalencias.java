package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VConsultaEquivalencias extends JPanel {

    private JTextField        txtProveedor;
    private JTextField        txtCodigo;
    private JTextField        txtSku;
    private JButton           btnBuscar;
    private JButton           btnLimpiar;
    private JTable            tblResultados;
    private DefaultTableModel modeloTabla;

    public VConsultaEquivalencias() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        JPanel barraTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        barraTop.setBackground(Color.WHITE);
        barraTop.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        txtProveedor = crearCampoFiltro(150);
        txtCodigo    = crearCampoFiltro(130);
        txtSku       = crearCampoFiltro(130);

        btnBuscar  = VBuscadorProductos.crearBoton("🔍 Buscar",  new Color(37, 99, 235));
        btnLimpiar = VBuscadorProductos.crearBoton("✕ Limpiar", new Color(107, 114, 128));

        barraTop.add(new JLabel("Proveedor:"));
        barraTop.add(txtProveedor);
        barraTop.add(new JLabel("Código interno:"));
        barraTop.add(txtCodigo);
        barraTop.add(new JLabel("SKU:"));
        barraTop.add(txtSku);
        barraTop.add(Box.createHorizontalStrut(8));
        barraTop.add(btnBuscar);
        barraTop.add(btnLimpiar);

        String[] cols = {"Proveedor", "Código interno", "SKU"};
        modeloTabla = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblResultados = VBuscadorProductos.crearTabla(modeloTabla);
        tblResultados.getColumnModel().getColumn(0).setPreferredWidth(220);
        tblResultados.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblResultados.getColumnModel().getColumn(2).setPreferredWidth(160);

        JScrollPane scroll = new JScrollPane(tblResultados);
        JLabel lblAyuda = new JLabel("  💡 Los tres filtros son opcionales y se combinan entre sí");
        lblAyuda.setFont(new Font("Arial", Font.ITALIC, 12));
        lblAyuda.setForeground(new Color(107, 114, 128));

        add(barraTop, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);
        add(lblAyuda, BorderLayout.SOUTH);
    }

    private JTextField crearCampoFiltro(int ancho) {
        JTextField t = new JTextField();
        t.setFont(new Font("Arial", Font.PLAIN, 13));
        t.setPreferredSize(new Dimension(ancho, 32));
        return t;
    }

    public String  getFiltroProveedor() { return txtProveedor.getText().trim(); }
    public String  getFiltroCodigo()    { return txtCodigo.getText().trim(); }
    public String  getFiltroSku()       { return txtSku.getText().trim(); }
    public JButton getBtnBuscar()       { return btnBuscar; }
    public JButton getBtnLimpiar()      { return btnLimpiar; }
    public JTable  getTblResultados()   { return tblResultados; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void limpiarFiltros() {
        txtProveedor.setText("");
        txtCodigo.setText("");
        txtSku.setText("");
    }

    public void limpiarTabla()             { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila) { modeloTabla.addRow(fila); }
}
