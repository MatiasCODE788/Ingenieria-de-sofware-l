package cl.antucayen.view;

import cl.antucayen.util.SesionActual;
import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VAjusteInventario extends JPanel {

    private JTextField txtArchivo;
    private JButton btnSeleccionar;
    private JComboBox<String> cmbModalidad;
    private JCheckBox chkCorreccionAutorizada;
    private JButton btnCargar;
    private JTable tblPreview;
    private DefaultTableModel modeloPreview;
    private JButton btnConfirmar;
    private JButton btnCancelar;
    private JButton btnResolverDuplicados;
    private JLabel lblEstado;
    private JLabel lblDuplicadosAviso;
    private JPanel panelPasos;
    private CardLayout cardLayout;
    private VReporteErrores panelErrores;

    public VAjusteInventario() {
        initComponents();
    }

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

        panelErrores = new VReporteErrores();
        panelErrores.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        add(panelErrores, BorderLayout.SOUTH);
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

        JLabel lbl = new JLabel("Paso 1: Seleccionar archivo");
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        lbl.setForeground(new Color(17, 24, 39));
        gbc.gridx = 0;
        gbc.gridy = 0;
        p.add(lbl, gbc);

        JLabel desc = new JLabel("Selecciona un archivo Excel (.xlsx) o CSV con columnas SKU y cantidad en la primera fila.");
        desc.setFont(new Font("Arial", Font.PLAIN, 13));
        desc.setForeground(new Color(107, 114, 128));
        gbc.gridy = 1;
        p.add(desc, gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 0.8;
        gbc.gridx = 0;
        gbc.gridy = 2;
        txtArchivo = new JTextField();
        txtArchivo.setEditable(false);
        txtArchivo.setFont(new Font("Arial", Font.PLAIN, 13));
        txtArchivo.setBackground(new Color(248, 250, 252));
        txtArchivo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        txtArchivo.setPreferredSize(new Dimension(0, 36));
        p.add(txtArchivo, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.2;
        btnSeleccionar = ComponentesSwing.crearBoton("Examinar", new Color(37, 99, 235));
        btnSeleccionar.setPreferredSize(new Dimension(0, 36));
        p.add(btnSeleccionar, gbc);

        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel lblMod = new JLabel("Modalidad de ajuste:");
        lblMod.setFont(new Font("Arial", Font.BOLD, 13));
        lblMod.setForeground(new Color(71, 85, 105));
        p.add(lblMod, gbc);

        gbc.gridy = 4;
        cmbModalidad = new JComboBox<>(new String[]{
                "Sumar al stock actual",
                "Reemplazar stock actual"
        });
        cmbModalidad.setSelectedIndex(-1);
        cmbModalidad.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbModalidad.setPreferredSize(new Dimension(0, 36));
        p.add(cmbModalidad, gbc);

        gbc.gridy = 5;
        chkCorreccionAutorizada = new JCheckBox(
                "Corrección autorizada (permite cantidades negativas en el archivo)");
        chkCorreccionAutorizada.setFont(new Font("Arial", Font.PLAIN, 13));
        chkCorreccionAutorizada.setBackground(Color.WHITE);
        chkCorreccionAutorizada.setForeground(new Color(71, 85, 105));
        boolean esAdmin = SesionActual.esAdministrador();
        chkCorreccionAutorizada.setEnabled(esAdmin);
        if (!esAdmin) {
            chkCorreccionAutorizada.setToolTipText(
                    "Solo un Administrador puede autorizar correcciones con cantidades negativas");
        }
        p.add(chkCorreccionAutorizada, gbc);

        gbc.gridy = 6;
        btnCargar = ComponentesSwing.crearBoton("Cargar y previsualizar", new Color(5, 150, 105));
        btnCargar.setPreferredSize(new Dimension(0, 40));
        p.add(btnCargar, gbc);

        gbc.gridy = 7;
        lblEstado = new JLabel("");
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(185, 28, 28));
        p.add(lblEstado, gbc);

        return p;
    }

    private JPanel crearPaso2() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(243, 244, 246));

        JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        titulo.setBackground(Color.WHITE);
        titulo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));
        JLabel lbl = new JLabel("Paso 2: Previsualización del ajuste");
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        lbl.setForeground(new Color(17, 24, 39));
        titulo.add(lbl);

        JLabel lblLeyenda = new JLabel("OK | Error (no se aplicará) | Advertencia | Duplicado");
        lblLeyenda.setFont(new Font("Arial", Font.PLAIN, 12));
        lblLeyenda.setForeground(new Color(107, 114, 128));
        titulo.add(lblLeyenda);

        lblDuplicadosAviso = new JLabel("");
        lblDuplicadosAviso.setFont(new Font("Arial", Font.BOLD, 12));
        lblDuplicadosAviso.setForeground(new Color(180, 83, 9));
        lblDuplicadosAviso.setVisible(false);

        String[] cols = {"SKU", "Nombre producto", "Stock actual",
                "Cantidad", "Stock proyectado", "Estado"};
        modeloPreview = new DefaultTableModel(cols, 0) {
            @Override
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
        tblPreview.getColumnModel().getColumn(5).setPreferredWidth(210);

        tblPreview.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Object estado = table.getModel().getValueAt(row, 5);
                if (isSelected) {
                    setBackground(new Color(219, 234, 254));
                    setForeground(new Color(17, 24, 39));
                } else if (estado != null && estado.toString().startsWith("ERROR")) {
                    setBackground(new Color(254, 226, 226));
                    setForeground(new Color(153, 27, 27));
                } else if (estado != null && estado.toString().startsWith("DUPLICADO")) {
                    setBackground(new Color(237, 233, 254));
                    setForeground(new Color(91, 33, 182));
                } else if (estado != null && estado.toString().startsWith("ADVERTENCIA")) {
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

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setBackground(new Color(243, 244, 246));
        centro.add(scroll, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 13));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setForeground(new Color(30, 41, 59));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnResolverDuplicados = ComponentesSwing.crearBoton(
                "Resolver duplicados...", new Color(124, 58, 237));
        btnResolverDuplicados.setVisible(false);

        btnConfirmar = ComponentesSwing.crearBoton("Confirmar ajuste", new Color(5, 150, 105));

        botones.add(lblDuplicadosAviso);
        botones.add(btnCancelar);
        botones.add(btnResolverDuplicados);
        botones.add(btnConfirmar);

        p.add(titulo, BorderLayout.NORTH);
        p.add(centro, BorderLayout.CENTER);
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    public void mostrarPaso2() { cardLayout.show(panelPasos, "PASO2"); }
    public void mostrarPaso1() { cardLayout.show(panelPasos, "PASO1"); }

    public String getNombreArchivo() { return txtArchivo.getText(); }
    public void setNombreArchivo(String n) { txtArchivo.setText(n); }
    public String getModalidad() { return (String) cmbModalidad.getSelectedItem(); }
    public void limpiarModalidad() { cmbModalidad.setSelectedIndex(-1); }
    public boolean isCorreccionAutorizada() {
        return chkCorreccionAutorizada.isSelected() && chkCorreccionAutorizada.isEnabled();
    }

    public void mostrarError(String msg) { lblEstado.setText(msg); }
    public void limpiarError() { lblEstado.setText(""); }

    public void limpiarPreview() { modeloPreview.setRowCount(0); }
    public void agregarFilaPreview(Object[] f) { modeloPreview.addRow(f); }

    public VReporteErrores getPanelErroresEstructura() { return panelErrores; }

    /** Muestra para cada SKU duplicado cuántas filas lo contienen y cuáles son. */
    public void mostrarAvisoDuplicados(Map<String, List<Integer>> duplicados) {
        boolean hay = duplicados != null && !duplicados.isEmpty();
        lblDuplicadosAviso.setVisible(hay);
        btnResolverDuplicados.setVisible(hay);
        btnConfirmar.setEnabled(!hay);
        if (!hay) {
            lblDuplicadosAviso.setText("");
            return;
        }
        String detalle = duplicados.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue().size() + " filas (" +
                        e.getValue().stream().map(String::valueOf).collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.joining("; "));
        lblDuplicadosAviso.setText("Duplicados pendientes - " + detalle);
        lblDuplicadosAviso.setToolTipText(detalle);
    }

    public JButton getBtnSeleccionar() { return btnSeleccionar; }
    public JButton getBtnCargar() { return btnCargar; }
    public JButton getBtnConfirmar() { return btnConfirmar; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnResolverDuplicados() { return btnResolverDuplicados; }
    public JTable getTblPreview() { return tblPreview; }
}
