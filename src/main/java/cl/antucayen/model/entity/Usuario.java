package cl.antucayen.model.entity;

public class Usuario {
    private int    idUsuario;
    private String nombreUsuario;
    private String contrasenaHash;
    private String rol;

    public Usuario() {}

    public Usuario(int idUsuario, String nombreUsuario,
                   String contrasenaHash, String rol) {
        this.idUsuario      = idUsuario;
        this.nombreUsuario  = nombreUsuario;
        this.contrasenaHash = contrasenaHash;
        this.rol            = rol;
    }

    public int    getIdUsuario()      { return idUsuario; }
    public String getNombreUsuario()  { return nombreUsuario; }
    public String getContrasenaHash() { return contrasenaHash; }
    public String getRol()            { return rol; }

    public void setIdUsuario(int idUsuario)           { this.idUsuario = idUsuario; }
    public void setNombreUsuario(String nombreUsuario){ this.nombreUsuario = nombreUsuario; }
    public void setContrasenaHash(String hash)        { this.contrasenaHash = hash; }
    public void setRol(String rol)                    { this.rol = rol; }
}