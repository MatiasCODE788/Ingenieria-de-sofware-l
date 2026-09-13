package cl.antucayen.security;

/**
 * Catálogo único de roles soportados por Antucayen.
 * Cualquier nombre no incluido aquí se considera un perfil legado/no autorizado.
 */
public enum RolSistema {
    ADMINISTRADOR("Administrador"),
    BODEGUERO("Bodeguero"),
    CAJERO("Cajero");

    private final String nombrePerfil;

    RolSistema(String nombrePerfil) {
        this.nombrePerfil = nombrePerfil;
    }

    public String getNombrePerfil() {
        return nombrePerfil;
    }

    public boolean coincide(String nombre) {
        return nombre != null && nombrePerfil.equalsIgnoreCase(nombre.trim());
    }

    public static RolSistema desdeNombre(String nombre) {
        for (RolSistema rol : values()) {
            if (rol.coincide(nombre)) return rol;
        }
        return null;
    }

    public static boolean esSoportado(String nombre) {
        return desdeNombre(nombre) != null;
    }
}
