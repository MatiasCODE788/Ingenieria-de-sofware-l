package cl.antucayen;

import cl.antucayen.controller.ControladorLogin;
import cl.antucayen.view.VLogin;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {

        // Escalado para pantallas HiDPI (universal)
        System.setProperty("sun.java2d.uiScale", "2.0");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Fuente global más grande
        configurarFuenteGlobal(30);

        SwingUtilities.invokeLater(() -> {
            VLogin login = new VLogin();
            new ControladorLogin(login);
            login.setVisible(true);
        });
    }

    private static void configurarFuenteGlobal(int tamanio) {
        Font fuente = new Font("Arial", Font.PLAIN, tamanio);
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font) {
                UIManager.put(key, fuente.deriveFont(((Font) value).getStyle(),
                        tamanio));
            }
        }
    }
}