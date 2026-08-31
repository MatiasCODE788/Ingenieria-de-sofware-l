package cl.antucayen.model.exception;

/**
 * Se lanza cuando se intenta registrar una equivalencia (código interno de
 * proveedor -> SKU) que ya existe para ese mismo proveedor.
 */
public class EquivalenciaDuplicadaException extends Exception {

    public EquivalenciaDuplicadaException(String codigoInterno) {
        super("Ya existe una equivalencia registrada para el código interno '"
                + codigoInterno + "' con este proveedor.");
    }
}
