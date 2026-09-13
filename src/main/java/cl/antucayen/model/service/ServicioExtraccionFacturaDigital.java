package cl.antucayen.model.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Extracción local de ítems desde PDF/JPG/PNG. Primero intenta texto embebido
 * en PDF y, cuando no existe, aplica OCR. El resultado nunca modifica stock:
 * solo alimenta la etapa de revisión/procesamiento de la factura.
 */
public class ServicioExtraccionFacturaDigital {

    public record ItemExtraido(String codigoInterno, String descripcion,
                               int cantidad, String estado) {}

    private static final Set<String> CABECERAS = Set.of(
            "CODIGO", "CÓDIGO", "SKU", "ITEM", "ÍTEM", "PRODUCTO", "DESCRIPCION",
            "DESCRIPCIÓN", "CANTIDAD", "CANT", "FACTURA", "TOTAL", "SUBTOTAL", "RUT",
            "FECHA", "NETO", "IVA"
    );

    private final ServicioArchivoFactura archivos = new ServicioArchivoFactura();

    public List<ItemExtraido> extraer(File archivo) throws IOException {
        archivos.validar(archivo);
        String ext = extension(archivo.getName());
        String texto;
        if ("pdf".equals(ext)) {
            texto = extraerPdf(archivo);
        } else {
            texto = ejecutarOcr(archivo);
        }

        List<ItemExtraido> items = parsearLineas(texto);
        if (items.isEmpty()) {
            items.add(new ItemExtraido(null,
                    "No fue posible extraer automáticamente los ítems; revisar e ingresar manualmente",
                    0, "No Procesado"));
        }
        return items;
    }

    public BufferedImage renderizarVistaPrevia(File archivo) throws IOException {
        archivos.validar(archivo);
        String ext = extension(archivo.getName());
        if ("pdf".equals(ext)) {
            try (PDDocument doc = Loader.loadPDF(archivo)) {
                if (doc.getNumberOfPages() == 0) {
                    throw new IOException("El PDF no contiene páginas");
                }
                return new PDFRenderer(doc).renderImageWithDPI(0, 120);
            }
        }
        BufferedImage imagen = ImageIO.read(archivo);
        if (imagen == null) throw new IOException("No se pudo interpretar la imagen seleccionada");
        return imagen;
    }

    private String extraerPdf(File archivo) throws IOException {
        try (PDDocument doc = Loader.loadPDF(archivo)) {
            String texto = new PDFTextStripper().getText(doc);
            if (!parsearLineas(texto).isEmpty()) return texto;

            // PDF escaneado: OCR de cada página. Se limita al documento cargado
            // y concatena las páginas antes de la etapa de parsing.
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder ocr = new StringBuilder();
            Tesseract motor = crearTesseract();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage pagina = renderer.renderImageWithDPI(i, 180);
                try {
                    ocr.append(motor.doOCR(pagina)).append('\n');
                } catch (TesseractException ex) {
                    throw new IOException("No fue posible aplicar OCR al PDF: " + ex.getMessage(), ex);
                }
            }
            return ocr.toString();
        }
    }

    private String ejecutarOcr(File archivo) throws IOException {
        try {
            return crearTesseract().doOCR(archivo);
        } catch (TesseractException ex) {
            throw new IOException(
                    "No fue posible leer la imagen con OCR. Verifica la configuración de Tesseract/tessdata: "
                            + ex.getMessage(), ex);
        }
    }

    private Tesseract crearTesseract() {
        Tesseract tesseract = new Tesseract();
        String datapath = System.getenv("ANTUCAYEN_TESSDATA");
        if (datapath != null && !datapath.isBlank()) tesseract.setDatapath(datapath.trim());
        String idioma = System.getenv("ANTUCAYEN_OCR_LANG");
        tesseract.setLanguage(idioma == null || idioma.isBlank() ? "eng" : idioma.trim());
        tesseract.setPageSegMode(6);
        return tesseract;
    }

    /**
     * Parser conservador: interpreta el primer token como código del proveedor y
     * el primer entero posterior como cantidad. Las líneas no confiables no se
     * convierten en movimientos; el usuario puede corregirlas en la grilla.
     */
    List<ItemExtraido> parsearLineas(String texto) {
        List<ItemExtraido> resultado = new ArrayList<>();
        if (texto == null || texto.isBlank()) return resultado;

        for (String lineaOriginal : texto.split("\\R")) {
            String linea = lineaOriginal.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
            if (linea.length() < 3) continue;
            String[] tokens = linea.split(" ");
            if (tokens.length < 2) continue;

            String codigo = limpiarCodigo(tokens[0]);
            if (!esCodigoPlausible(codigo)) continue;
            if (CABECERAS.contains(codigo.toUpperCase(Locale.ROOT))) continue;

            int indiceCantidad = detectarIndiceCantidad(tokens);
            if (indiceCantidad < 0) continue;
            int cantidad;
            try {
                cantidad = Integer.parseInt(limpiarEntero(tokens[indiceCantidad]));
            } catch (NumberFormatException ex) {
                continue;
            }

            StringBuilder descripcion = new StringBuilder();
            for (int i = 1; i < tokens.length; i++) {
                if (i == indiceCantidad) continue;
                if (descripcion.length() > 0) descripcion.append(' ');
                descripcion.append(tokens[i]);
            }
            resultado.add(new ItemExtraido(codigo,
                    descripcion.toString().isBlank() ? null : descripcion.toString(),
                    cantidad, "Observado"));
        }
        return resultado;
    }

    private int detectarIndiceCantidad(String[] tokens) {
        if (tokens.length < 2) return -1;

        // Formato común: CODIGO CANTIDAD DESCRIPCION ...
        if (esEnteroPositivo(tokens[1])) return 1;

        // Formato común: CODIGO DESCRIPCION CANTIDAD PRECIO SUBTOTAL.
        // Cuando las tres últimas columnas son numéricas, la primera de ellas
        // corresponde a cantidad; evita confundir pesos/volúmenes de la descripción.
        if (tokens.length >= 4
                && esNumero(tokens[tokens.length - 1])
                && esNumero(tokens[tokens.length - 2])
                && esEnteroPositivo(tokens[tokens.length - 3])) {
            return tokens.length - 3;
        }

        // Formato: CODIGO DESCRIPCION CANTIDAD. Se toma el último entero positivo.
        for (int i = tokens.length - 1; i >= 1; i--) {
            if (esEnteroPositivo(tokens[i])) return i;
        }
        return -1;
    }

    private boolean esEnteroPositivo(String token) {
        String limpio = limpiarEntero(token);
        if (!limpio.matches("\\d{1,7}")) return false;
        try {
            int valor = Integer.parseInt(limpio);
            return valor > 0 && valor <= 1_000_000;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean esNumero(String token) {
        if (token == null) return false;
        return token.replaceAll("[^0-9,.-]", "").matches("-?\\d+(?:[.,]\\d+)?");
    }

    private String limpiarEntero(String token) {
        return token == null ? "" : token.replaceAll("[^0-9]", "");
    }

    private boolean esCodigoPlausible(String codigo) {
        if (codigo == null || codigo.length() < 2 || codigo.length() > 50) return false;
        return codigo.matches("[A-Za-z0-9][A-Za-z0-9._/-]*");
    }

    private String limpiarCodigo(String token) {
        if (token == null) return null;
        return token.replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9._/-]+$", "");
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
