package cl.antucayen.model.service;

import cl.antucayen.model.entity.PagoVenta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioVentaPagoTest {

    private final ServicioVenta servicio = new ServicioVenta();

    @Test
    void pagoExactoNoGeneraVuelto() {
        int vuelto = servicio.calcularVuelto(
                List.of(new PagoVenta("Efectivo", 8_500)),
                8_500);

        assertEquals(0, vuelto);
    }

    @Test
    void efectivoSuperiorGeneraVuelto() {
        int vuelto = servicio.calcularVuelto(
                List.of(new PagoVenta("Efectivo", 10_000)),
                8_500);

        assertEquals(1_500, vuelto);
    }

    @Test
    void pagoMixtoPuedeGenerarVueltoSoloDesdeEfectivo() {
        int vuelto = servicio.calcularVuelto(
                List.of(
                        new PagoVenta("Débito", 3_000),
                        new PagoVenta("Efectivo", 7_000)),
                8_500);

        assertEquals(1_500, vuelto);
    }

    @Test
    void sobrepagoElectronicoSinEfectivoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcularVuelto(
                        List.of(new PagoVenta("Débito", 10_000)),
                        8_500));
    }

    @Test
    void pagoInsuficienteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcularVuelto(
                        List.of(new PagoVenta("Efectivo", 8_000)),
                        8_500));
    }
    @Test
    void sobrepagoMixtoSeRechazaSiElVueltoSuperaElEfectivoRecibido() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcularVuelto(
                        List.of(
                                new PagoVenta("Débito", 9_000),
                                new PagoVenta("Efectivo", 1_000)),
                        8_500));
    }

    @Test
    void medioDePagoDesconocidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcularVuelto(
                        List.of(new PagoVenta("Transferencia", 8_500)),
                        8_500));
    }

    @Test
    void pagoCeroSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcularVuelto(
                        List.of(new PagoVenta("Efectivo", 0)),
                        0));
    }

    @Test
    void totalNegativoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcularVuelto(
                        List.of(new PagoVenta("Efectivo", 1)),
                        -1));
    }

    @Test
    void desbordamientoEnSumaDePagosSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.calcularVuelto(
                        List.of(
                                new PagoVenta("Efectivo", Integer.MAX_VALUE),
                                new PagoVenta("Efectivo", 1)),
                        Integer.MAX_VALUE));
    }

}
