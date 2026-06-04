package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VAjusteInventario extends JPanel {

    private JTextField        txtArchivo;
    private JButton           btnSeleccionar;
    private JComboBox<String> cmbModalidad;
    private JButton           btnCargar;
    private JTable            tblPreview;
    private DefaultTableModel modeloPreview;
    private JButton           btnConfirmar;
    private JButton           btnVolver;
    private JLabel            lblEstado;
    private JPanel            panelPasos;
    private CardLayout        cardLayout;

    public VAjusteInventario() { initComponents(); }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(243, 244, 246));

        cardLayout = new CardLayout();
        panelPasos = new JPanel(cardLayout);
        panelPasos.setBackground(new Color(243, 244, 246));
        panelPasos.add(crearPaso1(), "PASO1");
        panelPasos.add(crearPaso2(), "PASO2");
        add(panelPasos, BorderLayout.CENTER);
        cardLayout.show(panelPasos, "PASO1");
    }

    private JPanel crearPaso1() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;

        JLabel lbl = new JLabel("📂  Paso 1: Seleccionar archivo");
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        lbl.setForeground(new Color(17, 24, 39));
        gbc.gridx = 0; gbc.gridy = 0;
        p.add(lbl, gbc);

        JLabel desc = new JLabel("Selecciona un archivo Excel (.xlsx) o CSV con columnas SKU y Cantidad.");
        desc.setFont(new Font("Arial", Font.PLAIN, 13));
        desc.setForeground(new Color(107, 114, 128));
        gbc.gridy = 1;
        p.add(desc, gbc);

        // Selector archivo
        gbc.gridwidth = 1; gbc.weightx = 0.8;
        gbc.gridx = 0; gbc.gridy = 2;
        txtArchivo = new JTextField();
        txtArchivo.setEditable(false);
        txtArchivo.setFont(new Font("Arial", Font.PLAIN, 13));
        txtArchivo.setBackground(new Color(248, 250, 252));
        txtArchivo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        txtArchivo.setPreferredSize(new Dimension(0, 36));
        p.add(txtArchivo, gbc);

        gbc.gridx = 1; gbc.weightx = 0.2;
        btnSeleccionar = VBuscadorProductos.crearBoton("📁 Examinar", new Color(37, 99, 235));
        btnSeleccionar.setPreferredSize(new Dimension(0, 36));
        p.add(btnSeleccionar, gbc);

        // Modalidad
        gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel lblMod = new JLabel("Modalidad de ajuste:");
        lblMod.setFont(new Font("Arial", Font.BOLD, 13));
        lblMod.setForeground(new Color(71, 85, 105));
        p.add(lblMod, gbc);

        gbc.gridy = 4;
        cmbModalidad = new JComboBox<>(new String[]{
                "Sumar al stock actual",
                "Reemplazar stock actual",
                "Restar al stock actual"
        });
        cmbModalidad.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbModalidad.setPreferredSize(new Dimension(0, 36));
        p.add(cmbModalidad, gbc);

        // Botón cargar
        gbc.gridy = 5;
        btnCargar = VBuscadorProductos.crearBoton("Cargar y previsualizar →", new Color(5, 150, 105));
        btnCargar.setPreferredSize(new Dimension(0, 40));
        p.add(btnCargar, gbc);

        // Label estado/error
        gbc.gridy = 6;
        lblEstado = new JLabel("");
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(185, 28, 28));
        p.add(lblEstado, gbc);

        return p;
    }

    private JPanel crearPaso2() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(new Color(243, 244, 246));

        // Título
        JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        titulo.setBackground(Color.WHITE);
        titulo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));
        JLabel lbl = new JLabel("👁  Paso 2: Previsualización del ajuste");
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        lbl.setForeground(new Color(17, 24, 39));
        titulo.add(lbl);

        JLabel lblLeyenda = new JLabel(
                "  ✅ OK   |   ❌ Error (no se aplicará)   |   ⚠ Advertencia"
        );
        lblLeyenda.setFont(new Font("Arial", Font.PLAIN, 12));
        lblLeyenda.setForeground(new Color(107, 114, 128));
        titulo.add(lblLeyenda);

        // Tabla previsualización
        String[] cols = {"SKU", "Nombre producto", "Stock actual",
                "Cantidad", "Stock proyectado", "Estado"};
        modeloPreview = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblPreview = new JTable(modeloPreview);
        tblPreview.setFont(new Font("Arial", Font.PLAIN, 13));
        tblPreview.setRowHeight(36);
        tblPreview.setGridColor(new Color(229, 231, 235));
        tblPreview.setSelectionBackground(new Color(219, 234, 254));
        tblPreview.setSelectionForeground(new Color(17, 24, 39));
        tblPreview.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPreview.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblPreview.getTableHeader().setBackground(new Color(17, 24, 39));
        tblPreview.getTableHeader().setForeground(Color.WHITE);
        tblPreview.getTableHeader().setPreferredSize(new Dimension(0, 38));

        tblPreview.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblPreview.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblPreview.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblPreview.getColumnModel().getColumn(3).setPreferredWidth(80);
        tblPreview.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblPreview.getColumnModel().getColumn(5).setPreferredWidth(140);

        // Renderer para colorear filas según estado
        tblPreview.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                Object estado = table.getModel().getValueAt(row, 5);
                if (isSelected) {
                    setBackground(new Color(219, 234, 254));
                    setForeground(new Color(17, 24, 39));
                } else if (estado != null && estado.toString().startsWith("❌")) {
                    setBackground(new Color(254, 226, 226));
                    setForeground(new Color(153, 27, 27));
                } else if (estado != null && estado.toString().startsWith("⚠")) {
                    setBackground(new Color(254, 243, 199));
                    setForeground(new Color(146, 64, 14));
                } else {
                    setBackground(Color.WHITE);
                    setForeground(new Color(17, 24, 39));
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tblPreview);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));

        // Botones confirmar/volver
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                new Color(229, 231, 235)));

        btnVolver = new JButton("← Volver");
        btnVolver.setFont(new Font("Arial", Font.BOLD, 13));
        btnVolver.setBackground(new Color(241, 245, 249));
        btnVolver.setForeground(new Color(30, 41, 59));
        btnVolver.setFocusPainted(false);
        btnVolver.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConfirmar = VBuscadorProductos.crearBoton(
                "✅ Confirmar ajuste", new Color(5, 150, 105));

        botones.add(btnVolver);
        botones.add(btnConfirmar);

        p.add(titulo,  BorderLayout.NORTH);
        p.add(scroll,  BorderLayout.CENTER);
        p.add(botones, BorderLayout.SOUTH);

        return p;
    }

    // ── MÉTODOS PÚBLICOS ─────────────────────────────────────────
    public void mostrarPaso2() { cardLayout.show(panelPasos, "PASO2"); }
    public void mostrarPaso1() { cardLayout.show(panelPasos, "PASO1"); }

    public String getNombreArchivo()       { return txtArchivo.getText(); }
    public void   setNombreArchivo(String n){ txtArchivo.setText(n); }
    public String getModalidad()           { return (String) cmbModalidad.getSelectedItem(); }

    public void mostrarError(String msg)   { lblEstado.setText("⚠ " + msg); }
    public void limpiarError()             { lblEstado.setText(""); }

    public void limpiarPreview()               { modeloPreview.setRowCount(0); }
    public void agregarFilaPreview(Object[] f)  { modeloPreview.addRow(f); }

    public JButton getBtnSeleccionar() { return btnSeleccionar; }
    public JButton getBtnCargar()      { return btnCargar; }
    public JButton getBtnConfirmar()   { return btnConfirmar; }
    public JButton getBtnVolver()      { return btnVolver; }
    public JTable  getTblPreview()     { return tblPreview; }
}