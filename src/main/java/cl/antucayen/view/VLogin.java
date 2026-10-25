package cl.antucayen.view;

import cl.antucayen.security.ControlInactividadSesion;
import cl.antucayen.util.GestorTemas;
import cl.antucayen.util.Tema;
import cl.antucayen.view.components.EstiloAntucayen;
import cl.antucayen.view.components.IconoNavegacion;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Acceso con composición ilustración + formulario, manteniendo el flujo de autenticación. */
public class VLogin extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private JButton btnConfigurarConexion;
    private JLabel lblError;
    private Runnable onTemaCambiado;
    private final Tema tema = GestorTemas.getInstancia().getTema();

    public VLogin() {
        EstiloAntucayen.iniciar();
        initComponents();
    }

    private void initComponents() {
        setTitle("Sistema Antucayen — Inicio de sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 540));
        setSize(1100, 700);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 13, 13));
        barra.setPreferredSize(new Dimension(0, 54));
        barra.setBackground(EstiloAntucayen.cabecera());
        barra.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        barra.add(new JLabel(new IconoNavegacion("cart", Color.WHITE, 21)));
        JLabel encabezado = new JLabel("Sistema Antucayen");
        encabezado.setFont(new Font("Dialog", Font.BOLD, 15));
        encabezado.setForeground(Color.WHITE);
        barra.add(encabezado);
        add(barra, BorderLayout.NORTH);

        JPanel fondo = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D p = (Graphics2D)g.create();
                Color inicio = tema == Tema.AZUL_CORPORATIVO ? new Color(248, 250, 254) : tema.areaFondo;
                p.setPaint(new GradientPaint(0, 0, inicio, getWidth(), getHeight(),
                        new Color(229, 237, 250)));
                p.fillRect(0, 0, getWidth(), getHeight());
                p.dispose();
            }
        };
        JPanel tarjeta = new JPanel(new GridLayout(1, 2));
        tarjeta.setBackground(Color.WHITE);
        tarjeta.setPreferredSize(new Dimension(860, 465));
        tarjeta.setBorder(BorderFactory.createLineBorder(EstiloAntucayen.BORDER));
        tarjeta.add(new IlustracionTienda());

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createEmptyBorder(25, 32, 20, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);

        c.gridy = 0;
        JLabel titulo = new JLabel("Sistema Antucayen");
        titulo.setFont(new Font("Dialog", Font.BOLD, 24));
        titulo.setForeground(EstiloAntucayen.TEXT);
        formulario.add(titulo, c);
        c.gridy++;
        c.insets = new Insets(4, 0, 23, 0);
        JLabel sub = new JLabel("Plataforma de gestión para Minimarket Antucayen.");
        sub.setFont(new Font("Dialog", Font.PLAIN, 11));
        sub.setForeground(EstiloAntucayen.MUTED);
        formulario.add(sub, c);

        c.gridy++; c.insets = new Insets(0, 0, 5, 0);
        formulario.add(etiqueta("Usuario"), c);
        txtUsername = new JTextField();
        txtUsername.setPreferredSize(new Dimension(0, 40));
        txtUsername.putClientProperty("JTextField.placeholderText", "Ingresa tu usuario");
        c.gridy++; c.insets = new Insets(0, 0, 15, 0);
        formulario.add(txtUsername, c);

        c.gridy++; c.insets = new Insets(0, 0, 5, 0);
        formulario.add(etiqueta("Contraseña"), c);
        txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(0, 40));
        txtPassword.putClientProperty("JTextField.placeholderText", "Ingresa tu contraseña");
        char ecoOriginal = txtPassword.getEchoChar();
        JButton mostrar = new JButton("Mostrar contraseña");
        mostrar.setFont(new Font("Dialog", Font.PLAIN, 10));
        mostrar.setForeground(EstiloAntucayen.primario());
        mostrar.setBorderPainted(false);
        mostrar.setContentAreaFilled(false);
        mostrar.setFocusPainted(false);
        mostrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mostrar.addActionListener(e -> {
            boolean oculto = txtPassword.getEchoChar() == ecoOriginal;
            txtPassword.setEchoChar(oculto ? (char)0 : ecoOriginal);
            mostrar.setText(oculto ? "Ocultar contraseña" : "Mostrar contraseña");
        });
        JPanel password = new JPanel(new BorderLayout());
        password.setOpaque(false);
        password.add(txtPassword, BorderLayout.CENTER);
        c.gridy++; c.insets = new Insets(0, 0, 2, 0);
        formulario.add(password, c);
        c.gridy++; c.insets = new Insets(0, 0, 14, 0);
        formulario.add(mostrar, c);

        btnIngresar = new JButton("Ingresar");
        btnIngresar.setFont(new Font("Dialog", Font.BOLD, 13));
        btnIngresar.setBackground(EstiloAntucayen.primario());
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setPreferredSize(new Dimension(0, 42));
        btnIngresar.setFocusPainted(false);
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.gridy++; c.insets = new Insets(0, 0, 5, 0);
        formulario.add(btnIngresar, c);

        btnConfigurarConexion = new JButton("Configurar conexión a la base de datos (Wi-Fi/LAN)");
        btnConfigurarConexion.setFont(new Font("Dialog", Font.PLAIN, 11));
        btnConfigurarConexion.setForeground(EstiloAntucayen.primario());
        btnConfigurarConexion.setContentAreaFilled(false);
        btnConfigurarConexion.setFocusPainted(false);
        btnConfigurarConexion.setBorderPainted(false);
        btnConfigurarConexion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.gridy++; c.insets = new Insets(2, 0, 4, 0);
        formulario.add(btnConfigurarConexion, c);

        lblError = new JLabel(" ");
        lblError.setForeground(new Color(185, 28, 28));
        lblError.setFont(new Font("Dialog", Font.PLAIN, 11));
        lblError.setVisible(false);
        c.gridy++; c.insets = new Insets(3, 0, 7, 0);
        formulario.add(lblError, c);

        JPanel nota = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        nota.setBackground(EstiloAntucayen.BACKGROUND);
        JLabel aviso = new JLabel("La sesión expira tras " +
                ControlInactividadSesion.minutosConfigurados() + " min de inactividad.");
        aviso.setForeground(EstiloAntucayen.MUTED);
        aviso.setFont(new Font("Dialog", Font.PLAIN, 10));
        nota.add(new JLabel(new IconoNavegacion("settings", EstiloAntucayen.MUTED, 15)));
        nota.add(aviso);
        c.gridy++; c.insets = new Insets(2, 0, 0, 0);
        formulario.add(nota, c);

        tarjeta.add(formulario);
        fondo.add(tarjeta);
        add(fondo, BorderLayout.CENTER);
        add(crearSelectorDeTemas(), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnIngresar);
    }

    private JLabel etiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Dialog", Font.BOLD, 12));
        lbl.setForeground(EstiloAntucayen.TEXT);
        return lbl;
    }

    private JPanel crearSelectorDeTemas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 9, 7));
        panel.setBackground(new Color(236, 243, 252));
        JLabel label = new JLabel("Tema visual:");
        label.setForeground(EstiloAntucayen.MUTED);
        panel.add(label);
        for (Tema opcion : Tema.values()) {
            JButton swatch = new JButton();
            swatch.setToolTipText(opcion.getNombre());
            swatch.setPreferredSize(new Dimension(23, 23));
            swatch.setBackground(opcion.colorPrimario);
            swatch.setBorder(BorderFactory.createLineBorder(
                    opcion == tema ? EstiloAntucayen.TEXT : EstiloAntucayen.BORDER,
                    opcion == tema ? 2 : 1));
            swatch.setFocusPainted(false);
            swatch.addActionListener(e -> {
                GestorTemas.getInstancia().setTema(opcion);
                if (onTemaCambiado != null) onTemaCambiado.run();
            });
            panel.add(swatch);
        }
        return panel;
    }

    private static final class IlustracionTienda extends JPanel {
        IlustracionTienda() { setBackground(new Color(232, 240, 252)); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D p = (Graphics2D)g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.scale(getWidth() / 420.0, getHeight() / 465.0);
            p.setPaint(new GradientPaint(0, 0, new Color(248, 251, 255), 0, 465, new Color(209, 226, 248)));
            p.fillRect(0, 0, 420, 465);
            p.setColor(new Color(207, 222, 240));
            p.fill(new Ellipse2D.Double(35, 85, 135, 42));
            p.fill(new Ellipse2D.Double(260, 60, 110, 38));
            p.setColor(new Color(194, 211, 233));
            p.fillRect(0, 385, 420, 80);
            p.setColor(new Color(176, 200, 229));
            p.fillRoundRect(63, 146, 295, 250, 8, 8);
            p.setColor(new Color(248, 251, 255));
            p.fillRoundRect(76, 135, 270, 241, 4, 4);
            p.setColor(new Color(29, 92, 186));
            p.fillRect(76, 169, 270, 8);
            p.setColor(new Color(219, 230, 247));
            p.fillRoundRect(123, 153, 181, 54, 6, 6);
            p.setColor(new Color(19, 82, 178));
            p.setFont(new Font("Dialog", Font.BOLD, 21));
            p.drawString("MINIMARKET", 145, 176);
            p.setFont(new Font("Dialog", Font.BOLD, 18));
            p.drawString("ANTUCAYEN", 145, 197);
            p.draw(new Line2D.Double(123, 207, 306, 207));
            // Toldo de lona azul y blanca.
            p.setColor(new Color(12, 73, 169));
            p.fill(new Polygon(new int[]{69, 353, 370, 55}, new int[]{214, 214, 258, 258}, 4));
            for (int i=0; i<6; i++) {
                p.setColor(i%2 == 0 ? new Color(248, 252, 255) : new Color(55, 122, 217));
                p.fill(new Polygon(new int[]{73+i*47, 118+i*47, 120+i*47, 70+i*47},
                        new int[]{217, 217, 252, 252}, 4));
            }
            p.setColor(new Color(22, 70, 140));
            p.fillRect(76, 258, 270, 7);
            // Vidrieras y acceso, sin recursos externos ni fuentes de iconos.
            p.setColor(new Color(42, 106, 185));
            p.fillRect(95, 273, 120, 103);
            p.fillRect(230, 273, 100, 103);
            p.setColor(new Color(173, 212, 242));
            p.fillRect(102, 280, 106, 89);
            p.fillRect(237, 280, 86, 89);
            p.setColor(new Color(42, 106, 185));
            p.setStroke(new BasicStroke(4f));
            p.drawLine(155, 280, 155, 367); p.drawLine(102, 320, 208, 320);
            p.drawLine(280, 280, 280, 369);
            p.setColor(new Color(244, 249, 255));
            for (int i=0;i<4;i++) {
                p.fillRoundRect(108 + (i%2)*50, 288 + (i/2)*41, 28, 28, 4, 4);
            }
            // Carrito de compra ilustrado a un lado.
            p.setColor(new Color(41, 107, 181));
            p.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            p.drawLine(31, 349, 44, 349); p.drawLine(44, 349, 52, 376);
            p.drawLine(52, 376, 79, 376); p.drawLine(49, 353, 84, 353); p.drawLine(84, 353, 80, 369);
            p.fillOval(54, 381, 7, 7); p.fillOval(76, 381, 7, 7);
            p.dispose();
        }
    }

    public String getUsername() { return txtUsername.getText().trim(); }
    public String getPassword() { return new String(txtPassword.getPassword()); }
    public JButton getBtnIngresar() { return btnIngresar; }
    public JButton getBtnConfigurarConexion() { return btnConfigurarConexion; }
    public void setOnTemaCambiado(Runnable onTemaCambiado) { this.onTemaCambiado = onTemaCambiado; }
    public void mostrarError(String msg) { lblError.setText(msg); lblError.setVisible(true); }
}
