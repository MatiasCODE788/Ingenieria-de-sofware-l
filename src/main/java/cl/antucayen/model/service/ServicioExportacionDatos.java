package cl.antucayen.model.service;

import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.security.Autorizacion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Exportación local de datos para la aplicación Swing.
 *
 * <p>El proyecto no expone una API HTTP: las vistas desktop solicitan el archivo
 * mediante JFileChooser y este servicio genera CSV UTF-8 o Excel XLSX. La
 * autorización se verifica nuevamente en la capa de servicio para que la
 * seguridad no dependa únicamente de la visibilidad de los botones.</p>
 */
public class ServicioExportacionDatos {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

    public enum FormatoExportacion {
        CSV("csv", "CSV (*.csv)"),
        EXCEL("xlsx", "Excel (*.xlsx)");

        private final String extension;
        private final String descripcion;

        FormatoExportacion(String extension, String descripcion) {
            this.extension = extension;
            this.descripcion = descripcion;
        }

        public String getExtension() {
            return extension;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public Path exportarInventario(List<Producto> productos,
                                   Path destino,
                                   FormatoExportacion formato) throws IOException {
        Autorizacion.verificarConsultaStock();
        Objects.requireNonNull(productos, "La lista de productos no puede ser nula");
        Path archivo = prepararDestino(destino, formato);

        if (formato == FormatoExportacion.CSV) {
            exportarInventarioCsv(productos, archivo);
        } else {
            exportarInventarioExcel(productos, archivo);
        }
        return archivo;
    }

    public Path exportarMovimientos(List<MovimientoInventario> movimientos,
                                    Path destino,
                                    FormatoExportacion formato) throws IOException {
        Autorizacion.verificarHistorialInventario();
        Objects.requireNonNull(movimientos, "La lista de movimientos no puede ser nula");
        Path archivo = prepararDestino(destino, formato);

        if (formato == FormatoExportacion.CSV) {
            exportarMovimientosCsv(movimientos, archivo);
        } else {
            exportarMovimientosExcel(movimientos, archivo);
        }
        return archivo;
    }

    private Path prepararDestino(Path destino, FormatoExportacion formato) throws IOException {
        Objects.requireNonNull(destino, "El destino no puede ser nulo");
        Objects.requireNonNull(formato, "El formato no puede ser nulo");

        String nombre = destino.getFileName().toString();
        String extensionEsperada = "." + formato.getExtension();
        Path archivo = nombre.toLowerCase().endsWith(extensionEsperada)
                ? destino
                : destino.resolveSibling(nombre + extensionEsperada);

        Path parent = archivo.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        return archivo;
    }

    private void exportarInventarioCsv(List<Producto> productos, Path archivo) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                archivo, StandardCharsets.UTF_8)) {
            // BOM para que Excel en Windows detecte UTF-8 correctamente.
            writer.write('\ufeff');
            escribirFilaCsv(writer,
                    "SKU", "Nombre", "Código de barras", "Unidad",
                    "Precio de venta", "Stock actual", "Estado");

            for (Producto producto : productos) {
                escribirFilaCsv(writer,
                        producto.getSku(),
                        producto.getNombre(),
                        producto.getCodigoBarras(),
                        producto.getUnidadMedida(),
                        String.valueOf(producto.getPrecioVenta()),
                        String.valueOf(producto.getStockActual()),
                        producto.getEstado());
            }
        }
    }

    private void exportarMovimientosCsv(List<MovimientoInventario> movimientos,
                                        Path archivo) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                archivo, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            escribirFilaCsv(writer,
                    "Tipo", "Fecha/Hora", "SKU", "Producto",
                    "Stock anterior", "Cantidad", "Stock resultante",
                    "ID Usuario", "Usuario");

            for (MovimientoInventario movimiento : movimientos) {
                escribirFilaCsv(writer,
                        movimiento.getTipoMovimiento(),
                        movimiento.getFechaHora() == null
                                ? ""
                                : movimiento.getFechaHora().format(FORMATO_FECHA_HORA),
                        movimiento.getSku(),
                        movimiento.getNombreProducto(),
                        String.valueOf(movimiento.getStockAnterior()),
                        String.valueOf(movimiento.getCantidadAplicada()),
                        String.valueOf(movimiento.getStockResultante()),
                        String.valueOf(movimiento.getIdUsuario()),
                        nombreUsuarioVisible(movimiento));
            }
        }
    }

    private void escribirFilaCsv(BufferedWriter writer, String... valores) throws IOException {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) writer.write(',');
            writer.write(escaparCsv(valores[i]));
        }
        writer.newLine();
    }

    private String escaparCsv(String valor) {
        String texto = valor == null ? "" : valor;
        boolean requiereComillas = texto.indexOf(',') >= 0
                || texto.indexOf('"') >= 0
                || texto.indexOf('\n') >= 0
                || texto.indexOf('\r') >= 0;
        if (!requiereComillas) return texto;
        return '"' + texto.replace("\"", "\"\"") + '"';
    }

    private void exportarInventarioExcel(List<Producto> productos, Path archivo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inventario");
            CellStyle encabezado = crearEstiloEncabezado(workbook);
            String[] columnas = {
                    "SKU", "Nombre", "Código de barras", "Unidad",
                    "Precio de venta", "Stock actual", "Estado"
            };
            crearEncabezado(sheet, encabezado, columnas);

            int fila = 1;
            for (Producto producto : productos) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(valor(producto.getSku()));
                row.createCell(1).setCellValue(valor(producto.getNombre()));
                row.createCell(2).setCellValue(valor(producto.getCodigoBarras()));
                row.createCell(3).setCellValue(valor(producto.getUnidadMedida()));
                row.createCell(4).setCellValue(producto.getPrecioVenta());
                row.createCell(5).setCellValue(producto.getStockActual());
                row.createCell(6).setCellValue(valor(producto.getEstado()));
            }

            ajustarColumnas(sheet, columnas.length);
            escribirWorkbook(workbook, archivo);
        }
    }

    private void exportarMovimientosExcel(List<MovimientoInventario> movimientos,
                                          Path archivo) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Movimientos");
            CellStyle encabezado = crearEstiloEncabezado(workbook);
            String[] columnas = {
                    "Tipo", "Fecha/Hora", "SKU", "Producto",
                    "Stock anterior", "Cantidad", "Stock resultante",
                    "ID Usuario", "Usuario"
            };
            crearEncabezado(sheet, encabezado, columnas);

            int fila = 1;
            for (MovimientoInventario movimiento : movimientos) {
                Row row = sheet.createRow(fila++);
                row.createCell(0).setCellValue(valor(movimiento.getTipoMovimiento()));
                row.createCell(1).setCellValue(movimiento.getFechaHora() == null
                        ? ""
                        : movimiento.getFechaHora().format(FORMATO_FECHA_HORA));
                row.createCell(2).setCellValue(valor(movimiento.getSku()));
                row.createCell(3).setCellValue(valor(movimiento.getNombreProducto()));
                row.createCell(4).setCellValue(movimiento.getStockAnterior());
                row.createCell(5).setCellValue(movimiento.getCantidadAplicada());
                row.createCell(6).setCellValue(movimiento.getStockResultante());
                row.createCell(7).setCellValue(movimiento.getIdUsuario());
                row.createCell(8).setCellValue(nombreUsuarioVisible(movimiento));
            }

            ajustarColumnas(sheet, columnas.length);
            escribirWorkbook(workbook, archivo);
        }
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void crearEncabezado(Sheet sheet, CellStyle estilo, String[] columnas) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            row.createCell(i).setCellValue(columnas[i]);
            row.getCell(i).setCellStyle(estilo);
        }
        sheet.createFreezePane(0, 1);
    }

    private void ajustarColumnas(Sheet sheet, int cantidadColumnas) {
        for (int i = 0; i < cantidadColumnas; i++) {
            sheet.autoSizeColumn(i);
            int anchoActual = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(anchoActual + 768, 12000));
        }
    }

    private void escribirWorkbook(Workbook workbook, Path archivo) throws IOException {
        try (OutputStream out = Files.newOutputStream(archivo)) {
            workbook.write(out);
        }
    }

    private String nombreUsuarioVisible(MovimientoInventario movimiento) {
        String nombre = valor(movimiento.getNombreUsuario()).trim();
        String username = valor(movimiento.getUsernameUsuario()).trim();
        if (nombre.isEmpty()) return username;
        if (!username.isEmpty() && !username.equals(nombre)) {
            return nombre + " (" + username + ")";
        }
        return nombre;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
