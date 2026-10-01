package cl.antucayen.view;

import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VReportes extends JPanel {

    private JComboBox<String> cmbReporte;
    private JComboBox<String> cmbEstadoFactura;
    private JTextField txtDesde;
    private JTextField txtHasta;
    private JButton btnGenerar;
    private JButton btnExportar;
    private JButton btnEquivalencias;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblEstado;

    public VReportes() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(243, 244, 246));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        filtros.setBackground(Color.WHITE);
        filtros.add(new JLabel("Reporte:"));
        cmbReporte = new JComboBox<>(new String[]{
                "RF-57 Stock actual",
                "RF-58 Equivalencias faltantes",
                "RF-59 Facturas",
                "RF-60 Movimientos"
        });
        filtros.add(cmbReporte);

        filtros.add(new JLabel("Estado factura:"));
        cmbEstadoFactura = new JComboBox<>(new String[]{"Todos", "Pendiente", "Procesada", "Observada"});
        filtros.add(cmbEstadoFactura);

        filtros.add(new JLabel("Desde:"));
        txtDesde = new JTextField("dd/mm/aaaa", 9);
        filtros.add(txtDesde);
        filtros.add(new JLabel("Hasta:"));
        txtHasta = new JTextField("dd/mm/aaaa", 9);
        filtros.add(txtHasta);

        btnGenerar = ComponentesSwing.crearBoton("Generar", new Color(37, 99, 235));
        btnExportar = ComponentesSwing.crearBoton("Exportar Excel", new Color(5, 150, 105));
        btnEquivalencias = ComponentesSwing.crearBoton("Ir a Equivalencias", new Color(124, 58, 237));
        btnExportar.setEnabled(false);
        btnEquivalencias.setVisible(false);
        filtros.add(btnGenerar);
        filtros.add(btnExportar);
        filtros.add(btnEquivalencias);

        lblEstado = new JLabel(" ");
        lblEstado.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        modelo = new DefaultTableModel();
        tabla = ComponentesSwing.crearTabla(modelo);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(filtros, BorderLayout.CENTER);
        norte.add(lblEstado, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public String getReporteSeleccionado() { return (String) cmbReporte.getSelectedItem(); }
    public String getEstadoFactura() { return (String) cmbEstadoFactura.getSelectedItem(); }
    public String getDesde() { return txtDesde.getText().trim(); }
    public String getHasta() { return txtHasta.getText().trim(); }
    public JButton getBtnGenerar() { return btnGenerar; }
    public JButton getBtnExportar() { return btnExportar; }
    public JButton getBtnEquivalencias() { return btnEquivalencias; }

    public void cargarReporte(List<String> columnas, List<List<Object>> filas) {
        modelo.setDataVector(new Object[0][0], columnas.toArray());
        for (List<Object> fila : filas) modelo.addRow(fila.toArray());
        btnExportar.setEnabled(!filas.isEmpty());
    }

    public void setEquivalenciasVisible(boolean visible) {
        btnEquivalencias.setVisible(visible);
    }

    public void setEstado(String texto, boolean error) {
        lblEstado.setText(texto == null || texto.isBlank() ? " " : texto);
        lblEstado.setForeground(error ? new Color(185, 28, 28) : new Color(5, 150, 105));
    }
}
