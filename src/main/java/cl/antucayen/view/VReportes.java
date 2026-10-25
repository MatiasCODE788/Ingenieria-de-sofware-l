package cl.antucayen.view;

import cl.antucayen.model.domain.EstadoFactura;
import cl.antucayen.view.components.ComponentesSwing;
import cl.antucayen.view.components.EstiloAntucayen;
import cl.antucayen.view.components.IconoNavegacion;
import cl.antucayen.view.components.SelectorFecha;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.util.List;
import java.time.format.DateTimeFormatter;

public class VReportes extends JPanel {

    private static final DateTimeFormatter FORMATO_FILTRO_FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
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
        cmbEstadoFactura = new JComboBox<>(new String[]{
                "Todos",
                EstadoFactura.PENDIENTE.valorDb(),
                EstadoFactura.PROCESADA.valorDb(),
                EstadoFactura.OBSERVADA.valorDb()
        });
        filtros.add(cmbEstadoFactura);

        filtros.add(new JLabel("Desde:"));
        txtDesde = new JTextField("dd/mm/aaaa", 9);
        filtros.add(SelectorFecha.crearCampoConCalendario(txtDesde, FORMATO_FILTRO_FECHA));
        filtros.add(new JLabel("Hasta:"));
        txtHasta = new JTextField("dd/mm/aaaa", 9);
        filtros.add(SelectorFecha.crearCampoConCalendario(txtHasta, FORMATO_FILTRO_FECHA));

        btnGenerar = ComponentesSwing.crearBoton("Generar", new Color(37, 99, 235));
        btnExportar = ComponentesSwing.crearBoton("Exportar Excel", new Color(5, 150, 105));
        btnEquivalencias = ComponentesSwing.crearBoton("Ir a Equivalencias", new Color(124, 58, 237));
        btnExportar.setEnabled(false);
        btnEquivalencias.setVisible(false);
        JPanel accionesFiltro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        accionesFiltro.setBackground(Color.WHITE);
        accionesFiltro.add(btnGenerar);
        accionesFiltro.add(btnExportar);
        accionesFiltro.add(btnEquivalencias);

        lblEstado = new JLabel(" ");
        lblEstado.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        modelo = new DefaultTableModel();
        tabla = ComponentesSwing.crearTabla(modelo);

        JPanel norte = new JPanel(new BorderLayout(0, 12));
        norte.setOpaque(false);
        JPanel tarjetas = new JPanel(new GridLayout(2, 2, 12, 12));
        tarjetas.setOpaque(false);
        tarjetas.add(crearTarjetaReporte("Reporte de stock actual", "Inventario y disponibilidad de productos.", 0, "products"));
        tarjetas.add(crearTarjetaReporte("Productos sin equivalencia", "Artículos sin código de proveedor relacionado.", 1, "suppliers"));
        tarjetas.add(crearTarjetaReporte("Reporte de facturas", "Facturas por período y estado de procesamiento.", 2, "facturas"));
        tarjetas.add(crearTarjetaReporte("Movimientos de inventario", "Ingresos, salidas y ajustes de stock.", 3, "reports"));
        norte.add(tarjetas, BorderLayout.NORTH);
        JPanel barraFiltros = new JPanel(new BorderLayout());
        JPanel filtrosYAcciones = new JPanel(new BorderLayout(0, 2));
        filtrosYAcciones.add(filtros, BorderLayout.NORTH);
        filtrosYAcciones.add(accionesFiltro, BorderLayout.SOUTH);
        barraFiltros.add(filtrosYAcciones, BorderLayout.CENTER);
        barraFiltros.add(lblEstado, BorderLayout.SOUTH);
        norte.add(barraFiltros, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    private JPanel crearTarjetaReporte(String titulo, String descripcion, int indice, String icono) {
        JPanel tarjeta = new JPanel(new BorderLayout(8, 6));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloAntucayen.BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cabecera.setOpaque(false);
        cabecera.add(new JLabel(new IconoNavegacion(icono, EstiloAntucayen.primario(), 18)));
        JLabel nombre = new JLabel(titulo);
        nombre.setFont(new Font("Dialog", Font.BOLD, 13));
        nombre.setForeground(EstiloAntucayen.TEXT);
        cabecera.add(nombre);
        tarjeta.add(cabecera, BorderLayout.NORTH);
        JLabel ayuda = new JLabel(descripcion);
        ayuda.setFont(new Font("Dialog", Font.PLAIN, 11));
        ayuda.setForeground(EstiloAntucayen.MUTED);
        tarjeta.add(ayuda, BorderLayout.CENTER);
        JButton generar = ComponentesSwing.crearBoton("Generar reporte", EstiloAntucayen.primario());
        generar.setPreferredSize(new Dimension(0, 32));
        generar.addActionListener(e -> { cmbReporte.setSelectedIndex(indice); btnGenerar.doClick(); });
        tarjeta.add(generar, BorderLayout.SOUTH);
        return tarjeta;
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
