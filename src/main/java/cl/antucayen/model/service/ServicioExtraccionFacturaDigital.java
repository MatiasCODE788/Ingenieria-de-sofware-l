package cl.antucayen.model.service;

import cl.antucayen.model.domain.EstadoItemFactura;
import cl.antucayen.model.dto.ItemFacturaExtraido;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /** valor: parte entera; exacto: con decimales, por ejemplo 0,6 o 47.723,33. */
    private record NumeroToken(int indice, long valor, boolean entero, double exacto) {
        NumeroToken(int indice, long valor, boolean entero) {
            this(indice, valor, entero, valor);
        }
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
            "FECHA VENCIMIENTO", "FECHA DE VENCIMIENTO", "PAGUE HASTA",
            "FORMA DE PAGO", "CONDICION DE PAGO", "ORDEN DE COMPRA", "GUIA DE DESPACHO",
            "SEÑOR(ES)", "SENOR(ES)", "CLIENTE", "VENDEDOR", "OBSERVACIONES",
            "TIMBRE ELECTRONICO", "SII.CL", "WWW."
    );

    /**
     * El folio siempre tiene dígitos: así «FOLIO REFERENCIA» de la orden de
     * compra no se toma como número de factura. Primero se busca el número
     * junto a FACTURA, luego «NRO:» o «N°» y al final «FOLIO».
     */
    private static final List<Pattern> PATRONES_FOLIO = List.of(
            Pattern.compile("(?imu)\\bFACTURA(?:\\s+ELECTR[ÓO]NICA)?\\s*(?:N(?:RO|ÚMERO)?\\.?|N[°ºo#?'’*]?)?\\s*[:#-]?\\s*([0-9]{1,20})\\b"),
            Pattern.compile("(?imu)\\bN(?:RO|ÚMERO|UMERO)\\.?\\s*[:#-]?\\s*([0-9]{3,20})\\b"),
            Pattern.compile("(?imu)\\bN[°º?'’*]\\s*[:#-]?\\s*([0-9]{1,20})\\b"),
            Pattern.compile("(?imu)\\bFOLIO\\s*(?:N(?:RO|ÚMERO)?\\.?|N[°ºo]?)?\\s*[:#-]?\\s*([A-Z0-9-]*[0-9][A-Z0-9-]*)\\b")
    );

    /** Patrones de fecha de emisión, ordenados desde la etiqueta más específica al fallback "FECHA". */
    private static final List<Pattern> PATRONES_FECHA_EMISION = List.of(
            Pattern.compile("(?imu)\\bFECHA\\s+DE\\s+EMISI[ÓO]N\\s*[:#-]?\\s*(\\d{1,2}\\s+DE\\s+\\p{L}+\\s+DEL?\\s+\\d{4}|\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b"),
            Pattern.compile("(?imu)\\bFECHA\\s+EMISI[ÓO]N\\s*[:#-]?\\s*(\\d{1,2}\\s+DE\\s+\\p{L}+\\s+DEL?\\s+\\d{4}|\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b"),
            Pattern.compile("(?imu)^\\s*EMISI[ÓO]N\\s*[:#-]?\\s*(\\d{1,2}\\s+DE\\s+\\p{L}+\\s+DEL?\\s+\\d{4}|\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b"),
            Pattern.compile("(?imu)^\\s*FECHA\\s*[:#-]?\\s*(\\d{1,2}\\s+DE\\s+\\p{L}+\\s+DEL?\\s+\\d{4}|\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b")
    );

    private static final Pattern PATRON_ETIQUETA_EMISION = Pattern.compile("(?iu)\\bFECHA\\s+(?:DE\\s+)?EMISI[ÓO]N");
    private static final Pattern PATRON_FECHA_SUELTA = Pattern.compile("\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}");

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

    /** Fecha con el mes escrito, por ejemplo "09 de OCTUBRE de 2026". */
    private static final Pattern PATRON_FECHA_TEXTUAL = Pattern.compile(
            "(?i)(?<![0-9])([0-3]?[0-9])\\s+DE\\s+(\\p{L}+)\\s+DEL?\\s+((?:19|20)[0-9]{2})(?![0-9])");

    private static final List<String> MESES = List.of(
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO",
            "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE");

    /**
     * Unidades de medida de facturas con columna UNIDAD, por ejemplo
     * "3587 1 PK 3 Estuches Capuccino 1 PACK 7.179 1.077 6.102".
     */
    private static final Set<String> UNIDADES = Set.of(
            "PACK", "PAQ", "PAQUETE", "PQT", "DSPL", "DISPLAY", "DISP", "TARR", "TARRO",
            "SOBR", "SOBRE", "BARR", "BARRA", "POTE", "UN", "UND", "UNI", "UNID", "UNIDAD",
            "CAJA", "CJ", "CJA", "BOLSA", "BOL", "KG", "KGS", "LT", "LTS", "BOT", "BOTELLA",
            "FCO", "FRASCO", "DOC", "DOCENA", "BLIS", "ROLLO");

    /** Lecturas OCR habituales del número 1 en la columna de cantidad. */
    private static final Pattern PATRON_UNO_OCR = Pattern.compile("(?iu)^(?:[il|!ñ]{1,2}|dl|ay)[.:;,]?$");

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

    /** Pie de la factura: monto en palabras, total de unidades, timbre y leyendas. */
    private static final Pattern PATRON_FIN_DETALLE = Pattern.compile(
            "^(?:SON\\s*:|TOTAL(?:\\s|:|\\$|$)|TOTAL UNIDADES|VALOR NETO|TIMBRE|ESTIMADO CLIENTE|"
                    + "VERIFIQUE|ESTA FACTURA|RES\\.?\\s+\\d)");

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
        ResultadoExtraccion resultado;
        if ("pdf".equals(ext)) {
            resultado = analizarPdf(archivo);
        } else {
            BufferedImage imagen = PreprocesadorImagenFactura.leer(archivo);
            Tesseract motor = crearTesseract();
            try {
                resultado = analizarImagen(imagen, motor);
            } catch (Error ex) {
                throw errorNativoOcr(ex);
            }
        }

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
        return analizarTexto(texto, false);
    }

    ResultadoExtraccion analizarTexto(String texto, boolean desdeOcr) {
        String numero = extraerNumeroFactura(texto);
        LocalDate fechaEmision = extraerFechaEmision(texto);
        Integer total = extraerValorTotal(texto);
        List<ItemFacturaExtraido> items = parsearLineas(texto, desdeOcr);
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
        return PreprocesadorImagenFactura.leer(archivo);
    }

    /**
     * Para PDF con texto embebido se evita OCR innecesario. Si el texto
     * embebido es pobre, por ejemplo en un PDF escaneado, cada página se lee
     * como imagen y se elige la fuente que produjo más información útil
     * (ítems + folio + fecha de emisión + total).
     */
    private ResultadoExtraccion analizarPdf(File archivo) throws IOException {
        try (PDDocument doc = Loader.loadPDF(archivo)) {
            String embebido = new PDFTextStripper().getText(doc);
            ResultadoExtraccion resultadoEmbebido = analizarTexto(embebido);

            boolean textoSuficiente = embebido != null && embebido.strip().length() >= 80;
            boolean datosSuficientes = !resultadoEmbebido.items().isEmpty()
                    && (resultadoEmbebido.numeroFactura() != null
                    || resultadoEmbebido.valorTotal() != null);

            if (textoSuficiente && datosSuficientes) {
                return resultadoEmbebido;
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            boolean hayEmbebido = embebido != null && !embebido.isBlank();
            Tesseract motor;
            try {
                motor = crearTesseract();
            } catch (IOException ex) {
                if (hayEmbebido) return resultadoEmbebido;
                throw ex;
            }
            List<ResultadoExtraccion> paginas = new ArrayList<>();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage pagina = renderer.renderImageWithDPI(i, 220);
                try {
                    paginas.add(analizarImagen(pagina, motor));
                } catch (Error ex) {
                    if (hayEmbebido && !(ex instanceof VirtualMachineError)) {
                        return resultadoEmbebido;
                    }
                    throw errorNativoOcr(ex);
                }
            }

            ResultadoExtraccion resultadoOcr = unirPaginas(paginas);
            return puntaje(resultadoOcr, resultadoOcr.textoExtraido()) > puntaje(resultadoEmbebido, embebido)
                    ? resultadoOcr
                    : resultadoEmbebido;
        }
    }

    private ResultadoExtraccion unirPaginas(List<ResultadoExtraccion> paginas) {
        String numero = null;
        LocalDate fecha = null;
        Integer total = null;
        List<ItemFacturaExtraido> items = new ArrayList<>();
        StringBuilder texto = new StringBuilder();
        for (ResultadoExtraccion pagina : paginas) {
            if (numero == null) numero = pagina.numeroFactura();
            if (fecha == null) fecha = pagina.fechaEmision();
            if (pagina.valorTotal() != null) total = pagina.valorTotal();
            items.addAll(pagina.items());
            texto.append(pagina.textoExtraido()).append('\n');
        }
        return new ResultadoExtraccion(numero, fecha, total, items, texto.toString());
    }

    /** Palabras reconocidas en una lectura OCR y el texto armado con ellas, línea por línea. */
    private record LecturaOcr(List<ReconstructorTablaFactura.Palabra> palabras, String texto) {
    }

    /**
     * Lee una foto o una página escaneada. Primero se hace una lectura de texto
     * disperso, que reconoce mejor los números sueltos de cada columna; si la
     * tabla no queda completa se hace una segunda lectura por líneas y se usa
     * la que cuadre más filas. El detalle se arma con la posición de cada
     * palabra (ReconstructorTablaFactura), no con el orden de las líneas,
     * porque en una foto inclinada una fila de montos queda a la altura de la
     * fila vecina. Si la tabla no se puede reconstruir, se usa el texto.
     */
    private ResultadoExtraccion analizarImagen(BufferedImage imagen, Tesseract motor) {
        BufferedImage preparada = PreprocesadorImagenFactura.preparar(imagen);
        LecturaOcr dispersa = leerPalabras(motor, preparada, 11);
        ReconstructorTablaFactura.Resultado tabla = ReconstructorTablaFactura.reconstruir(dispersa.palabras());
        LecturaOcr porLineas = null;
        if (!tablaCompleta(tabla)) {
            porLineas = leerPalabras(motor, preparada, 6);
            ReconstructorTablaFactura.Resultado otra = ReconstructorTablaFactura.reconstruir(porLineas.palabras());
            if (otra.filasVerificadas() > tabla.filasVerificadas()
                    || (otra.filasVerificadas() == tabla.filasVerificadas() && otra.items().size() > tabla.items().size())) {
                tabla = otra;
            }
        }

        String texto = porLineas != null ? porLineas.texto() : dispersa.texto();
        ResultadoExtraccion porTexto = analizarTexto(texto, true);
        boolean faltanDatos = porTexto.numeroFactura() == null || porTexto.fechaEmision() == null
                || porTexto.valorTotal() == null;
        if (faltanDatos && porLineas == null) {
            // La lectura por líneas suele recuperar el folio, la fecha o el total.
            porLineas = leerPalabras(motor, preparada, 6);
            ResultadoExtraccion otra = analizarTexto(porLineas.texto(), true);
            porTexto = new ResultadoExtraccion(
                    porTexto.numeroFactura() != null ? porTexto.numeroFactura() : otra.numeroFactura(),
                    porTexto.fechaEmision() != null ? porTexto.fechaEmision() : otra.fechaEmision(),
                    porTexto.valorTotal() != null ? porTexto.valorTotal() : otra.valorTotal(),
                    porTexto.items(), texto + "\n" + porLineas.texto());
            texto = porTexto.textoExtraido();
        }
        if (!tabla.confiable()) return porTexto;

        List<ItemFacturaExtraido> items = marcarPorRevisar(tabla, extraerValorNeto(texto));
        return new ResultadoExtraccion(porTexto.numeroFactura(), porTexto.fechaEmision(),
                porTexto.valorTotal(), items, texto);
    }

    private static boolean tablaCompleta(ReconstructorTablaFactura.Resultado tabla) {
        return tabla.confiable() && tabla.filasVerificadas() * 10 >= tabla.items().size() * 8;
    }

    /**
     * Una fila que no cuadra con cantidad × precio = subtotal se marca para
     * revisar, salvo que la suma de todas coincida con el neto de la factura.
     */
    static List<ItemFacturaExtraido> marcarPorRevisar(ReconstructorTablaFactura.Resultado tabla, Integer neto) {
        boolean sumaCuadra = neto != null
                && Math.abs(tabla.sumaSubtotales() - neto) <= Math.max(2, 0.002 * neto);
        List<ItemFacturaExtraido> items = new ArrayList<>();
        for (int i = 0; i < tabla.items().size(); i++) {
            ItemFacturaExtraido item = tabla.items().get(i);
            boolean verificada = i < tabla.verificadas().size() && tabla.verificadas().get(i);
            if (sumaCuadra || verificada || !EstadoItemFactura.OBSERVADO.coincide(item.estado())) {
                items.add(item);
                continue;
            }
            String descripcion = item.descripcion() == null || item.descripcion().isBlank()
                    ? "revisar cantidad y precio en la factura"
                    : item.descripcion() + " · revisar cantidad y precio en la factura";
            items.add(new ItemFacturaExtraido(item.codigoInterno(), descripcion,
                    item.cantidad(), item.precioUnitario(), item.estado()));
        }
        return items;
    }

    private LecturaOcr leerPalabras(Tesseract motor, BufferedImage imagen, int modo) {
        motor.setPageSegMode(modo);
        List<Word> reconocidas = motor.getWords(imagen, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        List<ReconstructorTablaFactura.Palabra> palabras = new ArrayList<>();
        if (reconocidas != null) {
            for (Word w : reconocidas) {
                String texto = w.getText() == null ? "" : w.getText().trim();
                Rectangle caja = w.getBoundingBox();
                if (texto.isEmpty() || caja == null) continue;
                palabras.add(new ReconstructorTablaFactura.Palabra(texto, caja.x, caja.y, caja.width, caja.height));
            }
        }
        return new LecturaOcr(palabras, lineasDesdePalabras(palabras));
    }

    /** Agrupa las palabras por altura, de izquierda a derecha, para leer folio, fecha y totales. */
    static String lineasDesdePalabras(List<ReconstructorTablaFactura.Palabra> palabras) {
        if (palabras.isEmpty()) return "";
        List<ReconstructorTablaFactura.Palabra> ordenadas = new ArrayList<>(palabras);
        ordenadas.sort(Comparator.comparingDouble(ReconstructorTablaFactura.Palabra::centroY));
        double[] altos = ordenadas.stream().mapToDouble(ReconstructorTablaFactura.Palabra::alto).sorted().toArray();
        double alto = Math.max(1, altos[altos.length / 2]);

        StringBuilder texto = new StringBuilder();
        List<ReconstructorTablaFactura.Palabra> linea = new ArrayList<>();
        double centro = 0;
        for (ReconstructorTablaFactura.Palabra p : ordenadas) {
            if (!linea.isEmpty() && Math.abs(p.centroY() - centro) > 0.5 * alto) {
                agregarLinea(texto, linea);
                linea.clear();
            }
            linea.add(p);
            centro = linea.stream().mapToDouble(ReconstructorTablaFactura.Palabra::centroY).average().orElse(p.centroY());
        }
        agregarLinea(texto, linea);
        return texto.toString();
    }

    private static void agregarLinea(StringBuilder texto, List<ReconstructorTablaFactura.Palabra> linea) {
        if (linea.isEmpty()) return;
        linea.sort(Comparator.comparingInt(ReconstructorTablaFactura.Palabra::x));
        for (int i = 0; i < linea.size(); i++) {
            if (i > 0) texto.append(' ');
            texto.append(linea.get(i).texto());
        }
        texto.append('\n');
    }

    private int puntaje(ResultadoExtraccion resultado, String texto) {
        int puntaje = resultado.items().size() * 10;
        if (resultado.numeroFactura() != null) puntaje += 4;
        if (resultado.fechaEmision() != null) puntaje += 4;
        if (resultado.valorTotal() != null) puntaje += 4;
        if (texto != null && texto.strip().length() > 80) puntaje += 1;
        return puntaje;
    }

    /**
     * Traduce los errores de la librería nativa de Tesseract a un mensaje que el
     * usuario pueda resolver. JNA informa con java.lang.Error, por ejemplo
     * "Invalid memory access" cuando faltan los datos de idioma.
     */
    private IOException errorNativoOcr(Error ex) {
        if (ex instanceof VirtualMachineError grave) {
            throw grave;
        }
        if (ex instanceof LinkageError) {
            return new IOException("No se pudo cargar el motor OCR (Tesseract): " + ex.getMessage()
                    + ". En Windows, instala «Microsoft Visual C++ Redistributable 2015-2022 (x64)» "
                    + "o ingresa los ítems manualmente.", ex);
        }
        return new IOException("El motor OCR falló al leer el archivo: " + ex.getMessage()
                + ". Revisa que la carpeta tessdata contenga el idioma configurado "
                + "o ingresa los ítems manualmente.", ex);
    }

    /**
     * Crea el motor OCR. La carpeta de idiomas se busca en este orden: variable
     * ANTUCAYEN_TESSDATA, propiedad -Dantucayen.tessdata (la usa la aplicación
     * instalada), TESSDATA_PREFIX y carpetas «tessdata» junto a la aplicación o
     * en el directorio de trabajo. El idioma es ANTUCAYEN_OCR_LANG,
     * -Dantucayen.ocr.lang o, si no se indica, «spa» cuando está disponible.
     */
    private Tesseract crearTesseract() throws IOException {
        Path datapath = resolverCarpetaTessdata();
        if (datapath == null) {
            throw new IOException("No se encontraron los datos de idioma del OCR (carpeta tessdata). "
                    + "Instala la aplicación con su carpeta tessdata o define ANTUCAYEN_TESSDATA; "
                    + "mientras tanto puedes ingresar los ítems manualmente.");
        }
        String idioma = resolverIdiomaOcr(datapath);
        for (String parte : idioma.split("\\+")) {
            Path modelo = datapath.resolve(parte.trim() + ".traineddata");
            if (!Files.isRegularFile(modelo)) {
                throw new IOException("Falta el archivo de idioma " + modelo
                        + ". Copia " + parte.trim() + ".traineddata en esa carpeta "
                        + "o ingresa los ítems manualmente.");
            }
        }

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(datapath.toString());
        tesseract.setLanguage(idioma);
        // Las fotos no traen resolución; sin este dato Tesseract supone 70 ppp.
        tesseract.setVariable("user_defined_dpi", "300");

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

    /** Carpeta tessdata según la configuración del equipo y la ubicación de la aplicación. */
    static Path resolverCarpetaTessdata() {
        List<Path> candidatos = new ArrayList<>();
        String prefijo = System.getenv("TESSDATA_PREFIX");
        agregarRuta(candidatos, prefijo);
        if (prefijo != null && !prefijo.isBlank()) {
            agregarRuta(candidatos, prefijo.trim() + java.io.File.separator + "tessdata");
        }
        String lanzador = System.getProperty("jpackage.app-path");
        if (lanzador != null && !lanzador.isBlank()) {
            Path base = rutaSegura(lanzador);
            if (base != null && base.getParent() != null) {
                candidatos.add(base.getParent().resolve("app").resolve("tessdata"));
                candidatos.add(base.getParent().resolve("tessdata"));
            }
        }
        try {
            Path ubicacion = Path.of(ServicioExtraccionFacturaDigital.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            if (ubicacion.getParent() != null) {
                candidatos.add(ubicacion.getParent().resolve("tessdata"));
            }
        } catch (Exception ignored) {
            // Sin ubicación conocida del JAR se usan las demás alternativas.
        }
        String trabajo = System.getProperty("user.dir");
        if (trabajo != null && !trabajo.isBlank()) {
            Path directorio = rutaSegura(trabajo.trim());
            if (directorio != null) candidatos.add(directorio.resolve("tessdata"));
        }
        return elegirCarpetaTessdata(System.getenv("ANTUCAYEN_TESSDATA"),
                System.getProperty("antucayen.tessdata"), candidatos);
    }

    /**
     * Una ruta configurada explícitamente se respeta aunque no exista, para que el
     * error indique esa ruta; entre las ubicaciones automáticas se usa la primera
     * carpeta existente.
     */
    static Path elegirCarpetaTessdata(String variableEntorno, String propiedad, List<Path> candidatos) {
        for (String explicita : new String[]{variableEntorno, propiedad}) {
            if (explicita != null && !explicita.isBlank()) {
                Path ruta = rutaSegura(explicita.trim());
                if (ruta != null) return ruta.toAbsolutePath().normalize();
            }
        }
        for (Path candidato : candidatos) {
            if (candidato != null && Files.isDirectory(candidato)) {
                return candidato.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    static String resolverIdiomaOcr(Path datapath) {
        for (String configurado : new String[]{System.getenv("ANTUCAYEN_OCR_LANG"),
                System.getProperty("antucayen.ocr.lang")}) {
            if (configurado != null && !configurado.isBlank()) return configurado.trim();
        }
        return idiomaPorDefecto(datapath);
    }

    /** Las facturas están en español: se usa «spa» si sus datos están instalados. */
    static String idiomaPorDefecto(Path datapath) {
        if (datapath != null && Files.isRegularFile(datapath.resolve("spa.traineddata"))) {
            return "spa";
        }
        return "eng";
    }

    private static void agregarRuta(List<Path> destino, String ruta) {
        if (ruta == null || ruta.isBlank()) return;
        Path valor = rutaSegura(ruta.trim());
        if (valor != null) destino.add(valor);
    }

    private static Path rutaSegura(String ruta) {
        try {
            return Path.of(ruta);
        } catch (RuntimeException ex) {
            return null;
        }
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
        // En una foto la fecha puede quedar unas palabras después de la etiqueta,
        // o en la línea siguiente; nunca se cruza la etiqueta de vencimiento.
        Matcher etiqueta = PATRON_ETIQUETA_EMISION.matcher(texto);
        while (etiqueta.find()) {
            String siguiente = texto.substring(etiqueta.end(), Math.min(texto.length(), etiqueta.end() + 80));
            Matcher fecha = PATRON_FECHA_SUELTA.matcher(siguiente);
            if (fecha.find() && !normalizarComparacion(siguiente.substring(0, fecha.start())).contains("VENC")) {
                LocalDate valor = parsearFechaEmision(fecha.group());
                if (valor != null) return valor;
            }
        }
        return null;
    }

    private LocalDate parsearFechaEmision(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String fecha = valor.trim();
        Matcher textual = PATRON_FECHA_TEXTUAL.matcher(fecha);
        if (textual.matches()) {
            String nombreMes = normalizarComparacion(textual.group(2));
            int mes = "SETIEMBRE".equals(nombreMes) ? 9 : MESES.indexOf(nombreMes) + 1;
            if (mes <= 0) return null;
            try {
                return LocalDate.of(Integer.parseInt(textual.group(3)), mes, Integer.parseInt(textual.group(1)));
            } catch (java.time.DateTimeException ex) {
                return null;
            }
        }
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
                    || normalizada.contains("UNIDADES")
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

    /** Valor neto o subtotal de la factura, para comprobar la suma de los ítems leídos. */
    Integer extraerValorNeto(String texto) {
        if (texto == null || texto.isBlank()) return null;
        Matcher m = Pattern.compile("(?i)\\b(?:VALOR\\s+NETO|MONTO\\s+NETO|TOTAL\\s+NETO|SUB\\s?TOTAL|NETO)\\b"
                + "[^0-9\\n]{0,25}((?:[0-9]{1,3}(?:[.\\s][0-9]{3})+|[0-9]{4,12})(?:,[0-9]{1,2})?)").matcher(texto);
        Integer neto = null;
        while (m.find()) {
            Integer monto = parseMontoClp(m.group(1));
            if (monto != null && monto > 0) neto = monto;
        }
        return neto;
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
        return parsearLineas(texto, false);
    }

    /**
     * desdeOcr: el texto viene de una foto. Si no se reconoce la cabecera del
     * detalle, solo se acepta una fila cuando cantidad × precio = subtotal; así
     * el timbre electrónico y los textos del pie, que el OCR lee como letras y
     * números sueltos, no se convierten en productos.
     */
    List<ItemFacturaExtraido> parsearLineas(String texto, boolean desdeOcr) {
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

            if (seccionDetalleDetectada) {
                ItemFacturaExtraido conUnidad = parsearFilaConUnidad(linea, tokens);
                if (conUnidad != null) {
                    resultado.add(conUnidad);
                    continue;
                }
            }

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
            if (desdeOcr && !seccionDetalleDetectada && triple == null) continue;
            if (triple != null && !triple.cantidad().entero()) {
                resultado.add(itemCantidadFraccionaria(tokens, indiceCodigo, codigo, triple));
                continue;
            }
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

    /**
     * La base de datos guarda cantidades enteras. Una fila como «0,6 × 47.723,33
     * = 28.634» se conserva con su código y precio, como No Procesado, para que
     * el usuario indique cuántas unidades recibió.
     */
    private ItemFacturaExtraido itemCantidadFraccionaria(String[] tokens, int indiceCodigo,
                                                         String codigo, TriplePrecio triple) {
        String descripcion = construirDescripcion(tokens, indiceCodigo,
                triple.cantidad().indice(), triple.precioUnitario().indice());
        String cantidad = String.valueOf(triple.cantidad().exacto()).replace('.', ',');
        String nota = "la factura indica " + cantidad + " unidades, ingresa la cantidad entera";
        long precio = Math.round(triple.precioUnitario().exacto());
        return new ItemFacturaExtraido(
                codigo,
                descripcion.isBlank() ? nota : descripcion + " · " + nota,
                0,
                precio <= Integer.MAX_VALUE ? (int) precio : 0,
                EstadoItemFactura.NO_PROCESADO.valorDb());
    }

    /**
     * Fila con columna de unidad de medida: CODIGO DETALLE CANTIDAD UNIDAD
     * VALOR_UNITARIO [DESCUENTO] VALOR. Muchos proveedores imprimen el código
     * con un sufijo separado, como "3587 1" o "2086 0", y el OCR a veces lo
     * lee junto, como "2120" para "212 0". Para que la equivalencia sea
     * estable, el código se guarda siempre sin espacios: "35871", "20860".
     * Devuelve null si la fila no tiene esa estructura, y se usa el parser general.
     */
    private ItemFacturaExtraido parsearFilaConUnidad(String linea, String[] tokens) {
        if (tokens.length < 5) return null;

        // Última unidad de medida seguida solo de montos.
        int indiceUnidad = -1;
        for (int i = tokens.length - 2; i >= 2; i--) {
            String candidato = limpiarCodigo(tokens[i]);
            if (candidato == null) continue;
            candidato = candidato.replaceAll("\\.+$", "");
            if (!UNIDADES.contains(normalizarComparacion(candidato))) continue;
            boolean soloMontos = true;
            for (int j = i + 1; j < tokens.length; j++) {
                if (convertirNumero(tokens[j], j) == null) {
                    soloMontos = false;
                    break;
                }
            }
            if (soloMontos) {
                indiceUnidad = i;
                break;
            }
        }
        if (indiceUnidad < 0) return null;

        // Si después de la "unidad" vienen cantidad, precio y subtotal, la palabra
        // es parte de la descripción, como en "Arroz 1 kg 30 1.190 35.700".
        if (detectarTripleCantidadPrecioSubtotal(detectarNumeros(tokens, indiceUnidad + 1)) != null) return null;

        // Cantidad: token anterior a la unidad, aceptando lecturas OCR del 1.
        String tokenCantidad = tokens[indiceUnidad - 1];
        NumeroToken numeroCantidad = convertirNumero(tokenCantidad, indiceUnidad - 1);
        int cantidad;
        int finDescripcion = indiceUnidad - 1;
        boolean cantidadAntesDeUnidad = true;
        String sinPuntuacion = tokenCantidad.replaceAll("[^\\p{L}\\p{N}]", "");
        if (esCantidadPlausible(numeroCantidad)) {
            cantidad = (int) numeroCantidad.valor();
        } else if (PATRON_UNO_OCR.matcher(tokenCantidad).matches()
                || (sinPuntuacion.length() <= 2 && !sinPuntuacion.matches("\\d+"))) {
            // Símbolo corto ilegible en la columna de cantidad: el OCR suele leer así el 1.
            cantidad = 1;
        } else if (tokens[1].matches("\\d{1,3}") && tokens[2].matches(".*\\p{L}.*")) {
            // Formato CODIGO CANTIDAD DESCRIPCION UNIDAD PRECIO...: la cantidad va después del código.
            cantidad = Integer.parseInt(tokens[1]);
            if (cantidad <= 0) return null;
            finDescripcion = indiceUnidad;
            cantidadAntesDeUnidad = false;
        } else {
            return null;
        }

        // Código: número inicial más su sufijo numérico separado, o un código alfanumérico.
        String codigo;
        int inicioDescripcion;
        String primero = limpiarCodigo(tokens[0]);
        if (primero != null && primero.matches("\\d{2,15}")) {
            if (!cantidadAntesDeUnidad) {
                codigo = primero;
                inicioDescripcion = 2;
            } else if (tokens[1].matches("\\d{1,3}") && 2 < finDescripcion && tokens[2].matches(".*\\p{L}.*")) {
                codigo = primero + tokens[1];
                inicioDescripcion = 2;
            } else {
                codigo = primero;
                inicioDescripcion = 1;
            }
        } else if (primero != null && esCodigoProductoFuerte(primero)) {
            codigo = primero;
            inicioDescripcion = 1;
        } else {
            return null;
        }
        if (!esCodigoPlausible(codigo) || inicioDescripcion >= finDescripcion) return null;
        if (esLineaMetadata(linea, codigo, false)) return null;

        StringBuilder descripcion = new StringBuilder();
        for (int i = inicioDescripcion; i < finDescripcion; i++) {
            if (descripcion.length() > 0) descripcion.append(' ');
            descripcion.append(tokens[i]);
        }
        String textoDescripcion = descripcion.toString().replaceAll("[:;,]+$", "").trim();
        if (textoDescripcion.isBlank()) return null;

        NumeroToken precio = convertirNumero(tokens[indiceUnidad + 1], indiceUnidad + 1);
        int precioUnitario = precio != null && precio.valor() <= Integer.MAX_VALUE ? (int) precio.valor() : 0;

        return new ItemFacturaExtraido(
                codigo,
                textoDescripcion,
                cantidad,
                precioUnitario,
                EstadoItemFactura.OBSERVADO.valorDb());
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
        // Una cabecera nunca comienza con un código que tenga dígitos; así una fila como
        // "ABC-99 Producto con cantidad ilegible" no se confunde con la cabecera.
        if (normalizada.split(" ")[0].matches(".*[0-9].*")) return false;

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
        if (PATRON_FIN_DETALLE.matcher(normalizada).find()) return true;
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
                    || normalizada.startsWith(p + "-")
                    || normalizada.startsWith(p + ";")) {
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
        if (!codigoFuerte && PATRON_FECHA_TEXTUAL.matcher(linea).find()) return true;

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
            if (q.exacto() <= 0 || q.exacto() > 1_000_000) continue;
            if (!q.entero() && q.exacto() > 1000) continue;
            if (pu.exacto() <= 0 || sub.exacto() <= 0) continue;

            double esperado = q.exacto() * pu.exacto();
            double error = Math.abs(esperado - sub.exacto()) / Math.max(1d, sub.exacto());
            // Una cantidad fraccionaria, como 0,6, solo se acepta si cuadra exacto.
            double tolerancia = q.entero() ? 0.03d : 0.005d;
            if (error <= tolerancia || Math.abs(esperado - sub.exacto()) <= 2d) {
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
        String decimales = "";

        if (bruto.matches("\\d+")) {
            normalizado = bruto;
        } else if (bruto.matches("\\d{1,3}(?:[.,]\\d{3})+")) {
            normalizado = bruto.replace(".", "").replace(",", "");
        } else if (bruto.matches("\\d{1,3}(?:\\.\\d{3})+,\\d{1,2}")) {
            normalizado = bruto.substring(0, bruto.lastIndexOf(','))
                    .replace(".", "");
            decimales = bruto.substring(bruto.lastIndexOf(',') + 1);
        } else if (bruto.matches("\\d{1,3}(?:,\\d{3})+\\.\\d{1,2}")) {
            normalizado = bruto.substring(0, bruto.lastIndexOf('.'))
                    .replace(",", "");
            decimales = bruto.substring(bruto.lastIndexOf('.') + 1);
        } else if (bruto.matches("\\d+[.,]\\d{1,2}")) {
            // Decimal de unidad o volumen (1.5 L) o cantidad fraccionaria (0,6): no es entero.
            entero = false;
            int separador = Math.max(bruto.lastIndexOf('.'), bruto.lastIndexOf(','));
            normalizado = bruto.substring(0, separador);
            decimales = bruto.substring(separador + 1);
        } else {
            return null;
        }

        try {
            long valor = Long.parseLong(normalizado);
            double exacto = decimales.isEmpty() ? valor : Double.parseDouble(valor + "." + decimales);
            return new NumeroToken(indice, valor, entero, exacto);
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
