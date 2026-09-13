package cl.antucayen.security;

import cl.antucayen.util.SesionActual;

/**
 * Frontera RBAC central de Antucayen.
 *
 * <p>Los tres únicos roles soportados son Administrador, Bodeguero y Cajero.
 * Cada servicio debe usar el permiso de negocio específico y no inferir permisos
 * únicamente desde la visibilidad de botones en Swing.</p>
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

    /** Importaciones y ajustes directos de inventario: función administrativa. */
    public static void verificarAjustesInventario() {
        verificarAdministrador("Solo el Administrador puede ejecutar ajustes directos de inventario");
    }

    /** Consulta del historial operacional de inventario. */
    public static void verificarHistorialInventario() {
        verificarAdministradorOBodeguero(ACCESO_DENEGADO);
    }

    /** Punto de Venta: Cajero y Administrador supervisor. */
    public static void verificarPuntoVenta() {
        verificarSesionActiva();
        if (!SesionActual.esAdministrador() && !SesionActual.esCajero()) {
            throw new SecurityException(ACCESO_DENEGADO);
        }
    }

    /** Consulta de productos y stock en tiempo real para los tres roles vigentes. */
    public static void verificarConsultaStock() {
        verificarSesionActiva();
        if (!SesionActual.esAdministrador()
                && !SesionActual.esBodeguero()
                && !SesionActual.esCajero()) {
            throw new SecurityException(ACCESO_DENEGADO);
        }
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
