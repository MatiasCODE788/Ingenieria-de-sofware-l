package cl.antucayen.security;

import cl.antucayen.model.entity.Usuario;
import cl.antucayen.model.service.ServicioDashboard;
import cl.antucayen.model.service.ServicioProducto;
import cl.antucayen.model.service.ServicioVenta;
import cl.antucayen.util.SesionActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutorizacionRBACTest {

    @AfterEach
    void limpiarSesion() {
        SesionActual.cerrar();
    }

    @Test
    void cajeroPuedeVenderYConsultarStockPeroNoAdministrar() {
        iniciarComo("Cajero");

        assertDoesNotThrow(Autorizacion::verificarPuntoVenta);
        assertDoesNotThrow(Autorizacion::verificarConsultaStock);
        assertThrows(SecurityException.class, Autorizacion::verificarGestionProductos);
        assertThrows(SecurityException.class, Autorizacion::verificarGestionProveedores);
        assertThrows(SecurityException.class, Autorizacion::verificarGestionFacturas);
        assertThrows(SecurityException.class, Autorizacion::verificarAjustesInventario);
        assertThrows(SecurityException.class, Autorizacion::verificarHistorialInventario);
        assertThrows(SecurityException.class,
                () -> Autorizacion.verificarAdministrador("Solo Administrador"));
        assertThrows(SecurityException.class,
                () -> new ServicioProducto().listarProveedores("SKU-DE-PRUEBA"));
        assertThrows(SecurityException.class,
                () -> new ServicioVenta().totalMesActual());
    }

    @Test
    void bodegueroGestionaProductosProveedoresYFacturasPeroNoCajaNiAjustesDirectos() {
        iniciarComo("Bodeguero");

        assertDoesNotThrow(Autorizacion::verificarGestionProductos);
        assertDoesNotThrow(Autorizacion::verificarGestionProveedores);
        assertDoesNotThrow(Autorizacion::verificarGestionFacturas);
        assertDoesNotThrow(Autorizacion::verificarConsultaStock);
        assertThrows(SecurityException.class, Autorizacion::verificarPuntoVenta);
        assertThrows(SecurityException.class, Autorizacion::verificarAjustesInventario);
    }

    @Test
    void bodegueroNoPuedeAccederAlDashboardGerencial() {
        iniciarComo("Bodeguero");
        assertThrows(SecurityException.class,
                () -> new ServicioDashboard().productosActivos());
    }

    @Test
    void administradorPuedeSupervisarCajaYAdministrar() {
        iniciarComo("Administrador");

        assertDoesNotThrow(Autorizacion::verificarPuntoVenta);
        assertDoesNotThrow(Autorizacion::verificarGestionProductos);
        assertDoesNotThrow(Autorizacion::verificarAjustesInventario);
        assertDoesNotThrow(() -> Autorizacion.verificarAdministrador("Solo Administrador"));
    }

    @Test
    void perfilNoReconocidoQuedaBloqueado() {
        iniciarComo("PerfilNoAutorizado");

        assertFalse(SesionActual.esRolSoportado());
        assertThrows(SecurityException.class, Autorizacion::verificarConsultaStock);
        assertThrows(SecurityException.class, Autorizacion::verificarPuntoVenta);
    }

    private void iniciarComo(String perfil) {
        SesionActual.iniciar(new Usuario(1, "qa", true, 1, perfil));
    }
}
