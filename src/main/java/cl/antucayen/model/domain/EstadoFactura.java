package cl.antucayen.model.domain;

/** Estados persistidos de factura. */
public enum EstadoFactura {
    PENDIENTE("Pendiente"),
    PROCESADA("Procesada"),
    OBSERVADA("Observada");

    private final String valorDb;

    EstadoFactura(String valorDb) {
        this.valorDb = valorDb;
    }

    public String valorDb() {
        return valorDb;
    }

    public boolean coincide(String valor) {
        return valorDb.equals(valor);
    }
}
