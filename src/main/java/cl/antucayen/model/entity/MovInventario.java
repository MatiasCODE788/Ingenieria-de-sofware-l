package cl.antucayen.model.entity;

import java.time.LocalDate;
import java.time.LocalTime;

public class MovInventario {
    private int       idMovimiento;
    private LocalDate fecha;
    private LocalTime hora;
    private int       idUsuario;
    private String    sku;
    private int       stockAnterior;
    private int       cantidadAplicada;
    private String    motivo;

    public MovInventario() {}

    public MovInventario(int idMovimiento, LocalDate fecha, LocalTime hora,
                         int idUsuario, String sku, int stockAnterior,
                         int cantidadAplicada, String motivo) {
        this.idMovimiento    = idMovimiento;
        this.fecha           = fecha;
        this.hora            = hora;
        this.idUsuario       = idUsuario;
        this.sku             = sku;
        this.stockAnterior   = stockAnterior;
        this.cantidadAplicada = cantidadAplicada;
        this.motivo          = motivo;
    }

    public int       getIdMovimiento()    { return idMovimiento; }
    public LocalDate getFecha()           { return fecha; }
    public LocalTime getHora()            { return hora; }
    public int       getIdUsuario()       { return idUsuario; }
    public String    getSku()             { return sku; }
    public int       getStockAnterior()   { return stockAnterior; }
    public int       getCantidadAplicada(){ return cantidadAplicada; }
    public String    getMotivo()          { return motivo; }

    public void setIdMovimiento(int id)              { this.idMovimiento = id; }
    public void setFecha(LocalDate fecha)            { this.fecha = fecha; }
    public void setHora(LocalTime hora)              { this.hora = hora; }
    public void setIdUsuario(int idUsuario)          { this.idUsuario = idUsuario; }
    public void setSku(String sku)                   { this.sku = sku; }
    public void setStockAnterior(int stockAnterior)  { this.stockAnterior = stockAnterior; }
    public void setCantidadAplicada(int cantidad)    { this.cantidadAplicada = cantidad; }
    public void setMotivo(String motivo)             { this.motivo = motivo; }
}