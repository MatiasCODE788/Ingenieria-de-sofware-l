package cl.antucayen.model.dto;

/** Proyección de lectura para el ranking de productos vendidos. */
public record ProductoVendido(String sku, String nombre, int cantidadTotal, int montoTotal) {}
