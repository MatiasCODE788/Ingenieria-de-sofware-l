package cl.antucayen.model.service;

/**
 * Desglosa en pesos chilenos el precio de una venta que YA INCLUYE IVA (19%).
 * No altera el total cobrado ni requiere columnas o migraciones de base de datos.
 * Solo se aplica a operaciones afectas a IVA general.
 */
public final class CalculadoraImpuestos {
    private CalculadoraImpuestos() {}

    public record Desglose(int neto, int iva, int total) {}

    /**
     * Extrae el IVA incluido en el monto bruto: neto = bruto * 100 / 119.
     * El neto se redondea al peso más cercano; IVA = bruto - neto para
     * asegurar que el desglose siempre coincida con el monto a cobrar.
     */
    public static Desglose desglosar(int bruto) {
        if (bruto < 0) {
            throw new IllegalArgumentException("El total no puede ser negativo");
        }
        // Uso de long para evitar desbordamiento en bruto * 100.
        int neto = Math.toIntExact(((long) bruto * 100 + 59) / 119);
        return new Desglose(neto, bruto - neto, bruto);
    }
}
