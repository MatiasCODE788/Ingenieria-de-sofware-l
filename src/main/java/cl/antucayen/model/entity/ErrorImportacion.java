package cl.antucayen.model.entity;

/**
 * Representa un error detectado durante una importación de archivo o un
 * procesamiento (fila/ítem, código o SKU involucrado, y descripción).
 * No se persiste en base de datos, es solo para reportar en pantalla.
 */
public class ErrorImportacion {
    private int    fila;      // número de fila/ítem (1 = primera fila de datos)
    private String columna;   // nombre de columna o código/SKU involucrado
    private String descripcion;

    public ErrorImportacion(int fila, String columna, String descripcion) {
        this.fila        = fila;
        this.columna      = columna;
        this.descripcion  = descripcion;
    }

    public int    getFila()        { return fila; }
    public String getColumna()     { return columna; }
    public String getDescripcion() { return descripcion; }
}
