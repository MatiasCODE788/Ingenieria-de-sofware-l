package cl.antucayen.model.entity;

public class EquivalenciaSku {
    private int    idEquivalencia;
    private String skuInterno;
    private String rutProveedor;
    private String codigoProveedor;

    public EquivalenciaSku() {}

    public EquivalenciaSku(int idEquivalencia, String skuInterno,
                           String rutProveedor, String codigoProveedor) {
        this.idEquivalencia  = idEquivalencia;
        this.skuInterno      = skuInterno;
        this.rutProveedor    = rutProveedor;
        this.codigoProveedor = codigoProveedor;
    }

    public int    getIdEquivalencia()  { return idEquivalencia; }
    public String getSkuInterno()      { return skuInterno; }
    public String getRutProveedor()    { return rutProveedor; }
    public String getCodigoProveedor() { return codigoProveedor; }

    public void setIdEquivalencia(int id)               { this.idEquivalencia = id; }
    public void setSkuInterno(String skuInterno)        { this.skuInterno = skuInterno; }
    public void setRutProveedor(String rutProveedor)    { this.rutProveedor = rutProveedor; }
    public void setCodigoProveedor(String codigo)       { this.codigoProveedor = codigo; }
}