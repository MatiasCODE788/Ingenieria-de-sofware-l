package cl.antucayen.model.domain;

/** Estados persistidos de cada ítem de factura. */
public enum EstadoItemFactura {
    VALIDO("Válido"),
    OBSERVADO("Observado"),
    NO_PROCESADO("No Procesado");

    private final String valorDb;

    EstadoItemFactura(String valorDb) {
        this.valorDb = valorDb;
    }

    public String valorDb() {
        return valorDb;
    }

    public boolean coincide(String valor) {
        return valorDb.equals(valor);
    }
}
