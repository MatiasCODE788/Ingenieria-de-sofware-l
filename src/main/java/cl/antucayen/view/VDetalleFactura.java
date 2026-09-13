package cl.antucayen.view;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.view.components.ComponentesSwing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.util.List;

public class VDetalleFactura extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private JLabel lblNumero;
    private JLabel lblFecha;
    private JLabel lblProveedor;
    private JLabel lblEstado;
    private JLabel lblArchivo;
    private JTable tblItems;
    private DefaultTableModel modeloItems;
    private JButton btnAbrirArchivo;
    private JButton btnProcesar;
    private JButton btnObservar;
    private JButton btnProcesarItems;
    private JButton btnCerrar;
    private String rutaArchivo;

    public VDetalleFactura(JFrame parent) {
        super(parent, "Detalle de Factura", true);
        initComponents();
    }

    private void initComponents() {
        setSize(660, 510);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
        header.setBackground(new Color(17, 24, 39));
        JLabel lblTitulo = new JLabel("Detalle de Factura");
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

        gbc.gridx = 0; gbc.gridy = 0; cabecera.add(crearTitulo("Número"), gbc);
        gbc.gridx = 1; cabecera.add(crearTitulo("Fecha de emisión"), gbc);
        lblNumero = crearValor();
        lblFecha = crearValor();
        gbc.gridx = 0; gbc.gridy = 1; cabecera.add(lblNumero, gbc);
        gbc.gridx = 1; cabecera.add(lblFecha, gbc);

        gbc.gridx = 0; gbc.gridy = 2; cabecera.add(crearTitulo("Proveedor"), gbc);
        gbc.gridx = 1; cabecera.add(crearTitulo("Estado"), gbc);
        lblProveedor = crearValor();
        lblEstado = crearValor();
        gbc.gridx = 0; gbc.gridy = 3; cabecera.add(lblProveedor, gbc);
        gbc.gridx = 1; cabecera.add(lblEstado, gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 4;
        cabecera.add(crearTitulo("Archivo digital adjunto"), gbc);

        JPanel panelArchivo = new JPanel(new BorderLayout(8, 0));
        panelArchivo.setBackground(Color.WHITE);
        lblArchivo = crearValor();
        btnAbrirArchivo = new JButton("Abrir archivo");
        btnAbrirArchivo.setFont(new Font("Arial", Font.BOLD, 11));
        btnAbrirArchivo.setFocusPainted(false);
        btnAbrirArchivo.setEnabled(false);
        panelArchivo.add(lblArchivo, BorderLayout.CENTER);
        panelArchivo.add(btnAbrirArchivo, BorderLayout.EAST);
        gbc.gridy = 5;
        cabecera.add(panelArchivo, gbc);

        JPanel panelItems = new JPanel(new BorderLayout(0, 6));
        panelItems.setBackground(Color.WHITE);
        panelItems.setBorder(BorderFactory.createEmptyBorder(6, 24, 8, 24));

        JLabel lblItems = new JLabel("Ítems");
        lblItems.setFont(new Font("Arial", Font.BOLD, 13));
        lblItems.setForeground(new Color(17, 24, 39));

        String[] cols = {"Código proveedor", "SKU", "Cantidad", "Precio unitario", "Estado ítem"};
        modeloItems = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblItems = ComponentesSwing.crearTabla(modeloItems);
        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.setPreferredSize(new Dimension(0, 180));

        panelItems.add(lblItems, BorderLayout.NORTH);
        panelItems.add(scrollItems, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Color.WHITE);
        centro.add(cabecera, BorderLayout.NORTH);
        centro.add(panelItems, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        botones.setBackground(new Color(248, 250, 252));
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 231, 235)));

        btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Arial", Font.BOLD, 12));
        btnCerrar.setBackground(new Color(241, 245, 249));
        btnCerrar.setForeground(new Color(30, 41, 59));
        btnCerrar.setFocusPainted(false);
        btnCerrar.addActionListener(e -> dispose());

        btnObservar = new JButton("Marcar Observada");
        btnObservar.setFont(new Font("Arial", Font.BOLD, 12));
        btnObservar.setBackground(new Color(217, 119, 6));
        btnObservar.setForeground(Color.WHITE);
        btnObservar.setFocusPainted(false);
        btnObservar.setBorderPainted(false);

        btnProcesar = new JButton("Marcar Procesada");
        btnProcesar.setFont(new Font("Arial", Font.BOLD, 12));
        btnProcesar.setBackground(new Color(5, 150, 105));
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.setFocusPainted(false);
        btnProcesar.setBorderPainted(false);

        btnProcesarItems = new JButton("Procesar Ítems");
        btnProcesarItems.setFont(new Font("Arial", Font.BOLD, 12));
        btnProcesarItems.setBackground(new Color(37, 99, 235));
        btnProcesarItems.setForeground(Color.WHITE);
        btnProcesarItems.setFocusPainted(false);
        btnProcesarItems.setBorderPainted(false);

        botones.add(btnCerrar);
        botones.add(btnObservar);
        botones.add(btnProcesarItems);
        botones.add(btnProcesar);

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

    public void cargarCabecera(Factura f) {
        lblNumero.setText(f.getNumeroFactura());
        lblFecha.setText(f.getFechaEmision() != null ? FORMATO_FECHA.format(f.getFechaEmision()) : "-");
        lblProveedor.setText(f.getNombreProveedor());
        lblEstado.setText(f.getEstado());

        rutaArchivo = f.getRutaArchivoDigital();
        boolean tieneAdjunto = rutaArchivo != null && !rutaArchivo.isBlank();
        lblArchivo.setText(tieneAdjunto ? rutaArchivo : "(sin adjunto)");
        btnAbrirArchivo.setEnabled(tieneAdjunto && new File(rutaArchivo).isFile());
        btnAbrirArchivo.setToolTipText(tieneAdjunto && !new File(rutaArchivo).isFile()
                ? "La ruta registrada no existe en este equipo" : null);

        boolean procesada = "Procesada".equals(f.getEstado());
        btnProcesar.setEnabled(!procesada);
        btnObservar.setEnabled(!procesada);
        // El controlador decide si Procesar Ítems queda disponible para intentar
        // un reproceso según perfil, incluso cuando la factura ya está Procesada.
        btnProcesarItems.setEnabled(true);
    }

    public void cargarItems(List<ItemFactura> items) {
        modeloItems.setRowCount(0);
        for (ItemFactura it : items) {
            modeloItems.addRow(new Object[]{
                    it.getCodigoInternoProveedor() != null ? it.getCodigoInternoProveedor() : "-",
                    it.getSku() != null ? it.getSku() : "(sin resolver)",
                    it.getCantidadFacturada(),
                    it.getPrecioUnitarioCompra(),
                    it.getEstadoItem()
            });
        }
    }

    public String getRutaArchivo() { return rutaArchivo; }
    public JButton getBtnAbrirArchivo() { return btnAbrirArchivo; }
    public JButton getBtnProcesar() { return btnProcesar; }
    public JButton getBtnObservar() { return btnObservar; }
    public JButton getBtnProcesarItems() { return btnProcesarItems; }
    public JButton getBtnCerrar() { return btnCerrar; }
}
