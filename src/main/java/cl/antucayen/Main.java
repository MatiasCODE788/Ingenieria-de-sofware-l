package cl.antucayen;

import cl.antucayen.view.VLogin;

public class Main {
    public static void main(String[] args) {
        // Ejecutar la interfaz en el hilo de Swing
        javax.swing.SwingUtilities.invokeLater(() -> {
            VLogin login = new VLogin();
            login.setVisible(true);
        });
    }
}