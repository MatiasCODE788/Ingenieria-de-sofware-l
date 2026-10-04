package cl.antucayen.model.domain;

/** Estados persistidos de venta. Se conserva EN_CURSO por compatibilidad histórica. */
public enum EstadoVenta {
    EN_CURSO("En curso"),
    PAGADA("Pagada"),
    CANCELADA("Cancelada"),
    ANULADA("Anulada");

    private final String valorDb;

    EstadoVenta(String valorDb) {
        this.valorDb = valorDb;
    }

    public String valorDb() {
        return valorDb;
    }

    public boolean coincide(String valor) {
        return valorDb.equals(valor);
    }
}
