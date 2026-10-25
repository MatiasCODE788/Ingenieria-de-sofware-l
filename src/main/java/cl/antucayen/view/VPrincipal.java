package cl.antucayen.view;

import cl.antucayen.security.RolSistema;
import cl.antucayen.util.GestorTemas;
import cl.antucayen.util.Tema;
import cl.antucayen.view.components.EstiloAntucayen;
import cl.antucayen.view.components.IconoNavegacion;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Cascarón gráfico común: cabecera azul, menú lateral y contenido del módulo activo. */
public class VPrincipal extends JFrame {
    private JPanel panelContenido;
    private final List<JButton> botonesNavegacion = new ArrayList<>();
    private JButton btnDashboard, btnVentas, btnProductos, btnProveedores, btnEquivalencias;
    private JButton btnFacturas, btnProcesarFactura, btnImportarInventario, btnHistorial;
    private JButton btnReportes, btnUsuarios, btnAuditoriaArchivos, btnCerrarSesion;
    private final String perfil;
    private final String username;
    private Tema tema = GestorTemas.getInstancia().getTema();

    public VPrincipal(String username, String perfil) {
        this.username = username;
        this.perfil = perfil;
        EstiloAntucayen.iniciar();
        initComponents();
        configurarMenuSegunPerfil();
    }

    private void initComponents() {
        setTitle("Sistema Antucayen — Minimarket Antucayen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 640));
        setSize(1360, 820);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Cabecera continua a todo lo ancho, como en las referencias.
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(EstiloAntucayen.cabecera());
        topbar.setPreferredSize(new Dimension(0, 54));
        topbar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 22));
        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        marca.setOpaque(false);
        marca.add(new JLabel(new IconoNavegacion("cart", Color.WHITE, 21)));
        JLabel nombreSistema = new JLabel("Sistema Antucayen");
        nombreSistema.setForeground(Color.WHITE);
        nombreSistema.setFont(new Font("Dialog", Font.BOLD, 15));
        marca.add(nombreSistema);
        topbar.add(marca, BorderLayout.WEST);
        JPanel cuenta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        cuenta.setOpaque(false);
        JLabel comercio = new JLabel("Minimarket Antucayen");
        comercio.setForeground(new Color(212, 225, 255));
        comercio.setFont(new Font("Dialog", Font.PLAIN, 12));
        cuenta.add(comercio);
        JLabel iniciales = new JLabel(iniciales(username), SwingConstants.CENTER);
        iniciales.setOpaque(true);
        iniciales.setBackground(Color.WHITE);
        iniciales.setForeground(EstiloAntucayen.cabecera());
        iniciales.setFont(new Font("Dialog", Font.BOLD, 10));
        iniciales.setPreferredSize(new Dimension(25, 25));
        cuenta.add(iniciales);
        JLabel usuario = new JLabel(perfil + "  ·  " + username);
        usuario.setForeground(Color.WHITE);
        usuario.setFont(new Font("Dialog", Font.PLAIN, 12));
        cuenta.add(usuario);
        topbar.add(cuenta, BorderLayout.EAST);
        add(topbar, BorderLayout.NORTH);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(EstiloAntucayen.BACKGROUND);
        add(contenedor, BorderLayout.CENTER);

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(212, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, EstiloAntucayen.BORDER));
        JPanel items = new JPanel();
        items.setBackground(Color.WHITE);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        btnDashboard = crearBotonMenu("Dashboard", "dashboard"); items.add(btnDashboard);
        btnProductos = crearBotonMenu("Productos", "products"); items.add(btnProductos);
        btnProveedores = crearBotonMenu("Proveedores", "suppliers"); items.add(btnProveedores);
        btnEquivalencias = crearBotonMenu("Equivalencias", "suppliers"); items.add(btnEquivalencias);
        btnFacturas = crearBotonMenu("Facturas", "facturas"); items.add(btnFacturas);
        btnProcesarFactura = crearBotonMenu("Procesar factura", "facturas"); items.add(btnProcesarFactura);
        btnImportarInventario = crearBotonMenu("Importación", "upload"); items.add(btnImportarInventario);
        btnVentas = crearBotonMenu("Punto de Venta", "cart"); items.add(btnVentas);
        btnHistorial = crearBotonMenu("Historial", "history"); items.add(btnHistorial);
        btnReportes = crearBotonMenu("Reportes", "reports"); items.add(btnReportes);
        btnUsuarios = crearBotonMenu("Usuarios", "users"); items.add(btnUsuarios);
        btnAuditoriaArchivos = crearBotonMenu("Auditoría", "settings"); items.add(btnAuditoriaArchivos);

        JScrollPane scrollMenu = new JScrollPane(items);
        scrollMenu.setBorder(null);
        scrollMenu.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMenu.getVerticalScrollBar().setUnitIncrement(15);
        sidebar.add(scrollMenu, BorderLayout.CENTER);

        JPanel pieMenu = new JPanel();
        pieMenu.setLayout(new BoxLayout(pieMenu, BoxLayout.Y_AXIS));
        pieMenu.setBackground(Color.WHITE);
        pieMenu.setBorder(BorderFactory.createEmptyBorder(14, 15, 15, 15));
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
        logo.setBackground(EstiloAntucayen.BACKGROUND);
        logo.setBorder(BorderFactory.createLineBorder(EstiloAntucayen.BORDER));
        logo.add(new JLabel(new IconoNavegacion("cart", EstiloAntucayen.primario(), 27)));
        JLabel logoTexto = new JLabel("<html><b>MINIMARKET</b><br>ANTUCAYEN</html>");
        logoTexto.setForeground(EstiloAntucayen.primario());
        logoTexto.setFont(new Font("Dialog", Font.BOLD, 11));
        logo.add(logoTexto);
        logo.setMaximumSize(new Dimension(186, 58));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pieMenu.add(logo);
        JLabel version = new JLabel("Versión 1.0.0", SwingConstants.CENTER);
        version.setFont(new Font("Dialog", Font.PLAIN, 10));
        version.setForeground(EstiloAntucayen.MUTED);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        version.setBorder(BorderFactory.createEmptyBorder(7, 0, 8, 0));
        pieMenu.add(version);
        JButton btnTema = new JButton("Cambiar tema");
        btnTema.setIcon(new IconoNavegacion("settings", EstiloAntucayen.MUTED, 15));
        btnTema.setForeground(EstiloAntucayen.MUTED);
        btnTema.setFont(new Font("Dialog", Font.PLAIN, 11));
        btnTema.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTema.setMaximumSize(new Dimension(186, 29));
        btnTema.addActionListener(e -> mostrarSelectorDeTemas());
        pieMenu.add(btnTema);
        btnCerrarSesion = new JButton("Cerrar sesión");
        btnCerrarSesion.setForeground(new Color(183, 38, 52));
        btnCerrarSesion.setFont(new Font("Dialog", Font.BOLD, 11));
        btnCerrarSesion.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCerrarSesion.setMaximumSize(new Dimension(186, 29));
        pieMenu.add(btnCerrarSesion);
        sidebar.add(pieMenu, BorderLayout.SOUTH);
        contenedor.add(sidebar, BorderLayout.WEST);

        panelContenido = new JPanel(new BorderLayout());
        panelContenido.setBackground(EstiloAntucayen.BACKGROUND);
        contenedor.add(panelContenido, BorderLayout.CENTER);
    }

    private static String iniciales(String usuario) {
        if (usuario == null || usuario.isBlank()) return "AC";
        String[] nombres = usuario.trim().split("\\s+");
        return ("" + Character.toUpperCase(nombres[0].charAt(0)) +
                (nombres.length > 1 ? Character.toUpperCase(nombres[1].charAt(0)) : ' ')).trim();
    }

    private JButton crearBotonMenu(String texto, String icono) {
        JButton btn = new JButton(texto, new IconoNavegacion(icono, EstiloAntucayen.MUTED, 17));
        btn.putClientProperty("navIcon", icono);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(12);
        btn.setFont(new Font("Dialog", Font.PLAIN, 12));
        btn.setForeground(EstiloAntucayen.MUTED);
        btn.setBackground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(190, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 6));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("seleccionado"))) btn.setBackground(EstiloAntucayen.BACKGROUND);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("seleccionado"))) btn.setBackground(Color.WHITE);
            }
        });
        botonesNavegacion.add(btn);
        return btn;
    }

    private void seleccionarBoton(String titulo) {
        JButton elegido;
        if (titulo.contains("Dashboard")) elegido = btnDashboard;
        else if (titulo.contains("Venta")) elegido = btnVentas;
        else if (titulo.contains("Equivalencia")) elegido = btnEquivalencias;
        else if (titulo.contains("Procesar factura")) elegido = btnProcesarFactura;
        else if (titulo.contains("Factura") || titulo.contains("Facturas")) elegido = btnFacturas;
        else if (titulo.contains("Proveedor")) elegido = btnProveedores;
        else if (titulo.contains("Producto") || titulo.contains("Stock")) elegido = btnProductos;
        else if (titulo.contains("Importar") || titulo.contains("Ajuste")) elegido = btnImportarInventario;
        else if (titulo.contains("Historial")) elegido = btnHistorial;
        else if (titulo.contains("Reporte")) elegido = btnReportes;
        else if (titulo.contains("Usuario")) elegido = btnUsuarios;
        else if (titulo.contains("Auditoría")) elegido = btnAuditoriaArchivos;
        else elegido = null;
        for (JButton b : botonesNavegacion) {
            boolean marcado = b == elegido;
            b.putClientProperty("seleccionado", marcado);
            b.setBackground(marcado ? EstiloAntucayen.PALE : Color.WHITE);
            b.setForeground(marcado ? EstiloAntucayen.primario() : EstiloAntucayen.MUTED);
            b.setFont(new Font("Dialog", marcado ? Font.BOLD : Font.PLAIN, 12));
            b.setIcon(new IconoNavegacion((String)b.getClientProperty("navIcon"),
                    marcado ? EstiloAntucayen.primario() : EstiloAntucayen.MUTED, 17));
        }
    }

    private void configurarMenuSegunPerfil() {
        boolean admin = RolSistema.ADMINISTRADOR.coincide(perfil);
        boolean bodeguero = RolSistema.BODEGUERO.coincide(perfil);
        boolean cajero = RolSistema.CAJERO.coincide(perfil);
        boolean valido = admin || bodeguero || cajero;
        btnDashboard.setVisible(admin);
        btnVentas.setVisible(admin || cajero);
        btnProductos.setVisible(valido);
        btnProveedores.setVisible(admin || bodeguero);
        btnEquivalencias.setVisible(admin || bodeguero);
        btnFacturas.setVisible(admin || bodeguero);
        btnProcesarFactura.setVisible(admin || bodeguero);
        btnImportarInventario.setVisible(admin || bodeguero);
        btnHistorial.setVisible(valido);
        btnReportes.setVisible(valido);
        btnUsuarios.setVisible(admin);
        btnAuditoriaArchivos.setVisible(admin);
    }

    private void mostrarSelectorDeTemas() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        ButtonGroup grupo = new ButtonGroup();
        java.util.Map<Tema, JRadioButton> radios = new java.util.LinkedHashMap<>();
        for (Tema opcion : Tema.values()) {
            JRadioButton rb = new JRadioButton(opcion.getNombre(), opcion == tema);
            grupo.add(rb);
            radios.put(opcion, rb);
            panel.add(rb);
        }
        int resultado = JOptionPane.showConfirmDialog(this, panel, "Elegir tema del sistema",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado == JOptionPane.OK_OPTION) {
            for (var entry : radios.entrySet()) {
                if (entry.getValue().isSelected()) {
                    GestorTemas.getInstancia().setTema(entry.getKey());
                    break;
                }
            }
            dispose();
            VPrincipal nueva = new VPrincipal(username, perfil);
            new cl.antucayen.controller.ControladorPrincipal(nueva);
            nueva.setVisible(true);
        }
    }

    /** Acoge cualquier vista sin modificar sus datos, listeners, acciones o permisos. */
    public void setContenido(JPanel panel, String titulo) {
        EstiloAntucayen.aplicar(panel);
        seleccionarBoton(titulo);
        panelContenido.removeAll();
        JPanel pagina = new JPanel(new BorderLayout(0, 13));
        pagina.setBackground(EstiloAntucayen.BACKGROUND);
        pagina.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 18));
        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBackground(EstiloAntucayen.BACKGROUND);
        JLabel tituloVista = new JLabel(titulo);
        tituloVista.setFont(new Font("Dialog", Font.BOLD, 19));
        tituloVista.setForeground(EstiloAntucayen.TEXT);
        encabezado.add(tituloVista);
        JLabel subtitulo = new JLabel(descripcion(titulo));
        subtitulo.setFont(new Font("Dialog", Font.PLAIN, 11));
        subtitulo.setForeground(EstiloAntucayen.MUTED);
        subtitulo.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        encabezado.add(subtitulo);
        pagina.add(encabezado, BorderLayout.NORTH);
        pagina.add(panel, BorderLayout.CENTER);
        panelContenido.add(pagina, BorderLayout.CENTER);
        panelContenido.revalidate();
        panelContenido.repaint();
    }

    private static String descripcion(String titulo) {
        if (titulo.contains("Dashboard")) return "Resumen general de la operación del minimarket.";
        if (titulo.contains("Venta")) return "Registro de ventas, cobros y comprobantes de caja.";
        if (titulo.contains("Productos") || titulo.contains("Stock")) return "Administra y consulta los productos del inventario del minimarket.";
        if (titulo.contains("Proveedores")) return "Administra proveedores y consulta sus datos de contacto.";
        if (titulo.contains("Equivalencia")) return "Consulta y administra las equivalencias de los productos de proveedores.";
        if (titulo.contains("Factura")) return "Registra y consulta las facturas de compra de proveedores.";
        if (titulo.contains("Importar")) return "Importa archivos de actualización del inventario y valida sus resultados.";
        if (titulo.contains("Historial")) return "Consulta los movimientos y ajustes de inventario.";
        if (titulo.contains("Reporte")) return "Consulta y genera reportes de inventario, facturas y movimientos.";
        if (titulo.contains("Usuario")) return "Administra los usuarios y permisos del sistema.";
        if (titulo.contains("Auditoría")) return "Revisa la trazabilidad de los archivos procesados.";
        return "Minimarket Antucayen · Plataforma de gestión.";
    }

    public JButton getBtnDashboard() { return btnDashboard; }
    public JButton getBtnVentas() { return btnVentas; }
    public JButton getBtnProductos() { return btnProductos; }
    public JButton getBtnProveedores() { return btnProveedores; }
    public JButton getBtnEquivalencias() { return btnEquivalencias; }
    public JButton getBtnFacturas() { return btnFacturas; }
    public JButton getBtnProcesarFactura() { return btnProcesarFactura; }
    public JButton getBtnImportarInventario() { return btnImportarInventario; }
    public JButton getBtnHistorial() { return btnHistorial; }
    public JButton getBtnReportes() { return btnReportes; }
    public JButton getBtnUsuarios() { return btnUsuarios; }
    public JButton getBtnAuditoriaArchivos() { return btnAuditoriaArchivos; }
    public JButton getBtnCerrarSesion() { return btnCerrarSesion; }
}
