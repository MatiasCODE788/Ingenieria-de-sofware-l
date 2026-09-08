package cl.antucayen.model.entity;

/**
 * Representa un pago individual dentro de una venta. Una venta pagada con
 * un solo medio tiene un único PagoVenta; una venta con pago dividido
 * (ej.: parte en Efectivo y parte en Débito) tiene uno por cada medio usado.
 */
public class PagoVenta {
    private int    idPago;
    private int    idVenta;
    private String medioPago;   // Efectivo, Débito, Crédito
    private int    monto;

    public PagoVenta() {}

    public PagoVenta(String medioPago, int monto) {
        this.medioPago = medioPago;
        this.monto     = monto;
    }

    public PagoVenta(int idPago, int idVenta, String medioPago, int monto) {
        this.idPago    = idPago;
        this.idVenta   = idVenta;
        this.medioPago = medioPago;
        this.monto     = monto;
    }

    public int    getIdPago()    { return idPago; }
    public int    getIdVenta()   { return idVenta; }
    public String getMedioPago() { return medioPago; }
    public int    getMonto()     { return monto; }

    public void setIdPago(int idPago)       { this.idPago = idPago; }
    public void setIdVenta(int idVenta)     { this.idVenta = idVenta; }
    public void setMedioPago(String medio)  { this.medioPago = medio; }
    public void setMonto(int monto)         { this.monto = monto; }
}