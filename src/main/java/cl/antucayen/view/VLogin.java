package cl.antucayen.view;

import javax.swing.*;
import java.awt.*;

public class VLogin extends JFrame {

    // Componentes de la pantalla
    private JTextField txtUsuario;
    private JPasswordField txtContrasena;
    private JButton btnIngresar;
    private JLabel lblError;

    public VLogin() {
        initComponents();
    }

    private void initComponents() {
        // Configuración de la ventana
        setTitle("Minimarket Antucayen — Iniciar Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null); // centrar en pantalla
        setResizable(false);

        // Panel principal
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 48, 84));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Título
        JLabel lblTitulo = new JLabel("Minimarket Antucayen");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitulo, gbc);

        // Subtítulo
        JLabel lblSub = new JLabel("Sistema de Gestión de Inventario");
        lblSub.setFont(new Font("Arial", Font.PLAIN, 12));
        lblSub.setForeground(new Color(180, 200, 230));
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        panel.add(lblSub, gbc);

        // Separador
        gbc.gridy = 2;
        panel.add(Box.createVerticalStrut(10), gbc);

        // Label usuario
        JLabel lblUsuario = new JLabel("Usuario");
        lblUsuario.setFont(new Font("Arial", Font.BOLD, 12));
        lblUsuario.setForeground(new Color(180, 200, 230));
        gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(lblUsuario, gbc);

        // Campo usuario
        txtUsuario = new JTextField();
        txtUsuario.setFont(new Font("Arial", Font.PLAIN, 13));
        txtUsuario.setPreferredSize(new Dimension(300, 35));
        gbc.gridy = 4;
        panel.add(txtUsuario, gbc);

        // Label contraseña
        JLabel lblContrasena = new JLabel("Contraseña");
        lblContrasena.setFont(new Font("Arial", Font.BOLD, 12));
        lblContrasena.setForeground(new Color(180, 200, 230));
        gbc.gridy = 5;
        panel.add(lblContrasena, gbc);

        // Campo contraseña
        txtContrasena = new JPasswordField();
        txtContrasena.setFont(new Font("Arial", Font.PLAIN, 13));
        txtContrasena.setPreferredSize(new Dimension(300, 35));
        gbc.gridy = 6;
        panel.add(txtContrasena, gbc);

        // Botón ingresar
        btnIngresar = new JButton("Iniciar sesión");
        btnIngresar.setFont(new Font("Arial", Font.BOLD, 13));
        btnIngresar.setBackground(new Color(5, 150, 105));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setPreferredSize(new Dimension(300, 38));
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIngresar.setFocusPainted(false);
        gbc.gridy = 7;
        gbc.insets = new Insets(14, 0, 6, 0);
        panel.add(btnIngresar, gbc);

        // Label error (oculto por defecto)
        lblError = new JLabel("");
        lblError.setFont(new Font("Arial", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 80, 80));
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        lblError.setVisible(false);
        gbc.gridy = 8;
        gbc.insets = new Insets(2, 0, 0, 0);
        panel.add(lblError, gbc);

        add(panel);
    }

    // Métodos para que el Controlador acceda a los datos
    public String getUsuario() {
        return txtUsuario.getText().trim();
    }

    public String getContrasena() {
        return new String(txtContrasena.getPassword());
    }

    public void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
    }

    public void limpiarError() {
        lblError.setText("");
        lblError.setVisible(false);
    }

    public void limpiarCampos() {
        txtUsuario.setText("");
        txtContrasena.setText("");
        limpiarError();
    }

    public JButton getBtnIngresar() {
        return btnIngresar;
    }
}