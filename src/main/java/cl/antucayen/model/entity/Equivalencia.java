package cl.antucayen.model.entity;

public class Equivalencia {
    private int    idProveedor;
    private String codigoInternoProveedor;
    private String sku;

    public Equivalencia() {}

    public Equivalencia(int idProveedor, String codigoInternoProveedor, String sku) {
        this.idProveedor             = idProveedor;
        this.codigoInternoProveedor  = codigoInternoProveedor;
        this.sku                     = sku;
    }

    public int    getIdProveedor()            { return idProveedor; }
    public String getCodigoInternoProveedor() { return codigoInternoProveedor; }
    public String getSku()                    { return sku; }

    public void setIdProveedor(int idProveedor)                        { this.idProveedor = idProveedor; }
    public void setCodigoInternoProveedor(String codigoInternoProveedor){ this.codigoInternoProveedor = codigoInternoProveedor; }
    public void setSku(String sku)                                     { this.sku = sku; }
}