package cl.antucayen.model.domain;

/** Estados persistidos de ajustes de inventario. */
public enum EstadoAjusteInventario {
    PENDIENTE("Pendiente"),
    APLICADO("Aplicado"),
    REVERTIDO("Revertido");

    private final String valorDb;

    EstadoAjusteInventario(String valorDb) {
        this.valorDb = valorDb;
    }

    public String valorDb() {
        return valorDb;
    }

    public boolean coincide(String valor) {
        return valorDb.equals(valor);
    }
}
