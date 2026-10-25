package cl.antucayen.controller;

import cl.antucayen.security.Autorizacion;
import cl.antucayen.security.ControlInactividadSesion;
import cl.antucayen.security.SesionActual;
import cl.antucayen.view.*;

import javax.swing.JOptionPane;

public class ControladorPrincipal {

    private final VPrincipal vista;
    private final ControlInactividadSesion controlInactividad;

    // Se cachea para que el carrito y los pagos en curso no se pierdan al cambiar de módulo y volver.
    private VVentas panelVentas;
    private ControladorVenta controladorVenta;

    public ControladorPrincipal(VPrincipal vista) {
        this.vista = vista;
        controlInactividad = new ControlInactividadSesion(vista, this::expirarSesion);
        vista.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                controlInactividad.detener();
            }
        });
        iniciarEventos();
        if (SesionActual.esAdministrador()) {
            mostrarDashboard();
        } else if (SesionActual.esBodeguero()) {
            mostrarProductos();
        } else if (SesionActual.esCajero()) {
            mostrarVentas();
        } else {
            throw new SecurityException("Perfil de usuario no autorizado");
        }
        controlInactividad.iniciar();
    }

    private void iniciarEventos() {
        vista.getBtnDashboard().addActionListener(e -> ejecutarSeguro(this::mostrarDashboard));
        vista.getBtnVentas().addActionListener(e -> ejecutarSeguro(this::mostrarVentas));

        vista.getBtnProductos().addActionListener(e -> ejecutarSeguro(this::mostrarProductos));

        vista.getBtnProveedores().addActionListener(e -> ejecutarSeguro(() -> {
            Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
            VBuscadorProveedores panel = new VBuscadorProveedores();
            new ControladorProveedor(panel);
            vista.setContenido(panel, "Proveedores");
        }));

        vista.getBtnEquivalencias().addActionListener(e -> ejecutarSeguro(() -> {
            Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
            VConsultaEquivalencias panel = new VConsultaEquivalencias();
            new ControladorProveedor(panel);
            vista.setContenido(panel, "Consulta de Equivalencias");
        }));

        vista.getBtnFacturas().addActionListener(e -> ejecutarSeguro(this::mostrarFacturas));

        vista.getBtnProcesarFactura().addActionListener(e -> ejecutarSeguro(this::abrirProcesarFactura));

        vista.getBtnImportarInventario().addActionListener(e -> ejecutarSeguro(() -> {
            Autorizacion.verificarAjustesInventario();
            VAjusteInventario panel = new VAjusteInventario();
            new ControladorAjusteInventario(panel);
            vista.setContenido(panel, "Importar Inventario");
        }));

        vista.getBtnHistorial().addActionListener(e -> ejecutarSeguro(() -> {
            Autorizacion.verificarHistorialInventario();
            VHistorial panel = new VHistorial();
            new ControladorHistorial(panel);
            vista.setContenido(panel, "Historial de Movimientos");
        }));

        vista.getBtnReportes().addActionListener(e -> ejecutarSeguro(this::mostrarReportes));

        vista.getBtnUsuarios().addActionListener(e -> ejecutarSeguro(() -> {
            Autorizacion.verificarAdministrador("Solo el Administrador puede gestionar usuarios");
            VGestionUsuarios panel = new VGestionUsuarios();
            new ControladorUsuario(panel);
            vista.setContenido(panel, "Usuarios y Permisos");
        }));

        vista.getBtnAuditoriaArchivos().addActionListener(e -> ejecutarSeguro(() -> {
            Autorizacion.verificarAdministrador(
                    "Solo el Administrador puede consultar la bitácora de archivos");
            VAuditoriaArchivos panel = new VAuditoriaArchivos();
            new ControladorAuditoriaArchivos(panel);
            vista.setContenido(panel, "Auditoría de archivos");
        }));

        vista.getBtnCerrarSesion().addActionListener(e -> cerrarSesion());
    }


    private void mostrarReportes() {
        Autorizacion.verificarReportes();
        VReportes panel = new VReportes();
        new ControladorReportes(panel, () -> ejecutarSeguro(() -> {
            Autorizacion.verificarGestionEquivalencias();
            VConsultaEquivalencias equivalencias = new VConsultaEquivalencias();
            new ControladorProveedor(equivalencias);
            vista.setContenido(equivalencias, "Consulta de Equivalencias");
        }));
        vista.setContenido(panel, "Reportes");
    }

    private void abrirProcesarFactura() {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        new ControladorFactura().abrirNuevaFactura(() -> ejecutarSeguro(this::mostrarFacturas));
    }

    private void mostrarFacturas() {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        VFacturas panel = new VFacturas();
        new ControladorFactura(panel);
        vista.setContenido(panel, "Facturas");
    }

    private void mostrarDashboard() {
        Autorizacion.verificarAdministrador("Solo el Administrador puede acceder al Dashboard global");
        VDashboard panel = new VDashboard();
        new ControladorDashboard(panel);
        vista.setContenido(panel, "Dashboard");
    }

    private void mostrarProductos() {
        Autorizacion.verificarConsultaStock();
        VBuscadorProductos panel = new VBuscadorProductos();
        new ControladorProducto(panel);
        vista.setContenido(panel, "Productos / Stock");
    }

    /**
     * Muestra el Punto de Venta reutilizando siempre el mismo panel/controlador.
     * Sólo Administrador y Cajero pueden operar caja.
     */
    private void mostrarVentas() {
        Autorizacion.verificarPuntoVenta();
        if (panelVentas == null) {
            panelVentas = new VVentas();
            controladorVenta = new ControladorVenta(panelVentas);
        } else {
            controladorVenta.alMostrar();
        }
        vista.setContenido(panelVentas, "Punto de Venta");
    }

    private void ejecutarSeguro(Runnable accion) {
        try {
            accion.run();
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage(), "Acceso denegado", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void expirarSesion() {
        volverAlLogin(true);
    }

    private void volverAlLogin(boolean porInactividad) {
        controlInactividad.detener();
        SesionActual.cerrar();
        // Los diálogos modales o auxiliares también pueden contener datos
        // de la sesión anterior: cerrarlos antes de reabrir el acceso.
        cerrarVentanasDeSesion();
        vista.dispose();
        VLogin login = new VLogin();
        new ControladorLogin(login);
        login.setVisible(true);
        if (porInactividad) {
            JOptionPane.showMessageDialog(login,
                    "La sesión se cerró después de "
                            + ControlInactividadSesion.minutosConfigurados()
                            + " minutos de inactividad. Inicia sesión nuevamente.",
                    "Sesión expirada", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void cerrarVentanasDeSesion() {
        // También cierra ventanas sin owner (p. ej. algunos diálogos de factura).
        // El nuevo login se crea DESPUÉS de liberar todas las vistas anteriores.
        for (java.awt.Window ventana : java.awt.Window.getWindows()) {
            if (ventana != vista && ventana.isDisplayable()) ventana.dispose();
        }
    }

    private void cerrarSesion() {
        int confirm = JOptionPane.showConfirmDialog(
                vista, "¿Estás seguro que deseas cerrar sesión?",
                "Cerrar sesión", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            volverAlLogin(false);
        }
    }
}
