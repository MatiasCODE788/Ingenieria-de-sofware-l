package cl.antucayen;

import cl.antucayen.controller.ControladorLogin;
import cl.antucayen.view.VLogin;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Mantiene el antialiasing sin imponer una fuente/tamaño global fijo.
        // Cada vista conserva sus tamaños propios y el escalado HiDPI del SO.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            VLogin login = new VLogin();
            new ControladorLogin(login);
            login.setVisible(true);
        });
    }
}
