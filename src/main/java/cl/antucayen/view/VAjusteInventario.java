package cl.antucayen.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VAjusteInventario extends JPanel {

    private JTextField txtArchivo;
    private JButton btnSeleccionarArchivo;
    private JComboBox<String> cmbModalidad;
    private JButton btnCargar;
    private JTable tblPreview;
    private DefaultTableModel modeloPreview;
    private JButton btnConfirmar;
    private JButton btnCancelar;
    private JLabel lblEstado;
    private JPanel panelPasos;
    private CardLayout cardLayout;

    public VAjusteInventario() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(240, 244, 248));

        cardLayout = new CardLayout();
        panelPasos = new JPanel(cardLayout);
        panelPasos.setBackground(new Color(240, 244, 248));

        panelPasos.add(crearPaso1(), "PASO1");
        panelPasos.add(crearPaso2(), "PASO2");

        add(panelPasos, BorderLayout.CENTER);
        cardLayout.show(panelPasos, "PASO1");
    }

    private JPanel crearPaso1() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridwidth = 2;

        JLabel lblTitulo = new JLabel("📂  Paso 1: Seleccionar archivo");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitulo.setForeground(new Color(30, 48, 84));
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblTitulo, gbc);

        JLabel lblDesc = new JLabel("Selecciona un archivo Excel (.xlsx) o CSV con columnas SKU y Cantidad.");
        lblDesc.setFont(new Font("Arial", Font.PLAIN, 13));
        lblDesc.setForeground(new Color(100, 116, 139));
        gbc.gridy = 1;
        panel.add(lblDesc, gbc);

        // Selector de archivo
        gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.8;
        txtArchivo = new JTextField();
        txtArchivo.setEditable(false);
        txtArchivo.setFont(new Font("Arial", Font.PLAIN, 13));
        txtArchivo.setPreferredSize(new Dimension(0, 36));
        txtArchivo.setBackground(new Color(248, 250, 252));
        panel.add(txtArchivo, gbc);

        gbc.gridx = 1; gbc.weightx = 0.2;
        btnSeleccionarArchivo = new JButton("📁 Examinar");
        btnSeleccionarArchivo.setFont(new Font("Arial", Font.BOLD, 12));
        btnSeleccionarArchivo.setBackground(new Color(37, 99, 235));
        btnSeleccionarArchivo.setForeground(Color.WHITE);
        btnSeleccionarArchivo.setFocusPainted(false);
        btnSeleccionarArchivo.setBorderPainted(false);
        btnSeleccionarArchivo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSeleccionarArchivo.setPreferredSize(new Dimension(0, 36));
        panel.add(btnSeleccionarArchivo, gbc);

        // Modalidad
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JLabel lblModalidad = new JLabel("Modalidad de ajuste:");
        lblModalidad.setFont(new Font("Arial", Font.BOLD, 13));
        lblModalidad.setForeground(new Color(71, 85, 105));
        panel.add(lblModalidad, gbc);

        gbc.gridy = 4;
        cmbModalidad = new JComboBox<>(new String[]{
                "Sumar al stock actual",
                "Reemplazar stock actual"
        });
        cmbModalidad.setFont(new Font("Arial", Font.PLAIN, 13));
        cmbModalidad.setPreferredSize(new Dimension(0, 36));
        panel.add(cmbModalidad, gbc);

        // Botón cargar
        gbc.gridy = 5;
        btnCargar = new JButton("Cargar y previsualizar →");
        btnCargar.setFont(new Font("Arial", Font.BOLD, 13));
        btnCargar.setBackground(new Color(5, 150, 105));
        btnCargar.setForeground(Color.WHITE);
        btnCargar.setFocusPainted(false);
        btnCargar.setBorderPainted(false);
        btnCargar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCargar.setPreferredSize(new Dimension(0, 40));
        panel.add(btnCargar, gbc);

        lblEstado = new JLabel("");
        lblEstado.setFont(new Font("Arial", Font.PLAIN, 12));
        lblEstado.setForeground(new Color(220, 38, 38));
        gbc.gridy = 6;
        panel.add(lblEstado, gbc);

        return panel;
    }

    private JPanel crearPaso2() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(new Color(240, 244, 248));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        // Título
        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        panelTitulo.setBackground(Color.WHITE);
        panelTitulo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 227, 237)));
        JLabel lblTitulo = new JLabel("👁  Paso 2: Previsualización del ajuste");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(new Color(30, 48, 84));
        panelTitulo.add(lblTitulo);

        // Tabla previsualización
        String[] columnas = {"SKU", "Nombre producto", "Stock actual", "Cantidad archivo", "Stock resultante"};
        modeloPreview = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblPreview = new JTable(modeloPreview);
        tblPreview.setFont(new Font("Arial", Font.PLAIN, 13));
        tblPreview.setRowHeight(34);
        tblPreview.setGridColor(new Color(220, 227, 237));
        tblPreview.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblPreview.getTableHeader().setBackground(new Color(30, 48, 84));
        tblPreview.getTableHeader().setForeground(Color.WHITE);
        tblPreview.getTableHeader().setPreferredSize(new Dimension(0, 36));

        JScrollPane scroll = new JScrollPane(tblPreview);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));

        // Botones confirmar/cancelar
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panelBotones.setBackground(new Color(248, 250, 252));
        panelBotones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 227, 237)));

        btnCancelar = new JButton("← Volver");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 13));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setForeground(new Color(30, 41, 59));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnConfirmar = new JButton("✅ Confirmar ajuste");
        btnConfirmar.setFont(new Font("Arial", Font.BOLD, 13));
        btnConfirmar.setBackground(new Color(5, 150, 105));
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setFocusPainted(false);
        btnConfirmar.setBorderPainted(false);
        btnConfirmar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panelBotones.add(btnCancelar);
        panelBotones.add(btnConfirmar);

        panel.add(panelTitulo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);

        return panel;
    }

    public void mostrarPaso2() { cardLayout.show(panelPasos, "PASO2"); }
    public void mostrarPaso1() { cardLayout.show(panelPasos, "PASO1"); }

    public void mostrarError(String msg) { lblEstado.setText("⚠ " + msg); }
    public void limpiarError()           { lblEstado.setText(""); }

    public void setNombreArchivo(String nombre) { txtArchivo.setText(nombre); }
    public String getModalidad() { return (String) cmbModalidad.getSelectedItem(); }

    public void limpiarPreview()              { modeloPreview.setRowCount(0); }
    public void agregarFilaPreview(Object[] f){ modeloPreview.addRow(f); }

    public JButton getBtnSeleccionarArchivo() { return btnSeleccionarArchivo; }
    public JButton getBtnCargar()             { return btnCargar; }
    public JButton getBtnConfirmar()          { return btnConfirmar; }
    public JButton getBtnCancelar()           { return btnCancelar; }
    public JTable getTblPreview()             { return tblPreview; }
}