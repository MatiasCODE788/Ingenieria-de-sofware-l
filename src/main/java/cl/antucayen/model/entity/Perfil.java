package cl.antucayen.model.entity;

public class Perfil {
    private int    idPerfil;
    private String nombrePerfil;

    public Perfil() {}

    public Perfil(int idPerfil, String nombrePerfil) {
        this.idPerfil      = idPerfil;
        this.nombrePerfil  = nombrePerfil;
    }

    public int    getIdPerfil()     { return idPerfil; }
    public String getNombrePerfil() { return nombrePerfil; }

    public void setIdPerfil(int idPerfil)          { this.idPerfil = idPerfil; }
    public void setNombrePerfil(String nombrePerfil){ this.nombrePerfil = nombrePerfil; }

    @Override
    public String toString() { return nombrePerfil; }
}