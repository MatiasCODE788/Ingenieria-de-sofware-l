package cl.antucayen.view;

import javax.swing.*;
import java.awt.*;

public class VPrincipal extends JFrame {

    // Paneles principales
    private JPanel panelSidebar;
    private JPanel panelContenido;
    private JLabel lblTituloPagina;

    // Botones del menú (visibilidad según rol)
    private JButton btnProductos;
    private JButton btnProveedores;
    private JButton btnInventario;
    private JButton btnUsuarios;
    private JButton btnHistorial;
    private JButton btnCerrarSesion;

    // Rol del usuario actual
    private String rolUsuario;
    private String nombreUsuario;

    public VPrincipal(String nombreUsuario, String rolUsuario) {
        this.nombreUsuario = nombreUsuario;
        this.rolUsuario = rolUsuario;
        initComponents();
        configurarMenuSegunRol();
    }

    private void initComponents() {
        setTitle("Minimarket Antucayen — Sistema de Gestión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── SIDEBAR ──────────────────────────────────────────────
        panelSidebar = new JPanel();
        panelSidebar.setBackground(new Color(30, 48, 84));
        panelSidebar.setPreferredSize(new Dimension(220, 0));
        panelSidebar.setLayout(new BorderLayout());

        // Panel superior del sidebar (logo + menú)
        JPanel panelMenu = new JPanel();
        panelMenu.setBackground(new Color(30, 48, 84));
        panelMenu.setLayout(new BoxLayout(panelMenu, BoxLayout.Y_AXIS));
        panelMenu.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Logo/Brand
        JPanel panelBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 16));
        panelBrand.setBackground(new Color(30, 48, 84));
        panelBrand.setMaximumSize(new Dimension(220, 60));

        JLabel lblIcono = new JLabel("🏪");
        lblIcono.setFont(new Font("Arial", Font.PLAIN, 22));

        JPanel panelBrandTexto = new JPanel();
        panelBrandTexto.setBackground(new Color(30, 48, 84));
        panelBrandTexto.setLayout(new BoxLayout(panelBrandTexto, BoxLayout.Y_AXIS));
        JLabel lblNombre = new JLabel("Antucayen");
        lblNombre.setFont(new Font("Arial", Font.BOLD, 14));
        lblNombre.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel("Gestión de Inventario");
        lblSub.setFont(new Font("Arial", Font.PLAIN, 10));
        lblSub.setForeground(new Color(150, 170, 210));
        panelBrandTexto.add(lblNombre);
        panelBrandTexto.add(lblSub);

        panelBrand.add(lblIcono);
        panelBrand.add(panelBrandTexto);

        // Separador
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(60, 80, 120));
        sep1.setMaximumSize(new Dimension(220, 1));

        // Sección menú
        JLabel lblSeccionMenu = new JLabel("  MENÚ PRINCIPAL");
        lblSeccionMenu.setFont(new Font("Arial", Font.BOLD, 10));
        lblSeccionMenu.setForeground(new Color(100, 130, 180));
        lblSeccionMenu.setBorder(BorderFactory.createEmptyBorder(14, 10, 4, 0));
        lblSeccionMenu.setMaximumSize(new Dimension(220, 30));

        // Botones del menú
        btnProductos    = crearBotonMenu("📦  Productos");
        btnProveedores  = crearBotonMenu("🏭  Proveedores");
        btnInventario   = crearBotonMenu("📋  Inventario");
        btnUsuarios     = crearBotonMenu("👥  Usuarios");
        btnHistorial    = crearBotonMenu("🕒  Historial");

        panelMenu.add(panelBrand);
        panelMenu.add(sep1);
        panelMenu.add(lblSeccionMenu);
        panelMenu.add(btnProductos);
        panelMenu.add(btnProveedores);
        panelMenu.add(btnInventario);
        panelMenu.add(btnUsuarios);
        panelMenu.add(btnHistorial);
        panelMenu.add(Box.createVerticalGlue());

        // Panel inferior del sidebar (usuario + cerrar sesión)
        JPanel panelUsuario = new JPanel();
        panelUsuario.setBackground(new Color(22, 36, 66));
        panelUsuario.setLayout(new BoxLayout(panelUsuario, BoxLayout.Y_AXIS));
        panelUsuario.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        panelUsuario.setMaximumSize(new Dimension(220, 90));

        JLabel lblNombreUsuario = new JLabel(nombreUsuario);
        lblNombreUsuario.setFont(new Font("Arial", Font.BOLD, 12));
        lblNombreUsuario.setForeground(Color.WHITE);

        JLabel lblRol = new JLabel(rolUsuario);
        lblRol.setFont(new Font("Arial", Font.PLAIN, 11));
        lblRol.setForeground(new Color(150, 170, 210));

        btnCerrarSesion = new JButton("Cerrar sesión");
        btnCerrarSesion.setFont(new Font("Arial", Font.BOLD, 11));
        btnCerrarSesion.setBackground(new Color(180, 40, 40));
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarSesion.setBorderPainted(false);
        btnCerrarSesion.setMaximumSize(new Dimension(200, 30));
        btnCerrarSesion.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelUsuario.add(lblNombreUsuario);
        panelUsuario.add(lblRol);
        panelUsuario.add(Box.createVerticalStrut(8));
        panelUsuario.add(btnCerrarSesion);

        panelSidebar.add(panelMenu, BorderLayout.CENTER);
        panelSidebar.add(panelUsuario, BorderLayout.SOUTH);

        // ── ÁREA DE CONTENIDO ─────────────────────────────────────
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setBackground(new Color(240, 244, 248));

        // Topbar
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setPreferredSize(new Dimension(0, 54));
        topbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 227, 237)));

        lblTituloPagina = new JLabel("  Dashboard");
        lblTituloPagina.setFont(new Font("Arial", Font.BOLD, 16));
        lblTituloPagina.setForeground(new Color(30, 41, 59));
        topbar.add(lblTituloPagina, BorderLayout.WEST);

        // Contenido central
        panelContenido = new JPanel(new BorderLayout());
        panelContenido.setBackground(new Color(240, 244, 248));
        panelContenido.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Mensaje de bienvenida por defecto
        JLabel lblBienvenida = new JLabel(
                "<html><center><br><br><span style='font-size:18px'>👋 Bienvenido, "
                        + nombreUsuario + "</span><br><br>"
                        + "<span style='color:#64748B'>Selecciona una opción del menú para comenzar</span></center></html>"
        );
        lblBienvenida.setHorizontalAlignment(SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("Arial", Font.PLAIN, 14));
        panelContenido.add(lblBienvenida, BorderLayout.CENTER);

        panelDerecho.add(topbar, BorderLayout.NORTH);
        panelDerecho.add(panelContenido, BorderLayout.CENTER);

        add(panelSidebar, BorderLayout.WEST);
        add(panelDerecho, BorderLayout.CENTER);
    }

    private JButton crearBotonMenu(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Arial", Font.PLAIN, 13));
        btn.setForeground(new Color(180, 200, 230));
        btn.setBackground(new Color(30, 48, 84));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(220, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(45, 65, 105));
                btn.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(30, 48, 84));
                btn.setForeground(new Color(180, 200, 230));
            }
        });
        return btn;
    }

    private void configurarMenuSegunRol() {
        // Por defecto todos pueden ver estos
        btnProductos.setVisible(true);
        btnProveedores.setVisible(true);
        btnInventario.setVisible(true);
        btnHistorial.setVisible(true);

        // Solo Administrador ve Usuarios
        btnUsuarios.setVisible(rolUsuario.equalsIgnoreCase("Administrador"));
    }

    // Método para cambiar el contenido central
    public void setContenido(JPanel panel, String titulo) {
        panelContenido.removeAll();
        panelContenido.add(panel, BorderLayout.CENTER);
        lblTituloPagina.setText("  " + titulo);
        panelContenido.revalidate();
        panelContenido.repaint();
    }

    // Getters para el Controlador
    public JButton getBtnProductos()    { return btnProductos; }
    public JButton getBtnProveedores()  { return btnProveedores; }
    public JButton getBtnInventario()   { return btnInventario; }
    public JButton getBtnUsuarios()     { return btnUsuarios; }
    public JButton getBtnHistorial()    { return btnHistorial; }
    public JButton getBtnCerrarSesion() { return btnCerrarSesion; }
}