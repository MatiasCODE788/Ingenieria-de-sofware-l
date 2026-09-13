package cl.antucayen.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VAjusteInventarioTest {

    @Test
    void modalidadEsObligatoriaSinValorPorDefectoYBotonesCoincidenConRf41() {
        VAjusteInventario vista = new VAjusteInventario();

        assertNull(vista.getModalidad());
        assertEquals("Confirmar ajuste", vista.getBtnConfirmar().getText());
        assertEquals("Cancelar", vista.getBtnCancelar().getText());
    }
}
