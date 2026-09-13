package cl.antucayen.model.exception;

/** Se lanza cuando el vínculo proveedor + código interno ya está registrado. */
public class EquivalenciaDuplicadaException extends Exception {

    private static final long serialVersionUID = 1L;

    public EquivalenciaDuplicadaException(String codigoInterno) {
        super("Equivalencia duplicada: el vínculo no fue registrado");
    }
}
