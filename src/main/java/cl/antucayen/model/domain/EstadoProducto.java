package cl.antucayen.model.domain;

/** Estados persistidos de producto. Mantiene exactamente los valores usados por MariaDB. */
public enum EstadoProducto {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String valorDb;

    EstadoProducto(String valorDb) {
        this.valorDb = valorDb;
    }

    public String valorDb() {
        return valorDb;
    }

    public boolean coincide(String valor) {
        return valorDb.equals(valor);
    }
}
