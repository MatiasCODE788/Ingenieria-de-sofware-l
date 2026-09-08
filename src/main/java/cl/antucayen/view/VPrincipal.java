package cl.antucayen.view;

import cl.antucayen.util.GestorTemas;
import cl.antucayen.util.Tema;

import javax.swing.*;
import java.awt.*;

public class VPrincipal extends JFrame {

    private JPanel  panelContenido;
    private JLabel  lblTituloPagina;

    // Botones sidebar
    private JButton btnDashboard;
    private JButton btnVentas;
    private JButton btnProductos;
    private JButton btnProveedores;
    private JButton btnEquivalencias;
    private JButton btnFacturas;
    private JButton btnProcesarFactura;
    private JButton btnImportarInventario;
    private JButton btnHistorial;
    private JButton btnUsuarios;
    private JButton btnCerrarSesion;

    private String perfil;
    private String username;
    private Tema   tema = GestorTemas.getInstancia().getTema();

    public VPrincipal(String username, String perfil) {
        this.username = username;
        this.perfil   = perfil;
        initComponents();
        configurarMenuSegunPerfil();
    }

    private void initComponents() {
        setTitle("Minimarket Antucayen — Sistema de Gestión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── SIDEBAR ───────────────────────────────────────────────
        JPanel sidebar = new JPanel();
        sidebar.setBackground(tema.sidebarFondo);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BorderLayout());

        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(tema.sidebarFondo);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

        // Brand
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 16));
        brand.setBackground(tema.sidebarFondo);
        brand.setMaximumSize(new Dimension(220, 64));
        JLabel iconoBrand = new JLabel("🏪");
        iconoBrand.setFont(new Font("Arial", Font.PLAIN, 24));
        JPanel brandTexto = new JPanel();
        brandTexto.setBackground(tema.sidebarFondo);
        brandTexto.setLayout(new BoxLayout(brandTexto, BoxLayout.Y_AXIS));
        JLabel lblBrand = new JLabel("Antucayen");
        lblBrand.setFont(new Font("Arial", Font.BOLD, 14));
        lblBrand.setForeground(Color.WHITE);
        JLabel lblBrandSub = new JLabel("Minimarket");
        lblBrandSub.setFont(new Font("Arial", Font.PLAIN, 10));
        lblBrandSub.setForeground(new Color(107, 114, 128));
        brandTexto.add(lblBrand);
        brandTexto.add(lblBrandSub);
        brand.add(iconoBrand);
        brand.add(brandTexto);

        menuPanel.add(brand);
        menuPanel.add(crearSeparador());

        // Secciones del menú
        menuPanel.add(crearSeccion("PRINCIPAL"));
        btnDashboard = crearBotonMenu("📊  Dashboard");
        menuPanel.add(btnDashboard);

        menuPanel.add(crearSeccion("VENTAS"));
        btnVentas = crearBotonMenu("🛒  Punto de Venta");
        menuPanel.add(btnVentas);

        menuPanel.add(crearSeccion("INVENTARIO"));
        btnProductos  = crearBotonMenu("📦  Productos");
        btnProveedores = crearBotonMenu("🏭  Proveedores");
        btnEquivalencias = crearBotonMenu("🔗  Equivalencias");
        menuPanel.add(btnProductos);
        menuPanel.add(btnProveedores);
        menuPanel.add(btnEquivalencias);

        menuPanel.add(crearSeccion("FACTURACIÓN"));
        btnFacturas        = crearBotonMenu("📄  Facturas");
        btnProcesarFactura = crearBotonMenu("⚙️  Procesar factura");
        menuPanel.add(btnFacturas);
        menuPanel.add(btnProcesarFactura);

        menuPanel.add(crearSeccion("INVENTARIO MASIVO"));
        btnImportarInventario = crearBotonMenu("📂  Importar inventario");
        menuPanel.add(btnImportarInventario);

        menuPanel.add(crearSeccion("ANÁLISIS"));
        btnHistorial = crearBotonMenu("🕒  Historial");
        menuPanel.add(btnHistorial);

        menuPanel.add(crearSeccion("ADMINISTRACIÓN"));
        btnUsuarios = crearBotonMenu("👥  Usuarios y permisos");
        menuPanel.add(btnUsuarios);

        menuPanel.add(Box.createVerticalGlue());

        // Panel usuario inferior
        JPanel panelUser = new JPanel();
        panelUser.setBackground(tema.sidebarHoverFondo);
        panelUser.setLayout(new BoxLayout(panelUser, BoxLayout.Y_AXIS));
        panelUser.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel lblUsername = new JLabel(username);
        lblUsername.setFont(new Font("Arial", Font.BOLD, 12));
        lblUsername.setForeground(Color.WHITE);
        JLabel lblPerfil = new JLabel(perfil);
        lblPerfil.setFont(new Font("Arial", Font.PLAIN, 11));
        lblPerfil.setForeground(new Color(107, 114, 128));

        btnCerrarSesion = new JButton("Cerrar sesión");
        btnCerrarSesion.setFont(new Font("Arial", Font.BOLD, 11));
        btnCerrarSesion.setBackground(new Color(185, 28, 28));
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarSesion.setMaximumSize(new Dimension(200, 30));
        btnCerrarSesion.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnTema = new JButton("🎨 Cambiar tema");
        btnTema.setFont(new Font("Arial", Font.PLAIN, 11));
        btnTema.setForeground(tema.sidebarTextoInactivo);
        btnTema.setBackground(tema.sidebarHoverFondo);
        btnTema.setFocusPainted(false);
        btnTema.setBorderPainted(false);
        btnTema.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTema.setMaximumSize(new Dimension(200, 26));
        btnTema.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnTema.addActionListener(e -> mostrarSelectorDeTemas());

        panelUser.add(lblUsername);
        panelUser.add(lblPerfil);
        panelUser.add(Box.createVerticalStrut(8));
        panelUser.add(btnTema);
        panelUser.add(Box.createVerticalStrut(6));
        panelUser.add(btnCerrarSesion);

        sidebar.add(menuPanel, BorderLayout.CENTER);
        sidebar.add(panelUser, BorderLayout.SOUTH);

        // ── ÁREA PRINCIPAL ────────────────────────────────────────
        JPanel areaPrincipal = new JPanel(new BorderLayout());
        areaPrincipal.setBackground(tema.areaFondo);

        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setPreferredSize(new Dimension(0, 56));
        topbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)));

        lblTituloPagina = new JLabel("  Dashboard");
        lblTituloPagina.setFont(new Font("Arial", Font.BOLD, 16));
        lblTituloPagina.setForeground(new Color(17, 24, 39));
        topbar.add(lblTituloPagina, BorderLayout.WEST);

        panelContenido = new JPanel(new BorderLayout());
        panelContenido.setBackground(tema.areaFondo);
        panelContenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Mensaje de bienvenida
        JLabel lblBienvenida = new JLabel(
                "<html><center><br><br><span style='font-size:20px'>👋 Bienvenido, " + username + "</span>"
                        + "<br><br><span style='color:#6B7280'>Selecciona una opción del menú lateral</span></center></html>",
                SwingConstants.CENTER
        );
        lblBienvenida.setFont(new Font("Arial", Font.PLAIN, 14));
        panelContenido.add(lblBienvenida, BorderLayout.CENTER);

        areaPrincipal.add(topbar, BorderLayout.NORTH);
        areaPrincipal.add(panelContenido, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(areaPrincipal, BorderLayout.CENTER);
    }

    private JButton crearBotonMenu(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.PLAIN, 13));
        btn.setForeground(tema.sidebarTextoInactivo);
        btn.setBackground(tema.sidebarFondo);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(220, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(tema.sidebarHoverFondo);
                btn.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(tema.sidebarFondo);
                btn.setForeground(tema.sidebarTextoInactivo);
            }
        });
        return btn;
    }

    private JLabel crearSeccion(String texto) {
        JLabel lbl = new JLabel("  " + texto);
        lbl.setFont(new Font("Arial", Font.BOLD, 10));
        lbl.setForeground(new Color(75, 85, 99));
        lbl.setBorder(BorderFactory.createEmptyBorder(12, 10, 4, 0));
        lbl.setMaximumSize(new Dimension(220, 28));
        return lbl;
    }

    private JSeparator crearSeparador() {
        JSeparator sep = new JSeparator();
        sep.setForeground(tema.sidebarHoverFondo);
        sep.setMaximumSize(new Dimension(220, 1));
        return sep;
    }

    private void configurarMenuSegunPerfil() {
        boolean esAdmin    = "Administrador".equalsIgnoreCase(perfil);
        boolean esBodeguero = "Bodeguero".equalsIgnoreCase(perfil);

        btnFacturas.setVisible(esAdmin || esBodeguero);
        btnProcesarFactura.setVisible(esAdmin || esBodeguero);
        btnImportarInventario.setVisible(esAdmin || esBodeguero);
        btnUsuarios.setVisible(esAdmin);
    }

    /** Diálogo simple para elegir uno de los 5 temas; al confirmar, reabre la pantalla con el tema aplicado. */
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

    public void setContenido(JPanel panel, String titulo) {
        panelContenido.removeAll();
        panelContenido.add(panel, BorderLayout.CENTER);
        lblTituloPagina.setText("  " + titulo);
        panelContenido.revalidate();
        panelContenido.repaint();
    }

    public JButton getBtnDashboard()          { return btnDashboard; }
    public JButton getBtnVentas()              { return btnVentas; }
    public JButton getBtnProductos()          { return btnProductos; }
    public JButton getBtnProveedores()        { return btnProveedores; }
    public JButton getBtnEquivalencias()      { return btnEquivalencias; }
    public JButton getBtnFacturas()           { return btnFacturas; }
    public JButton getBtnProcesarFactura()    { return btnProcesarFactura; }
    public JButton getBtnImportarInventario() { return btnImportarInventario; }
    public JButton getBtnHistorial()          { return btnHistorial; }
    public JButton getBtnUsuarios()           { return btnUsuarios; }
    public JButton getBtnCerrarSesion()       { return btnCerrarSesion; }
}