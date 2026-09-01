package cl.antucayen.view;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.model.service.ServicioProcesamientoFactura.ResumenProcesamiento;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VProcesamientoFactura extends JDialog {

    private JLabel  lblNumero;
    private JLabel  lblProveedor;
    private JTable  tblObservados;
    private DefaultTableModel modeloObservados;
    private JButton btnCorregir;
    private JButton btnReprocesar;
    private JButton btnCerrar;

    private JLabel lblLeidos;
    private JLabel lblValidos;
    private JLabel lblObservadosCount;
    private JLabel lblNoProcesados;

    // Mapeo fila de la tabla -> idItem real, para saber cuál corregir
    private List<Integer> idsPorFila;

    public VProcesamientoFactura(JFrame parent) {
        super(parent, "Procesamiento de Factura", true);
        initComponents();
    }

    private void initComponents() {
        setSize(680, 560);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel("⚙️  Procesamiento de Factura");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        header.add(lblTitulo);

        JPanel cabecera = new JPanel(new GridBagLayout());
        cabecera.setBackground(Color.WHITE);
        cabecera.setBorder(BorderFactory.createEmptyBorder(14, 24, 6, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 8);
        gbc.weightx = 0.5;

        gbc.gridx = 0; gbc.gridy = 0; cabecera.add(crearTitulo("Factura"), gbc);
        gbc.gridx = 1;               cabecera.add(crearTitulo("Proveedor"), gbc);
        lblNumero    = crearValor();
        lblProveedor = crearValor();
        gbc.gridx = 0; gbc.gridy = 1; cabecera.add(lblNumero, gbc);
        gbc.gridx = 1;               cabecera.add(lblProveedor, gbc);

        // Resumen
        JPanel panelResumen = new JPanel(new GridLayout(1, 4, 8, 0));
        panelResumen.setBackground(Color.WHITE);
        panelResumen.setBorder(BorderFactory.createEmptyBorder(10, 24, 6, 24));
        lblLeidos          = crearTarjetaResumen("Leídos", new Color(71, 85, 105));
        lblValidos         = crearTarjetaResumen("Válidos", new Color(5, 150, 105));
        lblObservadosCount = crearTarjetaResumen("Observados", new Color(217, 119, 6));
        lblNoProcesados    = crearTarjetaResumen("No procesados", new Color(220, 38, 38));
        panelResumen.add(envolverTarjeta(lblLeidos, "Leídos"));
        panelResumen.add(envolverTarjeta(lblValidos, "Válidos"));
        panelResumen.add(envolverTarjeta(lblObservadosCount, "Observados"));
        panelResumen.add(envolverTarjeta(lblNoProcesados, "No procesados"));

        // Ítems observados
        JPanel panelObs = new JPanel(new BorderLayout(0, 6));
        panelObs.setBackground(Color.WHITE);
        panelObs.setBorder(BorderFactory.createEmptyBorder(10, 24, 8, 24));

        JLabel lblObs = new JLabel("Ítems Observados (requieren corrección manual)");
        lblObs.setFont(new Font("Arial", Font.BOLD, 13));
        lblObs.setForeground(new Color(217, 119, 6));

        String[] cols = {"Código proveedor", "Cantidad", "Precio unitario", "Estado"};
        modeloObservados = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblObservados = VBuscadorProductos.crearTabla(modeloObservados);
        JScrollPane scrollObs = new JScrollPane(tblObservados);
        scrollObs.setPreferredSize(new Dimension(0, 180));

        panelObs.add(lblObs,      BorderLayout.NORTH);
        panelObs.add(scrollObs,   BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Color.WHITE);
        centro.add(cabecera,      BorderLayout.NORTH);
        JPanel medio = new JPanel(new BorderLayout());
        medio.setBackground(Color.WHITE);
        medio.add(panelResumen, BorderLayout.NORTH);
        medio.add(panelObs,     BorderLayout.CENTER);
        centro.add(medio, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCerrar.setBackground(new Color(241, 245, 249));
        btnCerrar.setForeground(new Color(30, 41, 59));
        btnCerrar.setFocusPainted(false);
        btnCerrar.addActionListener(e -> dispose());

        btnCorregir = new JButton("Corregir equivalencia (Admin)");
        btnCorregir.setFont(new Font("Arial", Font.BOLD, 12));
        btnCorregir.setBackground(new Color(37, 99, 235));
        btnCorregir.setForeground(Color.WHITE);
        btnCorregir.setFocusPainted(false);
        btnCorregir.setBorderPainted(false);

        btnReprocesar = new JButton("🔄 Reprocesar");
        btnReprocesar.setFont(new Font("Arial", Font.BOLD, 12));
        btnReprocesar.setBackground(new Color(5, 150, 105));
        btnReprocesar.setForeground(Color.WHITE);
        btnReprocesar.setFocusPainted(false);
        btnReprocesar.setBorderPainted(false);

        botones.add(btnCerrar);
        botones.add(btnCorregir);
        botones.add(btnReprocesar);

        add(header, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    private JLabel crearTitulo(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        l.setForeground(new Color(71, 85, 105));
        return l;
    }

    private JLabel crearValor() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Arial", Font.PLAIN, 13));
        l.setForeground(new Color(17, 24, 39));
        return l;
    }

    private JLabel crearTarjetaResumen(String texto, Color color) {
        JLabel l = new JLabel("0", SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 22));
        l.setForeground(color);
        return l;
    }

    private JPanel envolverTarjeta(JLabel numero, String etiqueta) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));
        JLabel lbl = new JLabel(etiqueta, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.PLAIN, 11));
        lbl.setForeground(new Color(107, 114, 128));
        p.add(numero, BorderLayout.CENTER);
        p.add(lbl,    BorderLayout.SOUTH);
        return p;
    }

    public void cargarCabecera(Factura f) {
        lblNumero.setText(f.getNumeroFactura());
        lblProveedor.setText(f.getNombreProveedor());
    }

    public void cargarResumen(ResumenProcesamiento r) {
        lblLeidos.setText(String.valueOf(r.leidos()));
        lblValidos.setText(String.valueOf(r.validos()));
        lblObservadosCount.setText(String.valueOf(r.observados()));
        lblNoProcesados.setText(String.valueOf(r.noProcesados()));
    }

    public void cargarObservados(List<ItemFactura> items) {
        modeloObservados.setRowCount(0);
        idsPorFila = items.stream().map(ItemFactura::getIdItem).toList();
        for (ItemFactura it : items) {
            modeloObservados.addRow(new Object[]{
                    it.getCodigoInternoProveedor(),
                    it.getCantidadFacturada(),
                    it.getPrecioUnitarioCompra(),
                    it.getEstadoItem()
            });
        }
    }

    /** @return idItem seleccionado en la tabla de observados, o -1 si no hay selección */
    public int getIdItemSeleccionado() {
        int fila = tblObservados.getSelectedRow();
        if (fila < 0 || idsPorFila == null || fila >= idsPorFila.size()) return -1;
        return idsPorFila.get(fila);
    }

    public void habilitarCorreccion(boolean habilitar) {
        btnCorregir.setEnabled(habilitar);
        btnCorregir.setToolTipText(habilitar ? null : "Solo un Administrador puede corregir equivalencias");
    }

    public String pedirSkuCorreccion(String codigoProveedor) {
        return JOptionPane.showInputDialog(this,
                "SKU correcto para el código de proveedor '" + codigoProveedor + "':",
                "Corregir equivalencia", JOptionPane.PLAIN_MESSAGE);
    }

    public JButton getBtnCorregir()   { return btnCorregir; }
    public JButton getBtnReprocesar() { return btnReprocesar; }
    public JButton getBtnCerrar()     { return btnCerrar; }
}