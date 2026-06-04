package cl.antucayen.util;

import cl.antucayen.model.entity.Usuario;

public class SesionActual {

    private static Usuario usuarioActual;

    public static void iniciar(Usuario usuario) { usuarioActual = usuario; }
    public static void cerrar()                 { usuarioActual = null; }
    public static Usuario getUsuario()          { return usuarioActual; }
    public static boolean haySesion()           { return usuarioActual != null; }

    public static String getPerfil() {
        return usuarioActual != null ? usuarioActual.getNombrePerfil() : "";
    }

    public static boolean esAdministrador() {
        return "Administrador".equalsIgnoreCase(getPerfil());
    }

    public static boolean esBodeguero() {
        return "Bodeguero".equalsIgnoreCase(getPerfil());
    }

    public static boolean esConsulta() {
        return "Consulta".equalsIgnoreCase(getPerfil());
    }
}