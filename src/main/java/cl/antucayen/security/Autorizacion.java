package cl.antucayen.security;

import cl.antucayen.util.SesionActual;

/**
 * Frontera RBAC central de Antucayen.
 *
 * <p>Los únicos roles soportados son Administrador, Bodeguero y Cajero. La
 * autorización se aplica nuevamente en servicios para no depender de la
 * visibilidad de botones o menús Swing.</p>
 */
public final class Autorizacion {

    public static final String ACCESO_DENEGADO =
            "Acceso denegado: su perfil no tiene permisos para esta función";

    private Autorizacion() {}

    public static void verificarSesionActiva() {
        if (!SesionActual.haySesion()) {
            throw new SecurityException("No existe una sesión de usuario activa");
        }
        if (!SesionActual.esRolSoportado()) {
            throw new SecurityException(ACCESO_DENEGADO);
        }
    }

    /** Alta, edición e inactivación de productos. */
    public static void verificarGestionProductos() {
        verificarAdministradorOBodeguero(ACCESO_DENEGADO);
    }

    /** Alta y edición de proveedores. */
    public static void verificarGestionProveedores() {
        verificarAdministradorOBodeguero(ACCESO_DENEGADO);
    }

    /** Registro, consulta y procesamiento ordinario de facturas de compra. */
    public static void verificarGestionFacturas() {
        verificarAdministradorOBodeguero(ACCESO_DENEGADO);
    }

    /** Registro inicial y consulta de equivalencias operativas. */
    public static void verificarGestionEquivalencias() {
        verificarAdministradorOBodeguero(ACCESO_DENEGADO);
    }

    /**
     * RF-31 a RF-45: Administrador y Bodeguero pueden importar, previsualizar
     * y aplicar ajustes ordinarios de inventario.
     */
    public static void verificarAjustesInventario() {
        verificarAdministradorOBodeguero(ACCESO_DENEGADO);
    }

    /** Correcciones que aceptan cantidades negativas: exclusivamente Admin. */
    public static void verificarCorreccionNegativaInventario() {
        verificarAdministrador(
                "Solo el Administrador puede autorizar correcciones con cantidades negativas");
    }

    /** RF-46: la reversión de un ajuste aplicado es exclusivamente Admin. */
    public static void verificarReversionAjuste() {
        verificarAdministrador("Solo el Administrador puede revertir ajustes de inventario");
    }

    /**
     * RF-47/RF-60: el historial/reportes de inventario son de consulta para
     * cualquiera de los tres roles autorizados. Las acciones administrativas
     * dentro de la pantalla siguen verificándose por separado.
     */
    public static void verificarHistorialInventario() {
        verificarSesionActiva();
    }

    /** Reportes operacionales disponibles para los tres roles vigentes. */
    public static void verificarReportes() {
        verificarSesionActiva();
    }

    /** Punto de Venta: Cajero y Administrador supervisor. */
    public static void verificarPuntoVenta() {
        verificarSesionActiva();
        if (!SesionActual.esAdministrador() && !SesionActual.esCajero()) {
            throw new SecurityException(ACCESO_DENEGADO);
        }
    }

    /** Consulta de productos y stock en tiempo real para los tres roles. */
    public static void verificarConsultaStock() {
        verificarSesionActiva();
    }

    public static void verificarAdministradorOBodeguero(String mensaje) {
        verificarSesionActiva();
        if (!SesionActual.esAdministrador() && !SesionActual.esBodeguero()) {
            throw new SecurityException(mensaje);
        }
    }

    public static void verificarAdministrador(String mensaje) {
        verificarSesionActiva();
        if (!SesionActual.esAdministrador()) {
            throw new SecurityException(mensaje);
        }
    }
}
