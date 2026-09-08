package cl.antucayen.controller;

import cl.antucayen.util.SesionActual;
import cl.antucayen.view.*;

public class ControladorPrincipal {

    private final VPrincipal vista;

    // Se cachea para que el carrito y los pagos en curso no se pierdan al cambiar de módulo y volver.
    private VVentas         panelVentas;
    private ControladorVenta controladorVenta;

    public ControladorPrincipal(VPrincipal vista) {
        this.vista = vista;
        iniciarEventos();
        // Mostrar dashboard al inicio
        mostrarDashboard();
    }

    private void iniciarEventos() {
        vista.getBtnDashboard().addActionListener(e -> mostrarDashboard());

        vista.getBtnVentas().addActionListener(e -> mostrarVentas());

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

        vista.getBtnEquivalencias().addActionListener(e -> {
            VConsultaEquivalencias panel = new VConsultaEquivalencias();
            new ControladorProveedor(panel);
            vista.setContenido(panel, "Consulta de Equivalencias");
        });

        vista.getBtnFacturas().addActionListener(e -> {
            VFacturas panel = new VFacturas();
            new ControladorFactura(panel);
            vista.setContenido(panel, "Facturas");
        });

        vista.getBtnProcesarFactura().addActionListener(e -> abrirProcesarFactura());

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

    /** Abre directamente el formulario de "Procesar Factura" (ingreso manual). */
    private void abrirProcesarFactura() {
        new ControladorFactura().abrirNuevaFactura();
    }

    private void mostrarDashboard() {
        VDashboard panel = new VDashboard();
        new ControladorDashboard(panel);
        vista.setContenido(panel, "Dashboard");
    }

    /**
     * Muestra el Punto de Venta reutilizando siempre el mismo panel/controlador:
     * así el carrito y los montos de pago ingresados no se pierden si el
     * usuario navega a otro módulo (ej. a consultar Productos) y vuelve.
     */
    private void mostrarVentas() {
        if (panelVentas == null) {
            panelVentas = new VVentas();
            controladorVenta = new ControladorVenta(panelVentas);
        } else {
            controladorVenta.alMostrar();
        }
        vista.setContenido(panelVentas, "Punto de Venta");
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