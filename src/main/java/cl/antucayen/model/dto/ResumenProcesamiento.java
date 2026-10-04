package cl.antucayen.model.dto;

/**
 * Resumen inmutable del estado de procesamiento de los ítems de una factura.
 * Se mantiene fuera del servicio para evitar que controladores y vistas
 * dependan de tipos internos de la capa de negocio.
 */
public record ResumenProcesamiento(
        int leidos,
        int validos,
        int observados,
        int noProcesados) {
}
