package cl.antucayen.view;

import cl.antucayen.security.RolSistema;
import cl.antucayen.util.GestorTemas;
import cl.antucayen.util.Tema;
import cl.antucayen.view.components.EstiloAntucayen;
import cl.antucayen.view.components.IconoNavegacion;
import cl.antucayen.view.components.LogoAntucayen;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Cascarón gráfico común: cabecera azul, menú lateral y contenido del módulo activo. */
public class VPrincipal extends JFrame {
    private JPanel panelContenido;
    private final List<JButton> botonesNavegacion = new ArrayList<>();
    private final List<JPanel> seccionesMenu = new ArrayList<>();

    // Paleta de navegación inspirada en el menú original del minimarket.
    private static final Color SIDEBAR_FONDO = new Color(17, 24, 39);
    private static final Color SIDEBAR_RESALTADO = new Color(36, 52, 76);
    private static final Color SIDEBAR_TEXTO = new Color(192, 205, 228);
    private static final Color SIDEBAR_TITULO = new Color(110, 132, 166);
    private static final Color SIDEBAR_ACENTO = new Color(156, 194, 31);
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
        setIconImage(LogoAntucayen.imagen(64));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 640));
        setSize(1360, 820);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Cabecera continua al total de lo ancho, como en las referencias.
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(EstiloAntucayen.cabecera());
        topbar.setPreferredSize(new Dimension(0, 54));
        topbar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 22));
        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 9));
        marca.setOpaque(false);
        marca.add(new JLabel(LogoAntucayen.icono(34)));
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
        sidebar.setPreferredSize(new Dimension(224, 0));
        sidebar.setBackground(SIDEBAR_FONDO);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(33, 44, 65)));

        // Marca propia en vez del icono genérico de carrito.
        JPanel cabeceraLateral = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        cabeceraLateral.setBackground(SIDEBAR_FONDO);
        cabeceraLateral.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(42, 52, 68)));
        cabeceraLateral.add(new JLabel(LogoAntucayen.icono(43)));
        JLabel nombreLateral = new JLabel("<html><b>Antucayen</b><br><span style='font-size:9px;color:#788DAE'>Minimarket</span></html>");
        nombreLateral.setForeground(Color.WHITE);
        nombreLateral.setFont(new Font("Dialog", Font.BOLD, 14));
        cabeceraLateral.add(nombreLateral);
        sidebar.add(cabeceraLateral, BorderLayout.NORTH);

        JPanel items = new JPanel();
        items.setBackground(SIDEBAR_FONDO);
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(BorderFactory.createEmptyBorder(5, 10, 3, 10));

        // Las etiquetas de los módulos permanecen idénticas a las de la versión previa.
        btnDashboard = crearBotonMenu("Dashboard", "dashboard");
        btnVentas = crearBotonMenu("Punto de Venta", "cart");
        btnProductos = crearBotonMenu("Productos", "products");
        btnProveedores = crearBotonMenu("Proveedores", "suppliers");
        btnEquivalencias = crearBotonMenu("Equivalencias", "suppliers");
        btnFacturas = crearBotonMenu("Facturas", "facturas");
        btnProcesarFactura = crearBotonMenu("Procesar factura", "facturas");
        btnImportarInventario = crearBotonMenu("Importación", "upload");
        btnHistorial = crearBotonMenu("Historial", "history");
        btnReportes = crearBotonMenu("Reportes", "reports");
        btnUsuarios = crearBotonMenu("Usuarios", "users");
        btnAuditoriaArchivos = crearBotonMenu("Auditoría", "settings");

        items.add(crearSeccionMenu("PRINCIPAL", btnDashboard, btnVentas));
        items.add(crearSeccionMenu("INVENTARIO", btnProductos, btnProveedores, btnEquivalencias));
        items.add(crearSeccionMenu("FACTURACIÓN", btnFacturas, btnProcesarFactura));
        items.add(crearSeccionMenu("INVENTARIO MASIVO", btnImportarInventario));
        items.add(crearSeccionMenu("ANÁLISIS", btnHistorial, btnReportes));
        items.add(crearSeccionMenu("ADMINISTRACIÓN", btnUsuarios, btnAuditoriaArchivos));

        JScrollPane scrollMenu = new JScrollPane(items);
        scrollMenu.setBorder(null);
        scrollMenu.setBackground(SIDEBAR_FONDO);
        scrollMenu.getViewport().setBackground(SIDEBAR_FONDO);
        scrollMenu.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar barraDesplazamiento = scrollMenu.getVerticalScrollBar();
        barraDesplazamiento.setUnitIncrement(15);
        barraDesplazamiento.setPreferredSize(new Dimension(8, 0));
        barraDesplazamiento.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(68, 82, 105);
                trackColor = SIDEBAR_FONDO;
            }
            @Override protected JButton createDecreaseButton(int orientation) { return botonScrollOculto(); }
            @Override protected JButton createIncreaseButton(int orientation) { return botonScrollOculto(); }
            private JButton botonScrollOculto() {
                JButton boton = new JButton();
                boton.setPreferredSize(new Dimension(0, 0));
                boton.setMinimumSize(new Dimension(0, 0));
                boton.setMaximumSize(new Dimension(0, 0));
                return boton;
            }
        });
        sidebar.add(scrollMenu, BorderLayout.CENTER);

        JPanel pieMenu = new JPanel();
        pieMenu.setLayout(new BoxLayout(pieMenu, BoxLayout.Y_AXIS));
        pieMenu.setBackground(SIDEBAR_FONDO);
        pieMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(42, 52, 68)),
                BorderFactory.createEmptyBorder(7, 15, 14, 15)));
        JLabel version = new JLabel("Versión 1.0.0", SwingConstants.CENTER);
        version.setFont(new Font("Dialog", Font.PLAIN, 10));
        version.setForeground(SIDEBAR_TITULO);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        version.setBorder(BorderFactory.createEmptyBorder(7, 0, 8, 0));
        pieMenu.add(version);
        JButton btnTema = new JButton("Cambiar tema");
        btnTema.setIcon(new IconoNavegacion("settings", SIDEBAR_TEXTO, 15));
        btnTema.setForeground(SIDEBAR_TEXTO);
        btnTema.setBackground(SIDEBAR_FONDO);
        btnTema.setBorderPainted(false);
        btnTema.setFocusPainted(false);
        btnTema.setFont(new Font("Dialog", Font.PLAIN, 11));
        btnTema.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTema.setMaximumSize(new Dimension(186, 29));
        btnTema.addActionListener(e -> mostrarSelectorDeTemas());
        pieMenu.add(btnTema);
        btnCerrarSesion = new JButton("Cerrar sesión");
        btnCerrarSesion.setForeground(new Color(255, 157, 164));
        btnCerrarSesion.setBackground(SIDEBAR_FONDO);
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setFocusPainted(false);
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

    /** Secciones independientes: se ocultan si el perfil no puede acceder a ningún módulo. */
    private JPanel crearSeccionMenu(String titulo, JButton... modulos) {
        JPanel seccion = new JPanel();
        seccion.setBackground(SIDEBAR_FONDO);
        seccion.setLayout(new BoxLayout(seccion, BoxLayout.Y_AXIS));
        seccion.setAlignmentX(Component.LEFT_ALIGNMENT);
        seccion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37 * (modulos.length + 1)));
        JLabel encabezado = new JLabel(titulo);
        encabezado.setFont(new Font("Dialog", Font.BOLD, 10));
        encabezado.setForeground(SIDEBAR_TITULO);
        encabezado.setAlignmentX(Component.LEFT_ALIGNMENT);
        encabezado.setBorder(BorderFactory.createEmptyBorder(15, 13, 5, 0));
        encabezado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37));
        seccion.add(encabezado);
        for (JButton btn : modulos) seccion.add(btn);
        seccion.putClientProperty("modulos", modulos);
        seccionesMenu.add(seccion);
        return seccion;
    }

    private JButton crearBotonMenu(String texto, String icono) {
        JButton btn = new JButton(texto, new IconoNavegacion(icono, SIDEBAR_TEXTO, 17));
        btn.putClientProperty("navIcon", icono);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(11);
        btn.setFont(new Font("Dialog", Font.PLAIN, 12));
        btn.setForeground(SIDEBAR_TEXTO);
        btn.setBackground(SIDEBAR_FONDO);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37));
        btn.setPreferredSize(new Dimension(202, 37));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 4));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("seleccionado")))
                    btn.setBackground(SIDEBAR_RESALTADO);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (!Boolean.TRUE.equals(btn.getClientProperty("seleccionado")))
                    btn.setBackground(SIDEBAR_FONDO);
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
            b.setBackground(marcado ? SIDEBAR_RESALTADO : SIDEBAR_FONDO);
            b.setForeground(marcado ? Color.WHITE : SIDEBAR_TEXTO);
            b.setFont(new Font("Dialog", marcado ? Font.BOLD : Font.PLAIN, 12));
            b.setIcon(new IconoNavegacion((String)b.getClientProperty("navIcon"),
                    marcado ? SIDEBAR_ACENTO : SIDEBAR_TEXTO, 17));
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

        // No mostrar categorías vacías cuando un rol tiene acceso restringido.
        for (JPanel seccion : seccionesMenu) {
            JButton[] modulos = (JButton[]) seccion.getClientProperty("modulos");
            boolean contieneVisibles = false;
            for (JButton modulo : modulos) {
                if (modulo.isVisible()) {
                    contieneVisibles = true;
                    break;
                }
            }
            seccion.setVisible(contieneVisibles);
        }
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
