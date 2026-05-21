package cl.antucayen;

import cl.antucayen.view.VFormularioProducto;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            VFormularioProducto formulario = new VFormularioProducto(null, false);
            formulario.setVisible(true);
        });
    }
}