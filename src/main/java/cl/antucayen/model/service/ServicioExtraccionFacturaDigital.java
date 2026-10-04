package cl.antucayen.model.service;

import cl.antucayen.model.domain.EstadoItemFactura;
import cl.antucayen.model.dto.ItemFacturaExtraido;

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
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracción local de datos de una factura desde PDF/JPG/PNG.
 *
 * <p>El servicio intenta primero aprovechar texto embebido en PDFs y usa OCR
 * cuando el documento es una imagen o cuando el texto embebido no entrega una
 * lectura suficientemente útil. Además de los ítems, extrae el folio/número de
 * factura, la fecha de emisión y el total para evitar digitación manual cuando el documento es
 * legible.</p>
 *
 * <p>La extracción es deliberadamente conservadora: ningún dato extraído se
 * considera definitivo hasta que el usuario lo revisa en el formulario.</p>
 */
public class ServicioExtraccionFacturaDigital {

    public record ResultadoExtraccion(String numeroFactura,
                                      LocalDate fechaEmision,
                                      Integer valorTotal,
                                      List<ItemFacturaExtraido> items,
                                      String textoExtraido) {
        public ResultadoExtraccion {
            items = items == null ? List.of() : List.copyOf(items);
            textoExtraido = textoExtraido == null ? "" : textoExtraido;
        }
    }

    private record NumeroToken(int indice, long valor, boolean entero) {
    }

    private record TriplePrecio(NumeroToken cantidad,
                                NumeroToken precioUnitario,
                                NumeroToken subtotal,
                                double errorRelativo) {
    }

    private static final Set<String> CABECERAS = Set.of(
            "CODIGO", "CÓDIGO", "SKU", "ITEM", "ÍTEM", "PRODUCTO", "DESCRIPCION",
            "DESCRIPCIÓN", "CANTIDAD", "CANT", "FACTURA", "TOTAL", "SUBTOTAL", "RUT",
            "FECHA", "NETO", "IVA", "PRECIO", "UNITARIO", "P.UNIT", "P.UNITARIO", "VALOR"
    );

    /** Términos usados para detectar la cabecera real de la tabla de detalle. */
    private static final Set<String> CABECERAS_DETALLE = Set.of(
            "CODIGO", "CÓDIGO", "SKU", "ITEM", "ÍTEM", "PRODUCTO", "DESCRIPCION",
            "DESCRIPCIÓN", "CANTIDAD", "CANT", "PRECIO", "UNITARIO", "P.UNIT",
            "P.UNITARIO", "VALOR", "SUBTOTAL", "TOTAL"
    );

    /** Prefijos administrativos que no corresponden a líneas de compra. */
    private static final List<String> PREFIJOS_METADATA = List.of(
            "FACTURA", "FOLIO", "RUT", "RAZON SOCIAL", "GIRO", "DIRECCION",
            "DOMICILIO", "COMUNA", "CIUDAD", "TELEFONO", "FONO", "CELULAR",
            "EMAIL", "CORREO", "FECHA EMISION", "FECHA DE EMISION", "VENCIMIENTO",
            "FORMA DE PAGO", "CONDICION DE PAGO", "ORDEN DE COMPRA", "GUIA DE DESPACHO",
            "SEÑOR(ES)", "SENOR(ES)", "CLIENTE", "VENDEDOR", "OBSERVACIONES",
            "TIMBRE ELECTRONICO", "SII.CL", "WWW."
    );

    private static final List<Pattern> PATRONES_FOLIO = List.of(
            Pattern.compile("(?im)\\bFOLIO\\s*(?:N(?:RO|ÚMERO)?\\.?|N[°ºo]?)?\\s*[:#-]?\\s*([A-Z0-9-]{2,30})\\b"),
            Pattern.compile("(?im)\\bFACTURA(?:\\s+ELECTR[ÓO]NICA)?\\s*(?:N(?:RO|ÚMERO)?\\.?|N[°ºo#]?)?\\s*[:#-]?\\s*([0-9]{1,20})\\b"),
            Pattern.compile("(?im)\\bN[°º]\\s*[:#-]?\\s*([0-9]{1,20})\\b")
    );

    /** Patrones de fecha de emisión, ordenados desde la etiqueta más específica al fallback "FECHA". */
    private static final List<Pattern> PATRONES_FECHA_EMISION = List.of(
            Pattern.compile("(?im)^\\s*FECHA\\s+DE\\s+EMISI[ÓO]N\\s*[:#-]?\\s*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b"),
            Pattern.compile("(?im)^\\s*FECHA\\s+EMISI[ÓO]N\\s*[:#-]?\\s*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b"),
            Pattern.compile("(?im)^\\s*EMISI[ÓO]N\\s*[:#-]?\\s*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b"),
            Pattern.compile("(?im)^\\s*FECHA\\s*[:#-]?\\s*(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b")
    );

    private static final List<DateTimeFormatter> FORMATOS_FECHA_EMISION = List.of(
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/M/d").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu.M.d").withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder()
                    .appendPattern("d-M-")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder()
                    .appendPattern("d/M/")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder()
                    .appendPattern("d.M.")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT)
    );

    private static final Pattern PATRON_TOTAL = Pattern.compile(
            "(?i)\\b(?:MONTO\\s+)?TOTAL(?:\\s+A\\s+PAGAR)?\\b");

    private static final Pattern PATRON_MONTO = Pattern.compile(
            "\\$?\\s*([0-9]{1,3}(?:[.\\s][0-9]{3})+(?:,[0-9]{1,2})?|[0-9]{2,12}(?:,[0-9]{1,2})?)");

    /** RUT chileno con o sin puntos; nunca debe convertirse en código de producto. */
    private static final Pattern PATRON_RUT_CHILENO = Pattern.compile(
            "(?i)(?<![0-9])(?:[0-9]{1,2}(?:\\.[0-9]{3}){2}|[0-9]{7,8})-[0-9K](?![A-Z0-9])");

    /** Líneas que marcan el término del detalle y el comienzo del resumen financiero. */
    private static final Pattern PATRON_RESUMEN_FINANCIERO = Pattern.compile(
            "^(?:SUBTOTAL|NETO|MONTO NETO|TOTAL NETO|IVA(?: [0-9]{1,2}%?)?|IMPUESTO|"
                    + "TOTAL A PAGAR|MONTO TOTAL|TOTAL EXENTO|EXENTO|DESCUENTO|RECARGO)"
                    + "(?:\\s|:|\\$|$).*");

    private static final Pattern PATRON_PERSONA_JURIDICA = Pattern.compile(
            "(?i)\\b(?:LTDA\\.?|LIMITADA|S\\.?P\\.?A\\.?|SPA|S\\.?A\\.?|SOCIEDAD|"
                    + "COMERCIAL|DISTRIBUIDORA|IMPORTADORA|EXPORTADORA|EMPRESA)\\b");

    private static final Pattern PATRON_CONTACTO = Pattern.compile(
            "(?i)(?:[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}|https?://|www\\.)");

    private static final Pattern PATRON_FECHA = Pattern.compile(
            "(?<![0-9])(?:[0-3]?[0-9][/-][01]?[0-9][/-](?:19|20)?[0-9]{2})(?![0-9])");

    /** Valores con aspecto de monto no deben usarse como SKU/código interno. */
    private static final Pattern PATRON_MONTO_SOLO = Pattern.compile(
            "^[0-9]{1,3}(?:[.,][0-9]{3})+(?:,[0-9]{1,2})?$");

    private final ServicioArchivoFactura archivos = new ServicioArchivoFactura();


    /** Extrae folio, total e ítems desde el documento digital. */
    public ResultadoExtraccion analizar(File archivo) throws IOException {
        archivos.validar(archivo);
        String ext = extension(archivo.getName());
        String texto;
        if ("pdf".equals(ext)) {
            texto = extraerMejorTextoPdf(archivo);
        } else {
            texto = ejecutarOcr(archivo);
        }

        ResultadoExtraccion resultado = analizarTexto(texto);
        if (resultado.items().isEmpty()) {
            List<ItemFacturaExtraido> placeholder = List.of(new ItemFacturaExtraido(
                    null,
                    "No fue posible extraer automáticamente los ítems; revisar e ingresar manualmente",
                    0,
                    0,
                    EstadoItemFactura.NO_PROCESADO.valorDb()));
            return new ResultadoExtraccion(
                    resultado.numeroFactura(),
                    resultado.fechaEmision(),
                    resultado.valorTotal(),
                    placeholder,
                    resultado.textoExtraido());
        }
        return resultado;
    }

    /** Visible para pruebas unitarias del parser sin depender de OCR/Tesseract. */
    ResultadoExtraccion analizarTexto(String texto) {
        String numero = extraerNumeroFactura(texto);
        LocalDate fechaEmision = extraerFechaEmision(texto);
        Integer total = extraerValorTotal(texto);
        List<ItemFacturaExtraido> items = parsearLineas(texto);
        return new ResultadoExtraccion(numero, fechaEmision, total, items, texto);
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
        if (imagen == null) {
            throw new IOException("No se pudo interpretar la imagen seleccionada");
        }
        return imagen;
    }

    /**
     * Para PDF con texto embebido se evita OCR innecesario. Si el texto
     * embebido es pobre, se ejecuta OCR y se elige la fuente que produjo más
     * información útil (ítems + folio + fecha de emisión + total).
     */
    private String extraerMejorTextoPdf(File archivo) throws IOException {
        try (PDDocument doc = Loader.loadPDF(archivo)) {
            String embebido = new PDFTextStripper().getText(doc);
            ResultadoExtraccion resultadoEmbebido = analizarTexto(embebido);

            boolean textoSuficiente = embebido != null && embebido.strip().length() >= 80;
            boolean datosSuficientes = !resultadoEmbebido.items().isEmpty()
                    && (resultadoEmbebido.numeroFactura() != null
                    || resultadoEmbebido.valorTotal() != null);

            if (textoSuficiente && datosSuficientes) {
                return embebido;
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder ocr = new StringBuilder();
            Tesseract motor = crearTesseract();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage pagina = renderer.renderImageWithDPI(i, 220);
                try {
                    ocr.append(motor.doOCR(pagina)).append('\n');
                } catch (TesseractException ex) {
                    if (embebido != null && !embebido.isBlank()) {
                        return embebido;
                    }
                    throw new IOException("No fue posible aplicar OCR al PDF: " + ex.getMessage(), ex);
                }
            }

            String textoOcr = ocr.toString();
            ResultadoExtraccion resultadoOcr = analizarTexto(textoOcr);
            return puntaje(resultadoOcr, textoOcr) > puntaje(resultadoEmbebido, embebido)
                    ? textoOcr
                    : embebido;
        }
    }

    private int puntaje(ResultadoExtraccion resultado, String texto) {
        int puntaje = resultado.items().size() * 10;
        if (resultado.numeroFactura() != null) puntaje += 4;
        if (resultado.fechaEmision() != null) puntaje += 4;
        if (resultado.valorTotal() != null) puntaje += 4;
        if (texto != null && texto.strip().length() > 80) puntaje += 1;
        return puntaje;
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
        if (datapath != null && !datapath.isBlank()) {
            tesseract.setDatapath(datapath.trim());
        }
        String idioma = System.getenv("ANTUCAYEN_OCR_LANG");
        tesseract.setLanguage(idioma == null || idioma.isBlank() ? "eng" : idioma.trim());

        int psm = 3; // automático: más tolerante con tablas/documentos que PSM 6.
        String psmEntorno = System.getenv("ANTUCAYEN_OCR_PSM");
        if (psmEntorno != null && psmEntorno.matches("\\d+")) {
            try {
                psm = Integer.parseInt(psmEntorno);
            } catch (NumberFormatException ignored) {
                psm = 3;
            }
        }
        tesseract.setPageSegMode(psm);
        return tesseract;
    }

    /** Parser de folio/número de factura. */
    String extraerNumeroFactura(String texto) {
        if (texto == null || texto.isBlank()) return null;
        for (Pattern patron : PATRONES_FOLIO) {
            Matcher matcher = patron.matcher(texto);
            if (matcher.find()) {
                String numero = matcher.group(1);
                if (numero != null && !numero.isBlank()) {
                    return numero.trim();
                }
            }
        }
        return null;
    }

    /**
     * Extrae la fecha de emisión evitando confundirla con vencimiento u otras
     * fechas administrativas. Primero se buscan etiquetas explícitas como
     * "FECHA DE EMISIÓN" y solo después se acepta el fallback "FECHA".
     *
     * <p>Se admiten dd-MM-aaaa, dd/MM/aaaa, dd.MM.aaaa, ISO aaaa-MM-dd y
     * variantes con año de dos dígitos. Si la fecha es inválida se ignora para
     * que el usuario pueda corregirla manualmente en el formulario.</p>
     */
    LocalDate extraerFechaEmision(String texto) {
        if (texto == null || texto.isBlank()) return null;

        for (Pattern patron : PATRONES_FECHA_EMISION) {
            Matcher matcher = patron.matcher(texto);
            while (matcher.find()) {
                String valor = matcher.group(1);
                LocalDate fecha = parsearFechaEmision(valor);
                if (fecha != null) {
                    return fecha;
                }
            }
        }
        return null;
    }

    private LocalDate parsearFechaEmision(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String fecha = valor.trim();
        for (DateTimeFormatter formato : FORMATOS_FECHA_EMISION) {
            try {
                return LocalDate.parse(fecha, formato);
            } catch (DateTimeParseException ignored) {
                // Se prueba el siguiente formato.
            }
        }
        return null;
    }

    /**
     * Busca desde el final del documento una línea TOTAL/MONTO TOTAL y toma el
     * último valor monetario de esa línea. Se excluyen SUBTOTAL/NETO/IVA.
     */
    Integer extraerValorTotal(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String[] lineas = texto.split("\\R");
        for (int i = lineas.length - 1; i >= 0; i--) {
            String linea = limpiarEspacios(lineas[i]);
            if (linea.isBlank()) continue;
            String normalizada = normalizarComparacion(linea);
            if (normalizada.contains("SUBTOTAL")
                    || normalizada.contains("TOTAL NETO")
                    || normalizada.contains("MONTO NETO")
                    || normalizada.matches(".*\\bIVA\\b.*")) {
                continue;
            }
            if (!PATRON_TOTAL.matcher(linea).find()) continue;

            Matcher matcher = PATRON_MONTO.matcher(linea);
            Integer ultimo = null;
            while (matcher.find()) {
                Integer monto = parseMontoClp(matcher.group(1));
                if (monto != null) ultimo = monto;
            }
            if (ultimo != null && ultimo >= 0) return ultimo;
        }
        return null;
    }

    /**
     * Parser de ítems con heurísticas para formatos comunes.
     *
     * <p>La estrategia es deliberadamente conservadora:</p>
     * <ol>
     *   <li>Si se detecta una cabecera de detalle, solo se analizan las líneas
     *       posteriores a esa cabecera y anteriores al resumen financiero.</li>
     *   <li>Se descartan RUT, razón social, direcciones, datos de contacto,
     *       folio/fecha y totales antes de intentar construir un ítem.</li>
     *   <li>Fuera de una tabla de detalle reconocida se exige una señal fuerte
     *       de código de producto para evitar convertir metadatos en compras.</li>
     * </ol>
     */
    List<ItemFacturaExtraido> parsearLineas(String texto) {
        List<ItemFacturaExtraido> resultado = new ArrayList<>();
        if (texto == null || texto.isBlank()) return resultado;

        List<String> lineas = new ArrayList<>();
        for (String lineaOriginal : texto.split("\\R")) {
            String linea = limpiarEspacios(lineaOriginal.replace('|', ' '));
            if (!linea.isBlank()) lineas.add(linea);
        }
        if (lineas.isEmpty()) return resultado;

        int indiceCabecera = encontrarCabeceraDetalle(lineas);
        boolean seccionDetalleDetectada = indiceCabecera >= 0;
        int inicio = seccionDetalleDetectada ? indiceCabecera + 1 : 0;
        int fin = lineas.size();

        if (seccionDetalleDetectada) {
            for (int i = inicio; i < lineas.size(); i++) {
                if (esInicioResumenFinanciero(lineas.get(i))) {
                    fin = i;
                    break;
                }
            }
        }

        for (int i = inicio; i < fin; i++) {
            String linea = lineas.get(i);
            if (linea.length() < 3 || esCabeceraDetalle(linea)) continue;

            String[] tokens = linea.split(" ");
            if (tokens.length < 2) continue;

            int indiceCodigo = detectarIndiceCodigo(tokens);
            if (indiceCodigo < 0) continue;

            String codigo = limpiarCodigo(tokens[indiceCodigo]);
            if (!esCodigoPlausible(codigo)) continue;

            boolean codigoFuerte = esCodigoProductoFuerte(codigo);
            if (esLineaMetadata(linea, codigo, codigoFuerte)) continue;

            List<NumeroToken> numeros = detectarNumeros(tokens, indiceCodigo + 1);
            NumeroToken cantidad;
            NumeroToken precioUnitario = null;

            TriplePrecio triple = detectarTripleCantidadPrecioSubtotal(numeros);
            if (triple != null) {
                cantidad = triple.cantidad();
                precioUnitario = triple.precioUnitario();
            } else {
                cantidad = detectarCantidad(tokens, indiceCodigo, numeros);
                if (cantidad != null) {
                    precioUnitario = inferirPrecioUnitario(numeros, cantidad);
                }
            }

            if (cantidad == null || cantidad.valor() <= 0 || cantidad.valor() > Integer.MAX_VALUE) {
                // RF-20: una fila del detalle que parece realmente un producto
                // pero cuya cantidad no pudo leerse se conserva como No Procesado.
                // Fuera de una sección de detalle no se crea el placeholder para
                // evitar falsos positivos como RUT, razón social o folios.
                if (seccionDetalleDetectada
                        && esCodigoConIdentidadDeProducto(codigo)
                        && !esLineaMetadata(linea, codigo, codigoFuerte)) {
                    resultado.add(new ItemFacturaExtraido(
                            codigo,
                            descripcionNoProcesada(linea, codigo),
                            0,
                            0,
                            EstadoItemFactura.NO_PROCESADO.valorDb()));
                }
                continue;
            }

            if (!esFilaItemConfiable(
                    codigo,
                    indiceCodigo,
                    cantidad,
                    precioUnitario,
                    triple,
                    seccionDetalleDetectada)) {
                continue;
            }

            int cantidadEntera = (int) cantidad.valor();
            int precio = precioUnitario != null
                    && precioUnitario.valor() >= 0
                    && precioUnitario.valor() <= Integer.MAX_VALUE
                    ? (int) precioUnitario.valor()
                    : 0;

            String descripcion = construirDescripcion(
                    tokens,
                    indiceCodigo,
                    cantidad.indice(),
                    precioUnitario == null ? -1 : precioUnitario.indice());

            resultado.add(new ItemFacturaExtraido(
                    codigo,
                    descripcion.isBlank() ? null : descripcion,
                    cantidadEntera,
                    precio,
                    EstadoItemFactura.OBSERVADO.valorDb()));
        }
        return deduplicarItems(resultado);
    }

    private int encontrarCabeceraDetalle(List<String> lineas) {
        for (int i = 0; i < lineas.size(); i++) {
            if (esCabeceraDetalle(lineas.get(i))) return i;
        }
        return -1;
    }

    private boolean esCabeceraDetalle(String linea) {
        String normalizada = normalizarComparacion(linea);
        if (normalizada.isBlank()) return false;

        int coincidencias = 0;
        boolean identificador = false;
        boolean datosDetalle = false;

        for (String token : normalizada.split(" ")) {
            String limpio = limpiarCodigo(token);
            if (limpio == null || limpio.isBlank()) continue;
            limpio = normalizarComparacion(limpio);
            if (!CABECERAS_DETALLE.contains(limpio)) continue;

            coincidencias++;
            if (Set.of("CODIGO", "SKU", "ITEM", "PRODUCTO").contains(limpio)) {
                identificador = true;
            }
            if (Set.of("DESCRIPCION", "CANTIDAD", "CANT", "PRECIO", "UNITARIO",
                    "P.UNIT", "P.UNITARIO", "VALOR", "SUBTOTAL", "TOTAL").contains(limpio)) {
                datosDetalle = true;
            }
        }

        return coincidencias >= 2
                && datosDetalle
                && (identificador || coincidencias >= 3);
    }

    private boolean esInicioResumenFinanciero(String linea) {
        String normalizada = normalizarComparacion(linea);
        if (normalizada.isBlank()) return false;
        if (PATRON_RESUMEN_FINANCIERO.matcher(normalizada).matches()) return true;
        return normalizada.matches("^[0-9]{1,2}%?\\s+IVA(?:\\s|:|$).*");
    }

    private boolean esLineaMetadata(String linea, String codigo, boolean codigoFuerte) {
        String normalizada = normalizarComparacion(linea);
        if (normalizada.isBlank()) return true;
        if (esCabeceraDetalle(linea) || esInicioResumenFinanciero(linea)) return true;

        if (codigo != null && esRutChileno(codigo)) return true;

        for (String prefijo : PREFIJOS_METADATA) {
            String p = normalizarComparacion(prefijo);
            if (normalizada.equals(p)
                    || normalizada.startsWith(p + " ")
                    || normalizada.startsWith(p + ":")
                    || normalizada.startsWith(p + "-")) {
                return true;
            }
        }

        if (PATRON_CONTACTO.matcher(linea).find()) return true;

        // RUT sin la palabra "RUT", por ejemplo: "76.123.456-7 Comercial San Pedro Ltda.".
        // Si una fila de producto tiene un código fuerte y por casualidad incluye
        // un RUT en su descripción no se descarta automáticamente.
        if (!codigoFuerte && PATRON_RUT_CHILENO.matcher(linea).find()) return true;

        // Razones sociales que vienen sin etiqueta: "COMERCIAL SAN PEDRO LTDA. 1234".
        if (!codigoFuerte && PATRON_PERSONA_JURIDICA.matcher(linea).find()) return true;

        // Fechas administrativas sin etiqueta. Una fila con código de producto
        // fuerte puede contener una fecha (por ejemplo lote/vencimiento), por eso
        // solo se descarta automáticamente cuando no hay identidad de producto.
        if (!codigoFuerte && PATRON_FECHA.matcher(linea).find()) return true;

        return false;
    }

    private boolean esFilaItemConfiable(String codigo,
                                        int indiceCodigo,
                                        NumeroToken cantidad,
                                        NumeroToken precioUnitario,
                                        TriplePrecio triple,
                                        boolean seccionDetalleDetectada) {
        if (codigo == null || cantidad == null || esRutChileno(codigo)) return false;

        // Una relación cantidad * precio = subtotal es evidencia muy fuerte,
        // incluso para códigos puramente numéricos.
        if (triple != null) return true;

        // Dentro de una tabla de detalle ya identificada, un código plausible +
        // cantidad es suficiente; el resto de metadatos quedó fuera por límites.
        if (seccionDetalleDetectada) return true;

        // Sin cabecera reconocida se exige una identidad de producto fuerte y
        // una estructura típica: cantidad inmediata o precio posterior.
        return esCodigoProductoFuerte(codigo)
                && (cantidad.indice() == indiceCodigo + 1 || precioUnitario != null);
    }

    private boolean esCodigoProductoFuerte(String codigo) {
        if (!esCodigoPlausible(codigo) || esRutChileno(codigo)) return false;
        if (PATRON_FECHA.matcher(codigo).matches()) return false;

        boolean tieneLetra = codigo.matches(".*[A-Za-z].*");
        boolean tieneDigito = codigo.matches(".*[0-9].*");
        boolean tieneSeparador = codigo.matches(".*[-_/].*");

        return (tieneLetra && tieneDigito)
                || (tieneSeparador && (tieneLetra || tieneDigito));
    }

    private boolean esCodigoConIdentidadDeProducto(String codigo) {
        if (!esCodigoPlausible(codigo) || esRutChileno(codigo)) return false;
        if (esCodigoProductoFuerte(codigo)) return true;

        /*
         * Para crear un placeholder "No Procesado" exigimos una identidad
         * de producto más fuerte que para una fila que sí trae cantidad.
         *
         * Un token formado solo por letras (por ejemplo "CODIGOS",
         * "INTERNOS" o "PRODUCTOS") puede ser simplemente el comienzo
         * de una nota explicativa dentro del cuerpo de la factura. Antes se
         * aceptaba como supuesto código y frases como:
         *
         *   "Códigos internos compatibles con las equivalencias precargadas
         *    del proveedor demo."
         *
         * terminaban apareciendo como un ítem con cantidad 0 y estado
         * "No Procesado".
         *
         * Los códigos alfanuméricos/con separadores siguen siendo aceptados
         * por esCodigoProductoFuerte(). Para códigos puramente numéricos se
         * permite una longitud de catálogo razonable.
         */
        return codigo.matches("[0-9]{3,20}");
    }

    private boolean esRutChileno(String valor) {
        if (valor == null || valor.isBlank()) return false;
        return PATRON_RUT_CHILENO.matcher(valor.trim()).matches();
    }

    private int detectarIndiceCodigo(String[] tokens) {
        int limite = Math.min(tokens.length, 3);
        for (int i = 0; i < limite; i++) {
            String candidato = limpiarCodigo(tokens[i]);
            if (!esCodigoPlausible(candidato)) continue;
            if (CABECERAS.contains(candidato.toUpperCase(Locale.ROOT))) continue;
            if (esRutChileno(candidato)) continue;

            // Un número corto al inicio suele ser número de línea, no código.
            if (i == 0 && candidato.matches("\\d{1,2}") && tokens.length > 2) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private List<NumeroToken> detectarNumeros(String[] tokens, int desde) {
        List<NumeroToken> numeros = new ArrayList<>();
        for (int i = Math.max(0, desde); i < tokens.length; i++) {
            NumeroToken numero = convertirNumero(tokens[i], i);
            if (numero != null) numeros.add(numero);
        }
        return numeros;
    }

    private TriplePrecio detectarTripleCantidadPrecioSubtotal(List<NumeroToken> numeros) {
        if (numeros.size() < 3) return null;
        List<TriplePrecio> candidatos = new ArrayList<>();
        for (int i = 0; i < numeros.size() - 2; i++) {
            NumeroToken q = numeros.get(i);
            NumeroToken pu = numeros.get(i + 1);
            NumeroToken sub = numeros.get(i + 2);
            if (!q.entero() || q.valor() <= 0 || q.valor() > 1_000_000) continue;
            if (pu.valor() <= 0 || sub.valor() <= 0) continue;

            double esperado = (double) q.valor() * (double) pu.valor();
            double error = Math.abs(esperado - sub.valor()) / Math.max(1d, sub.valor());
            if (error <= 0.03d || Math.abs(esperado - sub.valor()) <= 2d) {
                candidatos.add(new TriplePrecio(q, pu, sub, error));
            }
        }
        return candidatos.stream()
                .min(Comparator.comparingDouble(TriplePrecio::errorRelativo))
                .orElse(null);
    }

    private NumeroToken detectarCantidad(String[] tokens,
                                         int indiceCodigo,
                                         List<NumeroToken> numeros) {
        int siguiente = indiceCodigo + 1;
        if (siguiente < tokens.length) {
            NumeroToken inmediato = convertirNumero(tokens[siguiente], siguiente);
            if (esCantidadPlausible(inmediato)) return inmediato;
        }

        List<NumeroToken> enteros = numeros.stream()
                .filter(this::esCantidadPlausible)
                .toList();
        if (enteros.isEmpty()) return null;

        if (numeros.size() >= 3) {
            NumeroToken terceroDesdeFinal = numeros.get(numeros.size() - 3);
            if (esCantidadPlausible(terceroDesdeFinal)) return terceroDesdeFinal;
        }
        if (numeros.size() >= 2) {
            NumeroToken segundoDesdeFinal = numeros.get(numeros.size() - 2);
            if (esCantidadPlausible(segundoDesdeFinal)) return segundoDesdeFinal;
        }
        return enteros.get(enteros.size() - 1);
    }

    private boolean esCantidadPlausible(NumeroToken numero) {
        return numero != null
                && numero.entero()
                && numero.valor() > 0
                && numero.valor() <= 1_000_000;
    }

    private NumeroToken inferirPrecioUnitario(List<NumeroToken> numeros, NumeroToken cantidad) {
        for (NumeroToken numero : numeros) {
            if (numero.indice() > cantidad.indice()
                    && numero.entero()
                    && numero.valor() > 0) {
                return numero;
            }
        }
        return null;
    }

    private NumeroToken convertirNumero(String token, int indice) {
        if (token == null || token.isBlank()) return null;
        String bruto = token.trim().replace("$", "").replace("CLP", "").replace("clp", "");
        bruto = bruto.replaceAll("^[^0-9-]+|[^0-9.,-]+$", "");
        if (bruto.isBlank() || "-".equals(bruto)) return null;
        if (bruto.startsWith("-")) return null;

        boolean entero = true;
        String normalizado;

        if (bruto.matches("\\d+")) {
            normalizado = bruto;
        } else if (bruto.matches("\\d{1,3}(?:[.,]\\d{3})+")) {
            normalizado = bruto.replace(".", "").replace(",", "");
        } else if (bruto.matches("\\d{1,3}(?:\\.\\d{3})+,\\d{1,2}")) {
            normalizado = bruto.substring(0, bruto.lastIndexOf(','))
                    .replace(".", "");
        } else if (bruto.matches("\\d{1,3}(?:,\\d{3})+\\.\\d{1,2}")) {
            normalizado = bruto.substring(0, bruto.lastIndexOf('.'))
                    .replace(",", "");
        } else if (bruto.matches("\\d+[.,]\\d{1,2}")) {
            // Decimal típico de unidad/volumen (1.5 L); no sirve como cantidad entera.
            entero = false;
            normalizado = bruto.substring(0, Math.max(bruto.lastIndexOf('.'), bruto.lastIndexOf(',')));
        } else {
            return null;
        }

        try {
            long valor = Long.parseLong(normalizado);
            return new NumeroToken(indice, valor, entero);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String construirDescripcion(String[] tokens,
                                        int indiceCodigo,
                                        int indiceCantidad,
                                        int indicePrecio) {
        int inicio;
        int fin;

        if (indiceCantidad == indiceCodigo + 1) {
            inicio = indiceCantidad + 1;
            fin = indicePrecio > inicio ? indicePrecio : tokens.length;
        } else {
            inicio = indiceCodigo + 1;
            fin = indiceCantidad;
        }

        if (inicio >= fin || inicio >= tokens.length) return "";
        StringBuilder descripcion = new StringBuilder();
        for (int i = inicio; i < Math.min(fin, tokens.length); i++) {
            if (descripcion.length() > 0) descripcion.append(' ');
            descripcion.append(tokens[i]);
        }
        return descripcion.toString().trim();
    }

    private String descripcionNoProcesada(String linea, String codigo) {
        if (linea == null) return null;
        String descripcion = linea;
        if (codigo != null && descripcion.startsWith(codigo)) {
            descripcion = descripcion.substring(codigo.length()).trim();
        }
        return descripcion.isBlank() ? linea : descripcion;
    }

    private List<ItemFacturaExtraido> deduplicarItems(List<ItemFacturaExtraido> items) {
        List<ItemFacturaExtraido> unicos = new ArrayList<>();
        for (ItemFacturaExtraido item : items) {
            boolean repetido = unicos.stream().anyMatch(existente ->
                    java.util.Objects.equals(existente.codigoInterno(), item.codigoInterno())
                            && existente.cantidad() == item.cantidad()
                            && java.util.Objects.equals(existente.descripcion(), item.descripcion()));
            if (!repetido) unicos.add(item);
        }
        return unicos;
    }

    private Integer parseMontoClp(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String valor = texto.replace("$", "").replace(" ", "").trim();
        if (valor.isBlank()) return null;

        // Chile: punto suele ser miles y coma decimales. Para el modelo actual
        // se almacenan pesos enteros, por lo que se descarta la parte decimal.
        int coma = valor.lastIndexOf(',');
        if (coma >= 0 && valor.length() - coma - 1 <= 2) {
            valor = valor.substring(0, coma);
        }
        valor = valor.replace(".", "").replace(",", "");
        valor = valor.replaceAll("[^0-9]", "");
        if (valor.isBlank()) return null;
        try {
            long monto = Long.parseLong(valor);
            return monto <= Integer.MAX_VALUE ? (int) monto : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean esCodigoPlausible(String codigo) {
        if (codigo == null || codigo.length() < 2 || codigo.length() > 50) return false;
        if (!codigo.matches("[A-Za-z0-9][A-Za-z0-9._/-]*")) return false;
        if (codigo.matches("\\d{1,2}")) return false;
        if (PATRON_RUT_CHILENO.matcher(codigo).matches()) return false;
        if (PATRON_FECHA.matcher(codigo).matches()) return false;
        // Evita interpretar un monto como 25.000 o 1,250,000 como código.
        if (PATRON_MONTO_SOLO.matcher(codigo).matches()) return false;
        return true;
    }

    private String limpiarCodigo(String token) {
        if (token == null) return null;
        return token.replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9._/-]+$", "");
    }

    private String limpiarEspacios(String texto) {
        return texto == null
                ? ""
                : texto.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
    }

    private String normalizarComparacion(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalizado.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
