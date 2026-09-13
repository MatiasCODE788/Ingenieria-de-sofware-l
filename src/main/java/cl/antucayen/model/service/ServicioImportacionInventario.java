package cl.antucayen.model.service;

import cl.antucayen.model.entity.ErrorImportacion;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ServicioImportacionInventario {

    public static final long TAMANO_MAXIMO_BYTES = 10L * 1024L * 1024L;

    public record FilaCruda(int numeroFila, String sku, String cantidadTexto) {}

    public record ResultadoLectura(boolean estructuraValida, List<FilaCruda> filas,
                                   List<ErrorImportacion> erroresLectura) {}

    // El Incremento 2 exige exactamente las columnas SKU y cantidad.
    // La comparación es case-insensitive, pero no se aceptan alias semánticos.
    private static final Set<String> ENCABEZADO_SKU = Set.of("sku");
    private static final Set<String> ENCABEZADO_CANTIDAD = Set.of("cantidad");

    /**
     * Lee archivos Excel o CSV y valida formato, tamaño, encabezados y filas
     * antes de cualquier modificación de inventario.
     */
    public ResultadoLectura leerYValidar(String rutaArchivo) throws IOException {
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            return errorFormato("Debes seleccionar un archivo");
        }

        File archivo = new File(rutaArchivo);
        if (!archivo.isFile()) {
            return errorFormato("El archivo seleccionado no existe o no es un archivo válido");
        }
        if (archivo.length() > TAMANO_MAXIMO_BYTES) {
            return errorFormato("El archivo supera el límite de 10 MB");
        }

        String rutaNormalizada = rutaArchivo.toLowerCase(Locale.ROOT);
        if (rutaNormalizada.endsWith(".xlsx") || rutaNormalizada.endsWith(".xls")) {
            return leerExcel(rutaArchivo);
        }
        if (rutaNormalizada.endsWith(".csv")) {
            return leerCsv(rutaArchivo);
        }
        return errorFormato("Formato no soportado. Usa .xlsx, .xls o .csv");
    }

    private ResultadoLectura leerExcel(String ruta) throws IOException {
        try (FileInputStream fis = new FileInputStream(ruta);
             Workbook wb = WorkbookFactory.create(fis)) {
            if (wb.getNumberOfSheets() == 0) return errorSinEncabezado();

            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return errorSinEncabezado();

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            int colSku = ubicarColumna(header, ENCABEZADO_SKU, formatter, evaluator);
            int colCantidad = ubicarColumna(header, ENCABEZADO_CANTIDAD, formatter, evaluator);
            if (colSku < 0 || colCantidad < 0) return errorEncabezadoInvalido();

            List<FilaCruda> filas = new ArrayList<>();
            List<ErrorImportacion> errores = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                int numeroFila = i + 1;
                Row row = sheet.getRow(i);
                if (row == null) {
                    errores.add(new ErrorImportacion(numeroFila, "fila", "", "Fila vacía"));
                    continue;
                }

                String sku = valorCelda(row.getCell(colSku), formatter, evaluator);
                String cantidad = valorCelda(row.getCell(colCantidad), formatter, evaluator);

                if (sku.isBlank() && cantidad.isBlank()) {
                    errores.add(new ErrorImportacion(numeroFila, "fila", "", "Fila vacía"));
                    continue;
                }
                if (sku.isBlank()) {
                    errores.add(new ErrorImportacion(numeroFila, "SKU", "", "SKU vacío"));
                    continue;
                }
                if (cantidad.isBlank()) {
                    errores.add(new ErrorImportacion(numeroFila, "cantidad", sku, "Cantidad vacía"));
                    continue;
                }
                filas.add(new FilaCruda(numeroFila, sku.trim(), normalizarNumeroExcel(cantidad)));
            }
            return new ResultadoLectura(true, List.copyOf(filas), List.copyOf(errores));
        } catch (RuntimeException ex) {
            throw new IOException("No se pudo interpretar el archivo Excel: " + ex.getMessage(), ex);
        }
    }

    private String valorCelda(Cell celda, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (celda == null) return "";
        return formatter.formatCellValue(celda, evaluator).trim();
    }

    private String normalizarNumeroExcel(String valor) {
        String normalizado = valor.trim();
        if (normalizado.matches("-?\\d+[\\.,]0+")) {
            return normalizado.substring(0,
                    Math.max(normalizado.lastIndexOf('.'), normalizado.lastIndexOf(',')));
        }
        return normalizado;
    }

    private ResultadoLectura leerCsv(String ruta) throws IOException {
        ResultadoLectura conComa = leerCsvConSeparador(Path.of(ruta), ',');
        if (conComa.estructuraValida()) return conComa;

        ResultadoLectura conPuntoComa = leerCsvConSeparador(Path.of(ruta), ';');
        if (conPuntoComa.estructuraValida()) return conPuntoComa;

        return errorEncabezadoInvalido();
    }

    private ResultadoLectura leerCsvConSeparador(Path ruta, char separador) throws IOException {
        List<FilaCruda> filas = new ArrayList<>();
        List<ErrorImportacion> errores = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(ruta, StandardCharsets.UTF_8);
             CSVReader csv = new CSVReaderBuilder(reader)
                     .withCSVParser(new CSVParserBuilder().withSeparator(separador).build())
                     .build()) {

            String[] encabezados = csv.readNext();
            if (encabezados == null) return errorSinEncabezado();

            int colSku = ubicarColumna(encabezados, ENCABEZADO_SKU);
            int colCantidad = ubicarColumna(encabezados, ENCABEZADO_CANTIDAD);
            if (colSku < 0 || colCantidad < 0) return errorEncabezadoInvalido();

            String[] cols;
            int numeroFila = 2;
            while ((cols = csv.readNext()) != null) {
                if (filaVacia(cols)) {
                    errores.add(new ErrorImportacion(numeroFila, "fila", "", "Fila vacía"));
                    numeroFila++;
                    continue;
                }
                if (cols.length <= Math.max(colSku, colCantidad)) {
                    errores.add(new ErrorImportacion(numeroFila, "fila", "", "Fila incompleta"));
                    numeroFila++;
                    continue;
                }

                String sku = cols[colSku] == null ? "" : cols[colSku].trim();
                String cantidad = cols[colCantidad] == null ? "" : cols[colCantidad].trim();
                if (sku.isBlank() && cantidad.isBlank()) {
                    errores.add(new ErrorImportacion(numeroFila, "fila", "", "Fila vacía"));
                } else if (sku.isBlank()) {
                    errores.add(new ErrorImportacion(numeroFila, "SKU", "", "SKU vacío"));
                } else if (cantidad.isBlank()) {
                    errores.add(new ErrorImportacion(numeroFila, "cantidad", sku, "Cantidad vacía"));
                } else {
                    filas.add(new FilaCruda(numeroFila, sku, cantidad));
                }
                numeroFila++;
            }
            return new ResultadoLectura(true, List.copyOf(filas), List.copyOf(errores));
        } catch (CsvValidationException ex) {
            throw new IOException("CSV inválido: " + ex.getMessage(), ex);
        }
    }

    private boolean filaVacia(String[] columnas) {
        if (columnas.length == 0) return true;
        for (String valor : columnas) {
            if (valor != null && !valor.isBlank()) return false;
        }
        return true;
    }

    private int ubicarColumna(Row header, Set<String> encabezados,
                              DataFormatter formatter, FormulaEvaluator evaluator) {
        for (Cell c : header) {
            String texto = valorCelda(c, formatter, evaluator).toLowerCase(Locale.ROOT);
            if (encabezados.contains(texto)) return c.getColumnIndex();
        }
        return -1;
    }

    private int ubicarColumna(String[] encabezados, Set<String> permitidos) {
        for (int i = 0; i < encabezados.length; i++) {
            String valor = encabezados[i] == null ? "" : encabezados[i].trim().toLowerCase(Locale.ROOT);
            if (i == 0 && valor.startsWith("\ufeff")) valor = valor.substring(1);
            if (permitidos.contains(valor)) return i;
        }
        return -1;
    }

    private ResultadoLectura errorFormato(String mensaje) {
        return new ResultadoLectura(false, List.of(), List.of(
                new ErrorImportacion(0, "archivo", "", mensaje)));
    }

    private ResultadoLectura errorSinEncabezado() {
        return new ResultadoLectura(false, List.of(), List.of(
                new ErrorImportacion(0, "encabezado", "",
                        "El archivo está vacío o no tiene fila de encabezado")));
    }

    private ResultadoLectura errorEncabezadoInvalido() {
        return new ResultadoLectura(false, List.of(), List.of(
                new ErrorImportacion(0, "encabezado", "",
                        "Estructura inválida: faltan las columnas SKU o cantidad")));
    }

    /** @return mapa SKU normalizado -> números de fila en que aparece. */
    public Map<String, List<Integer>> detectarDuplicados(List<FilaCruda> filas) {
        Map<String, List<Integer>> mapa = new LinkedHashMap<>();
        for (FilaCruda fila : filas) {
            String skuNormalizado = fila.sku().trim().toUpperCase(Locale.ROOT);
            mapa.computeIfAbsent(skuNormalizado, k -> new ArrayList<>()).add(fila.numeroFila());
        }
        mapa.values().removeIf(lista -> lista.size() < 2);
        return mapa;
    }
}
