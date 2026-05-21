package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VHistorialMovimientos extends JPanel {

    private JTextField txtFechaDesde;
    private JTextField txtFechaHasta;
    private JTextField txtSKU;
    private JTextField txtUsuario;
    private JButton btnFiltrar;
    private JButton btnLimpiar;
    private JTable tblMovimientos;
    private DefaultTableModel modeloTabla;

    public VHistorialMovimientos() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(240, 244, 248));

        // ── FILTROS ───────────────────────────────────────────────
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        panelFiltros.setBackground(Color.WHITE);
        panelFiltros.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 227, 237)));

        panelFiltros.add(crearLabel("Desde:"));
        txtFechaDesde = crearCampo(100);
        txtFechaDesde.setText("dd/mm/aaaa");
        txtFechaDesde.setForeground(new Color(150, 160, 180));
        panelFiltros.add(txtFechaDesde);

        panelFiltros.add(crearLabel("Hasta:"));
        txtFechaHasta = crearCampo(100);
        txtFechaHasta.setText("dd/mm/aaaa");
        txtFechaHasta.setForeground(new Color(150, 160, 180));
        panelFiltros.add(txtFechaHasta);

        panelFiltros.add(crearLabel("SKU:"));
        txtSKU = crearCampo(90);
        panelFiltros.add(txtSKU);

        panelFiltros.add(crearLabel("Usuario:"));
        txtUsuario = crearCampo(100);
        panelFiltros.add(txtUsuario);

        btnFiltrar = new JButton("🔍 Filtrar");
        btnFiltrar.setFont(new Font("Arial", Font.BOLD, 12));
        btnFiltrar.setBackground(new Color(37, 99, 235));
        btnFiltrar.setForeground(Color.WHITE);
        btnFiltrar.setFocusPainted(false);
        btnFiltrar.setBorderPainted(false);
        btnFiltrar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFiltrar.setPreferredSize(new Dimension(100, 30));

        btnLimpiar = new JButton("✖ Limpiar");
        btnLimpiar.setFont(new Font("Arial", Font.BOLD, 12));
        btnLimpiar.setBackground(new Color(241, 245, 249));
        btnLimpiar.setForeground(new Color(30, 41, 59));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLimpiar.setPreferredSize(new Dimension(100, 30));

        panelFiltros.add(btnFiltrar);
        panelFiltros.add(btnLimpiar);

        // ── TABLA ─────────────────────────────────────────────────
        String[] columnas = {"Tipo", "Fecha", "Hora", "SKU", "Producto", "Stock anterior", "Stock nuevo", "Usuario"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblMovimientos = new JTable(modeloTabla);
        tblMovimientos.setFont(new Font("Arial", Font.PLAIN, 13));
        tblMovimientos.setRowHeight(34);
        tblMovimientos.setGridColor(new Color(220, 227, 237));
        tblMovimientos.setSelectionBackground(new Color(219, 234, 254));
        tblMovimientos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblMovimientos.getTableHeader().setBackground(new Color(30, 48, 84));
        tblMovimientos.getTableHeader().setForeground(Color.WHITE);
        tblMovimientos.getTableHeader().setPreferredSize(new Dimension(0, 36));

        tblMovimientos.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblMovimientos.getColumnModel().getColumn(1).setPreferredWidth(90);
        tblMovimientos.getColumnModel().getColumn(2).setPreferredWidth(70);
        tblMovimientos.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblMovimientos.getColumnModel().getColumn(4).setPreferredWidth(200);
        tblMovimientos.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblMovimientos.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblMovimientos.getColumnModel().getColumn(7).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(tblMovimientos);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));

        // Datos de ejemplo
        modeloTabla.addRow(new Object[]{"Ajuste",    "20/05/2026", "09:15", "SKU-001", "Arroz Grado 1 x 1kg",   120, 150, "admin"});
        modeloTabla.addRow(new Object[]{"Ajuste",    "19/05/2026", "14:30", "SKU-002", "Aceite Vegetal x 1L",    30,  45, "bodega1"});
        modeloTabla.addRow(new Object[]{"Inactivar", "18/05/2026", "11:00", "SKU-003", "Azúcar Blanca x 1kg",    10,   0, "admin"});

        add(panelFiltros, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JLabel crearLabel(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(new Color(71, 85, 105));
        return lbl;
    }

    private JTextField crearCampo(int ancho) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Arial", Font.PLAIN, 12));
        txt.setPreferredSize(new Dimension(ancho, 30));
        return txt;
    }

    public String getFechaDesde()  { return txtFechaDesde.getText().trim(); }
    public String getFechaHasta()  { return txtFechaHasta.getText().trim(); }
    public String getSKU()         { return txtSKU.getText().trim(); }
    public String getUsuario()     { return txtUsuario.getText().trim(); }

    public JButton getBtnFiltrar() { return btnFiltrar; }
    public JButton getBtnLimpiar() { return btnLimpiar; }
    public JTable getTblMovimientos() { return tblMovimientos; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }

    public void limpiarTabla()              { modeloTabla.setRowCount(0); }
    public void agregarFila(Object[] fila)  { modeloTabla.addRow(fila); }

    public void limpiarFiltros() {
        txtFechaDesde.setText("dd/mm/aaaa");
        txtFechaHasta.setText("dd/mm/aaaa");
        txtSKU.setText("");
        txtUsuario.setText("");
    }
}