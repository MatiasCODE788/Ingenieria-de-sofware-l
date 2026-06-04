package cl.antucayen.view;

import javax.swing.*;
import java.awt.*;

public class VLogin extends JFrame {

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JButton        btnIngresar;
    private JLabel         lblError;

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

        // Panel exterior con gradiente azul
        JPanel panelExterior = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(
                        0, 0,                   new Color(15, 30, 56),
                        getWidth(), getHeight(), new Color(26, 74, 110)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panelExterior.setOpaque(true);

        // Tarjeta blanca centrada
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));
        card.setPreferredSize(new Dimension(420, 480));

        // Sombra simulada con borde
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));

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
                g2.setColor(new Color(5, 150, 105));
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
        lblNombre.setForeground(new Color(15, 30, 56));
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
        btnIngresar.setBackground(new Color(15, 30, 56));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBorderPainted(false);
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIngresar.setPreferredSize(new Dimension(0, 44));
        btnIngresar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnIngresar.setBackground(new Color(30, 58, 95));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnIngresar.setBackground(new Color(15, 30, 56));
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

        panelExterior.add(card);
        add(panelExterior);
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
}