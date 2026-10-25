package cl.antucayen.view;

import cl.antucayen.util.DBConexion;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Properties;

/** Formulario local de conexión LAN, accesible antes de iniciar sesión. */
public final class DialogoConexion {
    private DialogoConexion() { }

    public static void mostrar(Window padre) {
        Properties actuales;
        try {
            actuales = DBConexion.obtenerConfiguracionEditable();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(padre, ex.getMessage(), "Configuración", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField host = new JTextField(actuales.getProperty("db.host"), 22);
        JTextField puerto = new JTextField(actuales.getProperty("db.port"), 22);
        JTextField base = new JTextField(actuales.getProperty("db.nombre"), 22);
        JTextField usuario = new JTextField(actuales.getProperty("db.usuario"), 22);
        JPasswordField clave = new JPasswordField(actuales.getProperty("db.contrasena"), 22);
        JCheckBox ssl = new JCheckBox("Usar TLS/SSL (solo si MariaDB está configurado)",
                Boolean.parseBoolean(actuales.getProperty("db.ssl", "false")));

        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        campos.add(new JLabel("IP del PC administrador:"), c);
        c.gridx = 1; campos.add(host, c);
        c.gridx = 0; c.gridy++; campos.add(new JLabel("Puerto MariaDB:"), c);
        c.gridx = 1; campos.add(puerto, c);
        c.gridx = 0; c.gridy++; campos.add(new JLabel("Base de datos:"), c);
        c.gridx = 1; campos.add(base, c);
        c.gridx = 0; c.gridy++; campos.add(new JLabel("Usuario MariaDB:"), c);
        c.gridx = 1; campos.add(usuario, c);
        c.gridx = 0; c.gridy++; campos.add(new JLabel("Contraseña MariaDB:"), c);
        c.gridx = 1; campos.add(clave, c);
        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        campos.add(ssl, c);

        JPanel contenido = new JPanel(new BorderLayout(8, 9));
        contenido.add(new JLabel("<html>Todos los equipos apuntan a <b>una única IP del servidor</b>. "
                + "Ejemplo: 192.168.100.10.<br/>"
                + "No ingreses la IP de cada cajero. En el PC servidor también se puede usar localhost.</html>"),
                BorderLayout.NORTH);
        contenido.add(campos, BorderLayout.CENTER);
        contenido.add(new JLabel("<html><small>Se guarda solo en este equipo. Requiere acceso "
                + "autorizado a MariaDB; no modifica la base de datos.</small></html>"), BorderLayout.SOUTH);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(padre, contenido,
                    "Configurar conexión a MariaDB (red 192.168.100.x)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return;

            Properties nuevos = new Properties();
            nuevos.setProperty("db.host", host.getText());
            nuevos.setProperty("db.port", puerto.getText());
            nuevos.setProperty("db.nombre", base.getText());
            nuevos.setProperty("db.usuario", usuario.getText());
            nuevos.setProperty("db.contrasena", new String(clave.getPassword()));
            nuevos.setProperty("db.ssl", Boolean.toString(ssl.isSelected()));
            try {
                DBConexion.guardarConfiguracion(nuevos);
                JOptionPane.showMessageDialog(padre,
                        "Configuración guardada en:\n" + DBConexion.rutaConfiguracionUsuario()
                        + "\n\nYa puedes intentar iniciar sesión.",
                        "Conexión configurada", JOptionPane.INFORMATION_MESSAGE);
                return;
            } catch (IllegalArgumentException | IOException ex) {
                JOptionPane.showMessageDialog(padre, ex.getMessage(),
                        "Revisar configuración", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
