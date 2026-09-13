package cl.antucayen.model.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServicioImportacionInventarioTest {

    @TempDir
    Path tempDir;

    private final ServicioImportacionInventario servicio = new ServicioImportacionInventario();

    @Test
    void csvRespetaCamposEntreComillasYComasEnDescripcion() throws IOException {
        Path archivo = tempDir.resolve("inventario.csv");
        Files.writeString(archivo,
                "SKU,Cantidad,Descripcion\nABR-0001,10,\"Arroz, 1 kg\"\n",
                StandardCharsets.UTF_8);

        var resultado = servicio.leerYValidar(archivo.toString());

        assertTrue(resultado.estructuraValida());
        assertTrue(resultado.erroresLectura().isEmpty());
        assertEquals(1, resultado.filas().size());
        assertEquals("ABR-0001", resultado.filas().get(0).sku());
        assertEquals("10", resultado.filas().get(0).cantidadTexto());
    }

    @Test
    void csvAceptaPuntoYComaYExtensionEnMayusculas() throws IOException {
        Path archivo = tempDir.resolve("inventario.CSV");
        Files.writeString(archivo, "SKU;cantidad\nBEB-0001;5\n", StandardCharsets.UTF_8);

        var resultado = servicio.leerYValidar(archivo.toString());

        assertTrue(resultado.estructuraValida());
        assertEquals(1, resultado.filas().size());
        assertEquals("5", resultado.filas().get(0).cantidadTexto());
    }

    @Test
    void rechazaAliasDeEncabezadoNoPermitidos() throws IOException {
        Path archivo = tempDir.resolve("alias.csv");
        Files.writeString(archivo, "codigo,qty\nABC-1,3\n", StandardCharsets.UTF_8);

        var resultado = servicio.leerYValidar(archivo.toString());

        assertFalse(resultado.estructuraValida());
        assertEquals("Estructura inválida: faltan las columnas SKU o cantidad",
                resultado.erroresLectura().get(0).getDescripcion());
    }

    @Test
    void reportaFilaVaciaConColumnaAfectada() throws IOException {
        Path archivo = tempDir.resolve("vacia.csv");
        Files.writeString(archivo, "SKU,cantidad\nABC-1,2\n,\nXYZ-2,4\n", StandardCharsets.UTF_8);

        var resultado = servicio.leerYValidar(archivo.toString());

        assertTrue(resultado.estructuraValida());
        assertEquals(1, resultado.erroresLectura().size());
        assertEquals(3, resultado.erroresLectura().get(0).getFila());
        assertEquals("fila", resultado.erroresLectura().get(0).getColumna());
        assertEquals("Fila vacía", resultado.erroresLectura().get(0).getDescripcion());
    }

    @Test
    void rechazaArchivoSobreDiezMegabytesAntesDeProcesarlo() throws IOException {
        Path archivo = tempDir.resolve("grande.csv");
        Files.write(archivo,
                new byte[(int) ServicioImportacionInventario.TAMANO_MAXIMO_BYTES + 1]);

        var resultado = servicio.leerYValidar(archivo.toString());

        assertFalse(resultado.estructuraValida());
        assertTrue(resultado.erroresLectura().get(0).getDescripcion().contains("10 MB"));
    }

    @Test
    void detectaDuplicadosNormalizandoMayusculas() {
        List<ServicioImportacionInventario.FilaCruda> filas = List.of(
                new ServicioImportacionInventario.FilaCruda(2, "abc-1", "1"),
                new ServicioImportacionInventario.FilaCruda(3, "ABC-1", "2"),
                new ServicioImportacionInventario.FilaCruda(4, "XYZ-2", "3")
        );

        Map<String, List<Integer>> duplicados = servicio.detectarDuplicados(filas);

        assertEquals(1, duplicados.size());
        assertEquals(List.of(2, 3), duplicados.get("ABC-1"));
    }
}
