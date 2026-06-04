package cl.antucayen.model.entity;

public class Proveedor {
    private int    idProveedor;
    private String rut;
    private String nombre;
    private String telefono;
    private String correoElectronico;

    public Proveedor() {}

    public Proveedor(int idProveedor, String rut, String nombre,
                     String telefono, String correoElectronico) {
        this.idProveedor       = idProveedor;
        this.rut               = rut;
        this.nombre            = nombre;
        this.telefono          = telefono;
        this.correoElectronico = correoElectronico;
    }

    public int    getIdProveedor()       { return idProveedor; }
    public String getRut()               { return rut; }
    public String getNombre()            { return nombre; }
    public String getTelefono()          { return telefono; }
    public String getCorreoElectronico() { return correoElectronico; }

    public void setIdProveedor(int idProveedor)              { this.idProveedor = idProveedor; }
    public void setRut(String rut)                           { this.rut = rut; }
    public void setNombre(String nombre)                     { this.nombre = nombre; }
    public void setTelefono(String telefono)                 { this.telefono = telefono; }
    public void setCorreoElectronico(String correoElectronico){ this.correoElectronico = correoElectronico; }
}