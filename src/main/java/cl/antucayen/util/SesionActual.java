package cl.antucayen.util;

import cl.antucayen.model.entity.Usuario;
import cl.antucayen.security.RolSistema;

public final class SesionActual {

    private static Usuario usuarioActual;

    private SesionActual() {}

    public static void iniciar(Usuario usuario) { usuarioActual = usuario; }
    public static void cerrar()                 { usuarioActual = null; }
    public static Usuario getUsuario()          { return usuarioActual; }
    public static boolean haySesion()           { return usuarioActual != null; }

    public static String getPerfil() {
        return usuarioActual != null ? usuarioActual.getNombrePerfil() : "";
    }

    public static RolSistema getRol() {
        return RolSistema.desdeNombre(getPerfil());
    }

    public static boolean esAdministrador() {
        return RolSistema.ADMINISTRADOR.coincide(getPerfil());
    }

    public static boolean esBodeguero() {
        return RolSistema.BODEGUERO.coincide(getPerfil());
    }

    public static boolean esCajero() {
        return RolSistema.CAJERO.coincide(getPerfil());
    }

    public static boolean esRolSoportado() {
        return getRol() != null;
    }
}
