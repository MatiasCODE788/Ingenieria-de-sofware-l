package cl.antucayen.model.entity;

public class Usuario {
    private int     idUsuario;
    private String  username;
    private String  passwordHash;
    private boolean estadoActivo;
    private int     idPerfil;
    private String  nombrePerfil; // se carga con JOIN

    public Usuario() {}

    public Usuario(int idUsuario, String username, String passwordHash,
                   boolean estadoActivo, int idPerfil, String nombrePerfil) {
        this.idUsuario    = idUsuario;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.estadoActivo = estadoActivo;
        this.idPerfil     = idPerfil;
        this.nombrePerfil = nombrePerfil;
    }

    public int     getIdUsuario()    { return idUsuario; }
    public String  getUsername()     { return username; }
    public String  getPasswordHash() { return passwordHash; }
    public boolean isEstadoActivo()  { return estadoActivo; }
    public int     getIdPerfil()     { return idPerfil; }
    public String  getNombrePerfil() { return nombrePerfil; }

    public void setIdUsuario(int idUsuario)          { this.idUsuario = idUsuario; }
    public void setUsername(String username)          { this.username = username; }
    public void setPasswordHash(String passwordHash)  { this.passwordHash = passwordHash; }
    public void setEstadoActivo(boolean estadoActivo) { this.estadoActivo = estadoActivo; }
    public void setIdPerfil(int idPerfil)             { this.idPerfil = idPerfil; }
    public void setNombrePerfil(String nombrePerfil)  { this.nombrePerfil = nombrePerfil; }
}