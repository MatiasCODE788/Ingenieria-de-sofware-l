package cl.antucayen.model.service;

import cl.antucayen.model.entity.ErrorImportacion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ServicioImportacionInventario {

    /** Una fila cruda leída del archivo, aún sin validar tipo de dato. */
    public record FilaCruda(int numeroFila, String sku, String cantidadTexto) {}

    /** Resultado de la lectura + validación estructural del archivo. */
    public record ResultadoLectura(boolean estructuraValida, List<FilaCruda> filas,
                                   List<ErrorImportacion> erroresEstructura) {}

    private static final Set<String> ALIAS_SKU = Set.of("sku", "codigo", "código");
    private static final Set<String> ALIAS_CANTIDAD = Set.of("cantidad", "cant", "qty");

    /**
     * Lee el archivo (.xlsx/.xls o .csv) y valida que la primera fila
     * contenga las columnas SKU y Cantidad ANTES de tocar cualquier dato
     * de inventario. Si la estructura no es válida, retorna sin filas.
     */
    public ResultadoLectura leerYValidar(String rutaArchivo) throws IOException {
        if (rutaArchivo.endsWith(".xlsx") || rutaArchivo.endsWith(".xls")) {
            return leerExcel(rutaArchivo);
        } else if (rutaArchivo.endsWith(".csv")) {
            return leerCsv(rutaArchivo);
        }
        return new ResultadoLectura(false, List.of(), List.of(
                new ErrorImportacion(0, "archivo",
                        "Formato no soportado. Usa .xlsx, .xls o .csv")));
    }

    private ResultadoLectura leerExcel(String ruta) throws IOException {
        try (FileInputStream fis = new FileInputStream(ruta);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) return errorSinEncabezado();

            int colSku = ubicarColumna(header, ALIAS_SKU);
            int colCantidad = ubicarColumna(header, ALIAS_CANTIDAD);
            if (colSku < 0 || colCantidad < 0) return errorEncabezadoInvalido();

            List<FilaCruda> filas = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell cSku = row.getCell(colSku);
                Cell cCant = row.getCell(colCantidad);
                if (cSku == null || cCant == null) continue;

                String sku = cSku.getCellType() == CellType.STRING
                        ? cSku.getStringCellValue().trim()
                        : String.valueOf((long) cSku.getNumericCellValue());
                String cant = cCant.getCellType() == CellType.NUMERIC
                        ? String.valueOf((int) cCant.getNumericCellValue())
                        : cCant.getStringCellValue().trim();

                if (!sku.isEmpty()) filas.add(new FilaCruda(i, sku, cant)); // i = número de fila real (0-index, fila 0 es header)
            }
            return new ResultadoLectura(true, filas, List.of());
        }
    }

    private ResultadoLectura leerCsv(String ruta) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String primeraLinea = br.readLine();
            if (primeraLinea == null) return errorSinEncabezado();

            String[] encabezados = primeraLinea.split("[,;]");
            int colSku = ubicarColumna(encabezados, ALIAS_SKU);
            int colCantidad = ubicarColumna(encabezados, ALIAS_CANTIDAD);
            if (colSku < 0 || colCantidad < 0) return errorEncabezadoInvalido();

            List<FilaCruda> filas = new ArrayList<>();
            String linea;
            int numeroFila = 1;
            while ((linea = br.readLine()) != null) {
                String[] cols = linea.split("[,;]");
                if (cols.length > colSku && cols.length > colCantidad) {
                    String sku = cols[colSku].trim();
                    String cant = cols[colCantidad].trim();
                    if (!sku.isEmpty()) filas.add(new FilaCruda(numeroFila, sku, cant));
                }
                numeroFila++;
            }
            return new ResultadoLectura(true, filas, List.of());
        }
    }

    private int ubicarColumna(Row header, Set<String> alias) {
        for (Cell c : header) {
            String texto = c.getCellType() == CellType.STRING ? c.getStringCellValue() : "";
            if (alias.contains(texto.trim().toLowerCase())) return c.getColumnIndex();
        }
        return -1;
    }

    private int ubicarColumna(String[] encabezados, Set<String> alias) {
        for (int i = 0; i < encabezados.length; i++) {
            if (alias.contains(encabezados[i].trim().toLowerCase())) return i;
        }
        return -1;
    }

    private ResultadoLectura errorSinEncabezado() {
        return new ResultadoLectura(false, List.of(), List.of(
                new ErrorImportacion(0, "encabezado", "El archivo está vacío o no tiene fila de encabezado")));
    }

    private ResultadoLectura errorEncabezadoInvalido() {
        return new ResultadoLectura(false, List.of(), List.of(
                new ErrorImportacion(0, "encabezado",
                        "La primera fila debe contener las columnas 'SKU' y 'Cantidad'")));
    }

    /**
     * Detecta SKUs que aparecen más de una vez en el archivo.
     * @return mapa SKU (en mayúsculas) -> lista de números de fila donde aparece
     */
    public Map<String, List<Integer>> detectarDuplicados(List<FilaCruda> filas) {
        Map<String, List<Integer>> mapa = new LinkedHashMap<>();
        for (FilaCruda f : filas) {
            mapa.computeIfAbsent(f.sku().toUpperCase(), k -> new ArrayList<>()).add(f.numeroFila());
        }
        mapa.values().removeIf(lista -> lista.size() < 2);
        return mapa;
    }
}
