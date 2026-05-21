package cl.antucayen;

import cl.antucayen.view.VBuscadorProductos;
import cl.antucayen.view.VPrincipal;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            VPrincipal principal = new VPrincipal("Felipe Salas", "Administrador");
            VBuscadorProductos buscador = new VBuscadorProductos();
            principal.setContenido(buscador, "Productos");
            principal.setVisible(true);
        });
    }
}