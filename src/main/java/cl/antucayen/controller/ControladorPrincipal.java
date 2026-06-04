package cl.antucayen.controller;

import cl.antucayen.util.SesionActual;
import cl.antucayen.view.*;

public class ControladorPrincipal {

    private final VPrincipal vista;

    public ControladorPrincipal(VPrincipal vista) {
        this.vista = vista;
        iniciarEventos();
        // Mostrar dashboard al inicio
        mostrarDashboard();
    }

    private void iniciarEventos() {
        vista.getBtnDashboard().addActionListener(e -> mostrarDashboard());

        vista.getBtnProductos().addActionListener(e -> {
            VBuscadorProductos panel = new VBuscadorProductos();
            new ControladorProducto(panel);
            vista.setContenido(panel, "Productos");
        });

        vista.getBtnProveedores().addActionListener(e -> {
            VBuscadorProveedores panel = new VBuscadorProveedores();
            new ControladorProveedor(panel);
            vista.setContenido(panel, "Proveedores");
        });

        vista.getBtnFacturas().addActionListener(e -> {
            VFacturas panel = new VFacturas();
            new ControladorFactura(panel);
            vista.setContenido(panel, "Facturas");
        });

        vista.getBtnProcesarFactura().addActionListener(e -> {
            VFacturas panel = new VFacturas();
            new ControladorFactura(panel);
            vista.setContenido(panel, "Procesar Factura");
        });

        vista.getBtnImportarInventario().addActionListener(e -> {
            VAjusteInventario panel = new VAjusteInventario();
            new ControladorAjusteInventario(panel);
            vista.setContenido(panel, "Importar Inventario");
        });

        vista.getBtnHistorial().addActionListener(e -> {
            VHistorial panel = new VHistorial();
            new ControladorHistorial(panel);
            vista.setContenido(panel, "Historial de Movimientos");
        });

        vista.getBtnUsuarios().addActionListener(e -> {
            VGestionUsuarios panel = new VGestionUsuarios();
            new ControladorUsuario(panel);
            vista.setContenido(panel, "Usuarios y Permisos");
        });

        vista.getBtnCerrarSesion().addActionListener(e -> cerrarSesion());
    }

    private void mostrarDashboard() {
        VDashboard panel = new VDashboard();
        new ControladorDashboard(panel);
        vista.setContenido(panel, "Dashboard");
    }

    private void cerrarSesion() {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(
                vista, "¿Estás seguro que deseas cerrar sesión?",
                "Cerrar sesión", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            SesionActual.cerrar();
            vista.dispose();
            VLogin login = new VLogin();
            new ControladorLogin(login);
            login.setVisible(true);
        }
    }
}