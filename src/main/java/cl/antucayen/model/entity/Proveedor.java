package cl.antucayen.model.entity;

public class Proveedor {
    private String rut;
    private String nombre;
    private String telefono;
    private String correo;
    private String condicionesComerciales;

    public Proveedor() {}

    public Proveedor(String rut, String nombre, String telefono,
                     String correo, String condicionesComerciales) {
        this.rut                   = rut;
        this.nombre                = nombre;
        this.telefono              = telefono;
        this.correo                = correo;
        this.condicionesComerciales = condicionesComerciales;
    }

    public String getRut()                      { return rut; }
    public String getNombre()                   { return nombre; }
    public String getTelefono()                 { return telefono; }
    public String getCorreo()                   { return correo; }
    public String getCondicionesComerciales()   { return condicionesComerciales; }

    public void setRut(String rut)                                      { this.rut = rut; }
    public void setNombre(String nombre)                                { this.nombre = nombre; }
    public void setTelefono(String telefono)                            { this.telefono = telefono; }
    public void setCorreo(String correo)                                { this.correo = correo; }
    public void setCondicionesComerciales(String condicionesComerciales){ this.condicionesComerciales = condicionesComerciales; }
}