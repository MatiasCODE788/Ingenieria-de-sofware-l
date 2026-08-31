package cl.antucayen.model.entity;

public class Equivalencia {
    private int    idProveedor;
    private String codigoInternoProveedor;
    private String sku;
    private String nombreProveedor; // se carga con JOIN, solo lectura

    public Equivalencia() {}

    public Equivalencia(int idProveedor, String codigoInternoProveedor, String sku) {
        this.idProveedor             = idProveedor;
        this.codigoInternoProveedor  = codigoInternoProveedor;
        this.sku                     = sku;
    }

    public Equivalencia(int idProveedor, String codigoInternoProveedor, String sku,
                        String nombreProveedor) {
        this.idProveedor             = idProveedor;
        this.codigoInternoProveedor  = codigoInternoProveedor;
        this.sku                     = sku;
        this.nombreProveedor         = nombreProveedor;
    }

    public int    getIdProveedor()            { return idProveedor; }
    public String getCodigoInternoProveedor() { return codigoInternoProveedor; }
    public String getSku()                    { return sku; }
    public String getNombreProveedor()        { return nombreProveedor; }

    public void setIdProveedor(int idProveedor)                        { this.idProveedor = idProveedor; }
    public void setCodigoInternoProveedor(String codigoInternoProveedor){ this.codigoInternoProveedor = codigoInternoProveedor; }
    public void setSku(String sku)                                     { this.sku = sku; }
    public void setNombreProveedor(String nombreProveedor)              { this.nombreProveedor = nombreProveedor; }
}