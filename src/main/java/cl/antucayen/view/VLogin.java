package cl.antucayen.view;

import cl.antucayen.util.GestorTemas;
import cl.antucayen.util.Tema;

import javax.swing.*;
import java.awt.*;

public class VLogin extends JFrame {

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JButton        btnIngresar;
    private JLabel         lblError;

    private Tema tema = GestorTemas.getInstancia().getTema();

    public VLogin() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Minimarket Antucayen — Iniciar Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(600, 500));
        setLocationRelativeTo(null);
        setResizable(true);

        // Panel exterior con gradiente según el tema
        JPanel panelExterior = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(
                        0, 0,                   tema.gradienteInicio,
                        getWidth(), getHeight(), tema.gradienteFin
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panelExterior.setOpaque(true);
        panelExterior.setLayout(new BorderLayout());

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);

        // Tarjeta blanca centrada
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));
        card.setPreferredSize(new Dimension(420, 480));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.weightx   = 1.0;
        gbc.gridwidth = 1;
        gbc.insets    = new Insets(5, 0, 5, 0);

        // Logo + nombre
        JPanel panelLogo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelLogo.setBackground(Color.WHITE);

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tema.colorAcento);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.PLAIN, 18));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("🏪", (getWidth() - fm.stringWidth("🏪")) / 2,
                        (getHeight() + fm.getAscent()) / 2 - 2);
            }
        };
        iconBox.setPreferredSize(new Dimension(40, 40));
        iconBox.setOpaque(false);

        JPanel panelNombreLogo = new JPanel();
        panelNombreLogo.setBackground(Color.WHITE);
        panelNombreLogo.setLayout(new BoxLayout(panelNombreLogo, BoxLayout.Y_AXIS));
        JLabel lblNombre = new JLabel("Antucayen");
        lblNombre.setFont(new Font("Arial", Font.BOLD, 18));
        lblNombre.setForeground(tema.colorPrimario);
        JLabel lblSistema = new JLabel("Sistema de gestión");
        lblSistema.setFont(new Font("Arial", Font.PLAIN, 11));
        lblSistema.setForeground(new Color(107, 114, 128));
        panelNombreLogo.add(lblNombre);
        panelNombreLogo.add(lblSistema);

        panelLogo.add(iconBox);
        panelLogo.add(Box.createHorizontalStrut(10));
        panelLogo.add(panelNombreLogo);

        gbc.gridx = 0; gbc.gridy = 0;
        card.add(panelLogo, gbc);

        // Subtítulo
        JLabel lblSubtitulo = new JLabel("Gestión de inventario y facturas del minimarket");
        lblSubtitulo.setFont(new Font("Arial", Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(107, 114, 128));
        gbc.gridy = 1;
        gbc.insets = new Insets(2, 0, 16, 0);
        card.add(lblSubtitulo, gbc);

        // Label USUARIO
        JLabel lblUser = new JLabel("USUARIO");
        lblUser.setFont(new Font("Arial", Font.BOLD, 11));
        lblUser.setForeground(new Color(107, 114, 128));
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 0, 3, 0);
        card.add(lblUser, gbc);

        // Campo usuario
        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 14));
        txtUsername.setForeground(new Color(30, 41, 59));
        txtUsername.setBackground(new Color(248, 250, 252));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        txtUsername.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(txtUsername, gbc);

        // Label CONTRASEÑA
        JLabel lblPass = new JLabel("CONTRASEÑA");
        lblPass.setFont(new Font("Arial", Font.BOLD, 11));
        lblPass.setForeground(new Color(107, 114, 128));
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 0, 3, 0);
        card.add(lblPass, gbc);

        // Campo contraseña
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPassword.setForeground(new Color(30, 41, 59));
        txtPassword.setBackground(new Color(248, 250, 252));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        txtPassword.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(txtPassword, gbc);

        // Botón ingresar
        btnIngresar = new JButton("Ingresar al sistema");
        btnIngresar.setFont(new Font("Arial", Font.BOLD, 14));
        btnIngresar.setBackground(tema.colorPrimario);
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIngresar.setPreferredSize(new Dimension(0, 44));
        btnIngresar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnIngresar.setBackground(tema.colorPrimarioHover);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnIngresar.setBackground(tema.colorPrimario);
            }
        });
        gbc.gridy = 6;
        gbc.insets = new Insets(16, 0, 6, 0);
        card.add(btnIngresar, gbc);

        // Label error
        lblError = new JLabel("", SwingConstants.LEFT);
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(185, 28, 28));
        lblError.setVisible(false);
        gbc.gridy = 7;
        gbc.insets = new Insets(4, 0, 0, 0);
        card.add(lblError, gbc);

        // Nota sesión
        JLabel lblNota = new JLabel("La sesión expira tras 30 min de inactividad.");
        lblNota.setFont(new Font("Arial", Font.PLAIN, 11));
        lblNota.setForeground(new Color(148, 163, 184));
        gbc.gridy = 8;
        gbc.insets = new Insets(8, 0, 0, 0);
        card.add(lblNota, gbc);

        centro.add(card);
        panelExterior.add(centro, BorderLayout.CENTER);
        panelExterior.add(crearSelectorDeTemas(), BorderLayout.SOUTH);

        add(panelExterior);

        // Enter activa el botón de ingresar, sin importar en qué campo esté el foco
        getRootPane().setDefaultButton(btnIngresar);
    }

    /** Franja inferior con 5 círculos de color para elegir el tema del sistema. */
    private JPanel crearSelectorDeTemas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 14));
        panel.setOpaque(false);

        JLabel lbl = new JLabel("Tema:  ");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        panel.add(lbl);

        for (Tema opcion : Tema.values()) {
            JButton swatch = new JButton();
            swatch.setToolTipText(opcion.getNombre());
            swatch.setPreferredSize(new Dimension(24, 24));
            swatch.setBackground(opcion.colorPrimario);
            swatch.setBorder(BorderFactory.createLineBorder(
                    opcion == tema ? Color.WHITE : new Color(255, 255, 255, 80),
                    opcion == tema ? 3 : 1));
            swatch.setFocusPainted(false);
            swatch.setCursor(new Cursor(Cursor.HAND_CURSOR));
            swatch.addActionListener(e -> {
                GestorTemas.getInstancia().setTema(opcion);
                dispose();
                new VLogin().setVisible(true); // recarga la pantalla con el nuevo tema aplicado
            });
            panel.add(swatch);
        }
        return panel;
    }

    public String  getUsername()    { return txtUsername.getText().trim(); }
    public String  getPassword()    { return new String(txtPassword.getPassword()); }
    public JButton getBtnIngresar() { return btnIngresar; }

    public void mostrarError(String msg) {
        lblError.setText("⚠ " + msg);
        lblError.setVisible(true);
    }

    public void limpiarCampos() {
        txtUsername.setText("");
        txtPassword.setText("");
        lblError.setVisible(false);
    }

    /** Solo borra la contraseña (por seguridad) y le devuelve el foco, sin tocar el usuario ni ocultar el error. */
    public void limpiarSoloContrasena() {
        txtPassword.setText("");
        txtPassword.requestFocusInWindow();
    }
}