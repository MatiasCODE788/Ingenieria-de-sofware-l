package cl.antucayen.model.entity;

public class Usuario {
    private int idUsuario;
    private String nombreCompleto;
    private String username;
    private boolean estadoActivo;
    private int idPerfil;
    private String nombrePerfil;

    public Usuario() {}

    /**
     * Constructor de compatibilidad para código de pruebas y llamadas históricas.
     * Si no se entrega un nombre visible separado, se utiliza el username.
     */
    public Usuario(int idUsuario, String username, boolean estadoActivo,
                   int idPerfil, String nombrePerfil) {
        this(idUsuario, username, username, estadoActivo, idPerfil, nombrePerfil);
    }

    public Usuario(int idUsuario, String nombreCompleto, String username,
                   boolean estadoActivo, int idPerfil, String nombrePerfil) {
        this.idUsuario = idUsuario;
        this.nombreCompleto = nombreCompleto;
        this.username = username;
        this.estadoActivo = estadoActivo;
        this.idPerfil = idPerfil;
        this.nombrePerfil = nombrePerfil;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getUsername() { return username; }
    public boolean isEstadoActivo() { return estadoActivo; }
    public int getIdPerfil() { return idPerfil; }
    public String getNombrePerfil() { return nombrePerfil; }

    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public void setUsername(String username) { this.username = username; }
    public void setEstadoActivo(boolean estadoActivo) { this.estadoActivo = estadoActivo; }
    public void setIdPerfil(int idPerfil) { this.idPerfil = idPerfil; }
    public void setNombrePerfil(String nombrePerfil) { this.nombrePerfil = nombrePerfil; }
}
