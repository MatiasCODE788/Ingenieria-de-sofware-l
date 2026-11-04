package cl.antucayen.model.service;

import cl.antucayen.model.domain.EstadoItemFactura;
import cl.antucayen.model.dto.ItemFacturaExtraido;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reconstruye la tabla de detalle de una factura a partir de las palabras que
 * entrega el OCR con su posición en la imagen.
 *
 * <p>No supone un formato fijo de columnas. La columna de códigos marca las
 * filas; cada columna numérica se ordena de arriba hacia abajo y se asigna a
 * las filas por su posición en la columna, no por la altura exacta. Así una
 * foto inclinada o una impresión desalineada, donde los montos quedan media
 * fila más arriba que los códigos, se lee igual. Las columnas de cantidad,
 * precio unitario y subtotal se reconocen porque cantidad × precio = subtotal
 * en la mayoría de las filas, sin importar en qué orden estén ni cuántas
 * columnas extra tenga la factura.</p>
 *
 * <p>Una cantidad que el OCR no logra leer se recupera como subtotal ÷ precio.
 * Lo que queda fuera de la tabla, como el timbre electrónico, los totales o
 * los textos al pie, nunca se convierte en producto.</p>
 */
final class ReconstructorTablaFactura {

    /** Palabra reconocida por el OCR con su caja en píxeles. */
    record Palabra(String texto, int x, int y, int ancho, int alto) {
        int derecha() { return x + ancho; }
        double centroY() { return y + alto / 2.0; }
    }

    /** Resultado de la reconstrucción: ítems y cuántos cuadran con cantidad × precio = subtotal. */
    record Resultado(List<ItemFacturaExtraido> items, List<Boolean> verificadas,
                     int filasVerificadas, long sumaSubtotales, boolean sinCodigo) {
        Resultado(List<ItemFacturaExtraido> items, List<Boolean> verificadas,
                  int filasVerificadas, long sumaSubtotales) {
            this(items, verificadas, filasVerificadas, sumaSubtotales, false);
        }

        static Resultado vacio() { return new Resultado(List.of(), List.of(), 0, 0); }

        /** La tabla es confiable si al menos dos filas, y la mitad de ellas, cuadran. */
        boolean confiable() {
            return filasVerificadas >= 2 && filasVerificadas * 2 >= items.size();
        }
    }

    private static final Set<String> CABECERA_IDENTIFICADOR = Set.of(
            "CODIGO", "COD", "ITEM", "SKU", "PRODUCTO", "ARTICULO", "DETALLE", "DESCRIPCION", "REF");
    private static final Set<String> CABECERA_DATOS = Set.of(
            "CANTIDAD", "CANT", "PRECIO", "UNITARIO", "UNIT", "VALOR", "TOTAL", "SUBTOTAL", "MONTO", "IMPORTE");
    /** Palabras que marcan el fin del detalle: totales, monto en palabras, timbre y pie. */
    private static final Set<String> FIN_DETALLE = Set.of(
            "SON", "TOTAL", "TOTALES", "NETO", "SUBTOTAL", "UNIDADES", "IVA", "TIMBRE", "ESTIMADO",
            "VERIFIQUE", "RETENCION", "EXENTO", "DESCUENTO", "RECARGO");

    private static final Pattern MONTO_MILES = Pattern.compile("\\d{1,3}(?:[.,]\\d{3})+(?:,\\d{1,2})?");
    private static final Pattern FECHA = Pattern.compile("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}");

    private ReconstructorTablaFactura() {
    }

    static Resultado reconstruir(List<Palabra> entrada) {
        if (entrada == null || entrada.isEmpty()) return Resultado.vacio();
        List<Palabra> palabras = separarPalabras(entrada);
        if (palabras.size() < 6) return Resultado.vacio();

        double altoTipico = mediana(palabras.stream()
                .filter(p -> p.texto().matches(".*[\\p{L}\\p{N}].*"))
                .mapToDouble(Palabra::alto).toArray());
        if (altoTipico <= 0) return Resultado.vacio();
        int anchoPagina = palabras.stream().mapToInt(Palabra::derecha).max().orElse(0);

        double inicioTabla = finCabecera(palabras, altoTipico);
        List<Palabra> codigos = columnaDeCodigos(palabras, inicioTabla, altoTipico, anchoPagina);
        // Factura sin columna de códigos, como la de un distribuidor pequeño que
        // solo escribe el nombre del producto: cada fila parte con la primera
        // palabra del nombre, alineada a la izquierda bajo la cabecera.
        // Si casi no hay códigos, los pocos que aparecen suelen ser números
        // sueltos del pie, como la orden de compra, y no una columna.
        boolean sinCodigo = false;
        if (codigos.size() < 4) {
            // En una foto la palabra «ITEM» de la cabecera suele leerse mal;
            // basta con que estén «CANTIDAD» y otra columna de montos.
            double cabecera = inicioTabla > 0 ? inicioTabla : cabeceraDeMontos(palabras, altoTipico);
            List<Palabra> nombres = cabecera > 0
                    ? columnaDeNombres(palabras, cabecera, altoTipico, anchoPagina) : List.of();
            if (nombres.size() >= 2 && nombres.size() > 2 * codigos.size()) {
                codigos = nombres;
                inicioTabla = cabecera;
                sinCodigo = true;
            }
        }
        if (codigos.size() < 2) return Resultado.vacio();

        double paso = pasoEntreFilas(codigos, altoTipico);
        double finTabla = finDetalle(palabras, codigos.get(0).centroY(), altoTipico);
        codigos = recortarFilas(codigos, finTabla, paso);
        if (codigos.size() < 2) return Resultado.vacio();
        paso = pasoEntreFilas(codigos, altoTipico);

        int n = codigos.size();
        double primera = codigos.get(0).centroY();
        double ultima = codigos.get(n - 1).centroY();
        double desde = Math.max(inicioTabla, primera - 1.6 * paso);
        double hasta = Math.min(finTabla - 0.3 * altoTipico, ultima + 1.6 * paso);
        // Sin códigos, la primera palabra del nombre tiene largo variable: el
        // resto del nombre empieza justo después de la más corta.
        int bordeCodigos = sinCodigo
                ? codigos.stream().mapToInt(Palabra::derecha).min().orElse(0)
                : codigos.stream().mapToInt(Palabra::derecha).max().orElse(0);

        List<Palabra> cuerpo = new ArrayList<>();
        for (Palabra p : palabras) {
            if (codigos.contains(p)) continue;
            if (p.centroY() < desde || p.centroY() > hasta) continue;
            if (p.x() < bordeCodigos - altoTipico) continue;
            cuerpo.add(p);
        }

        List<List<Palabra>> columnas = columnasNumericas(cuerpo, n, altoTipico, anchoPagina);
        if (columnas.isEmpty()) return Resultado.vacio();

        int anchoTabla = Math.max(1, anchoPagina - codigos.get(0).x());
        int limiteDescripcion = Integer.MAX_VALUE;
        List<List<Palabra>> columnasValores = new ArrayList<>();
        for (List<Palabra> columna : columnas) {
            int izquierda = (int) percentil(columna.stream().mapToDouble(Palabra::x).toArray(), 0.1);
            if (izquierda > bordeCodigos + 0.15 * anchoTabla) {
                limiteDescripcion = Math.min(limiteDescripcion, izquierda);
                columnasValores.add(columna);
            }
        }
        if (columnasValores.size() < 2) return Resultado.vacio();

        if (!sinCodigo) {
            codigos = agregarFilasSinCodigo(codigos, cuerpo, limiteDescripcion, paso, altoTipico);
        }
        n = codigos.size();

        // Valores de cada columna asignados a las filas por su orden.
        List<List<Palabra>> unidas = new ArrayList<>();
        List<int[]> asignaciones = new ArrayList<>();
        for (List<Palabra> columna : columnasValores) {
            List<Palabra> u = unirPorFila(columna, paso, altoTipico);
            unidas.add(u);
            asignaciones.add(asignarAFilas(codigos, u, paso));
        }
        ajustarPorAritmetica(codigos, unidas, asignaciones, paso);
        List<Double[]> valores = valoresPorFila(unidas, asignaciones, n);

        int[] roles = elegirRoles(valores, n);
        if (roles == null) return Resultado.vacio();

        // La descripción termina en la primera columna numérica alineada a la
        // derecha, como cantidad, precio o el valor sobre el que se aplicó el
        // impuesto. Los números sueltos de la descripción, como «1 kg» o «500 g»,
        // no forman una columna alineada y quedan en la descripción.
        int primeraColumna = roles[0] >= 0 ? roles[0] : roles[1];
        int limite = (int) percentil(columnasValores.get(primeraColumna).stream()
                .mapToDouble(Palabra::x).toArray(), 0.1);
        for (List<Palabra> columna : columnas) {
            int izquierdaColumna = (int) percentil(columna.stream().mapToDouble(Palabra::x).toArray(), 0.1);
            if (izquierdaColumna <= bordeCodigos + 0.15 * anchoTabla) continue;
            double[] bordes = columna.stream().mapToDouble(Palabra::derecha).toArray();
            boolean alineada = percentil(bordes, 0.9) - percentil(bordes, 0.1) <= 1.5 * altoTipico;
            if (alineada && columna.size() >= Math.max(2, 0.5 * n)) limite = Math.min(limite, izquierdaColumna);
        }
        List<Palabra> paraDescripcion = cuerpo;
        Map<Integer, List<Palabra>> descripciones = descripcionesPorFila(
                paraDescripcion, codigos, limite, paso, altoTipico);

        List<ItemFacturaExtraido> items = new ArrayList<>();
        List<Boolean> verificacion = new ArrayList<>();
        int verificadas = 0;
        long suma = 0;
        for (int i = 0; i < n; i++) {
            String codigo = limpiarCodigo(codigos.get(i).texto());
            if (codigo.isEmpty()) codigo = "";
            String descripcion = unirDescripcion(descripciones.get(i));
            if (sinCodigo) {
                // El nombre completo hace de código del proveedor, así la
                // equivalencia con el SKU se registra una vez por producto.
                descripcion = unirDescripcion(List.of(new Palabra(codigos.get(i).texto() + " " + descripcion,
                        0, 0, 1, 1)));
                codigo = codigoDesdeNombre(descripcion);
            }
            Double q = roles[0] < 0 ? null : valores.get(roles[0])[i];
            Double p = valores.get(roles[1])[i];
            Double s = valores.get(roles[2])[i];

            Fila fila = resolverFila(q, p, s);
            if (fila.verificada()) verificadas++;
            verificacion.add(fila.verificada());
            if (fila.subtotal() != null) suma += Math.round(fila.subtotal());

            if (codigo.isEmpty()) {
                String datos = fila.cantidad() == null ? "" : ", cantidad " + formatear(fila.cantidad());
                items.add(new ItemFacturaExtraido(null,
                        anotar(descripcion, "código ilegible" + datos + ", revisar en la factura"),
                        0, precio(fila.precio()), EstadoItemFactura.NO_PROCESADO.valorDb()));
            } else if (fila.cantidad() == null || fila.cantidad() <= 0) {
                items.add(new ItemFacturaExtraido(codigo,
                        anotar(descripcion, "cantidad ilegible, revisar en la factura"),
                        0, precio(fila.precio()), EstadoItemFactura.NO_PROCESADO.valorDb()));
            } else if (!esEntero(fila.cantidad())) {
                items.add(new ItemFacturaExtraido(codigo,
                        anotar(descripcion, "la factura indica " + formatear(fila.cantidad())
                                + " unidades, ingresa la cantidad entera"),
                        0, precio(fila.precio()), EstadoItemFactura.NO_PROCESADO.valorDb()));
            } else {
                items.add(new ItemFacturaExtraido(codigo, descripcion.isBlank() ? null : descripcion,
                        (int) Math.round(fila.cantidad()), precio(fila.precio()),
                        EstadoItemFactura.OBSERVADO.valorDb()));
            }
        }
        return new Resultado(items, verificacion, verificadas, suma, sinCodigo);
    }

    // ------------------------------------------------------------------ filas

    private record Fila(Double cantidad, Double precio, Double subtotal, boolean verificada) {
    }

    /**
     * Cantidad × precio = subtotal. Cuando no cuadra, la cantidad, que es el
     * dato más corto y el que el OCR lee peor, se recalcula como subtotal ÷ precio.
     */
    static Fila resolverFila(Double q, Double p, Double s) {
        if (q != null && p != null && s != null && cuadra(q, p, s)) {
            return new Fila(q, p, s, true);
        }
        if (p != null && s != null && p > 0) {
            Double derivada = cantidadLimpia(s, p);
            if (derivada != null && aceptaDerivada(q, derivada)) return new Fila(derivada, p, s, true);
        }
        // Sin verificación solo se acepta una cantidad corta, entera o con un decimal.
        boolean cantidadCreible = q != null && esCantidadRedonda(q);
        if (cantidadCreible && p != null && p > 0) {
            return new Fila(q, p, q * p, false);
        }
        if (cantidadCreible && s != null) {
            double precio = s / q;
            if (precio >= 1) return new Fila(q, precio, s, false);
        }
        return new Fila(null, p, s, false);
    }

    private static boolean cuadra(double q, double p, double s) {
        if (q <= 0 || p <= 0 || s <= 0 || !esCantidadPosible(q)) return false;
        // Margen del redondeo: el subtotal a pesos y el precio unitario que la
        // factura muestra sin decimales. Un margen mayor dejaría pasar lecturas
        // erradas que cuadran por casualidad.
        double margen = Math.max(1.5, Math.max(0.5 * q + 1, 0.0005 * s));
        return Math.abs(q * p - s) <= margen;
    }

    /**
     * Cantidad entera, o con un decimal, que resulta de dividir subtotal por
     * precio. El cociente es exacto salvo el redondeo del subtotal a pesos, que
     * lo mueve a lo más medio peso dividido por el precio; un precio mal leído
     * casi nunca da un cociente tan justo.
     */
    private static Double cantidadLimpia(double subtotal, double precio) {
        if (!(precio > 0)) return null;
        double valor = subtotal / precio;
        if (!(valor > 0) || valor > 1000) return null;
        // Medio peso del subtotal más medio peso por unidad si el precio se muestra redondeado.
        double entero = Math.rint(valor);
        if (entero >= 1 && Math.abs(valor - entero) <= (0.6 + 0.5 * entero) / precio + 1e-4) return entero;
        double unDecimal = Math.rint(valor * 10) / 10;
        if (unDecimal >= 0.1 && unDecimal < 10
                && Math.abs(valor - unDecimal) <= (0.6 + 0.5 * unDecimal) / precio + 1e-4) return unDecimal;
        return null;
    }

    /**
     * Una cantidad entera calculada se acepta siempre: es el caso típico de un
     * «1» leído como «7». Una con decimales solo si no se leyó otra cantidad,
     * o si la leída es la misma sin la coma, como «06» por «0,6»; si no, un
     * precio mal leído podría inventar una cantidad como 57,6.
     */
    private static boolean aceptaDerivada(Double leida, double derivada) {
        if (esEntero(derivada)) return true;
        if (leida == null || !esCantidadRedonda(leida)) return true;
        return Math.abs(leida - derivada) < 1e-6 || Math.abs(leida / 10 - derivada) < 1e-6;
    }

    /** Cantidad leída que parece real: entera o con un decimal, y no muy grande. */
    private static boolean esCantidadRedonda(double q) {
        if (!(q > 0) || q > 999) return false;
        return Math.abs(q * 10 - Math.rint(q * 10)) < 1e-6;
    }

    private static boolean esCantidadPosible(double q) {
        return q > 0 && q <= 10_000;
    }

    private static boolean esEntero(double q) {
        return Math.abs(q - Math.rint(q)) < 1e-6;
    }

    private static int precio(Double p) {
        if (p == null || p < 0 || p > Integer.MAX_VALUE) return 0;
        return (int) Math.round(p);
    }

    private static String formatear(double q) {
        return esEntero(q) ? String.valueOf((long) Math.rint(q))
                : String.valueOf(Math.rint(q * 100) / 100).replace('.', ',');
    }

    private static String anotar(String descripcion, String nota) {
        return (descripcion == null || descripcion.isBlank() ? "" : descripcion + " · ") + nota;
    }

    // ------------------------------------------------------------- columnas

    /**
     * Columna de mayor cantidad de códigos alineados a la izquierda. Ante un
     * empate gana la de más a la izquierda, porque el código del proveedor va
     * antes que la descripción.
     */
    private static List<Palabra> columnaDeCodigos(List<Palabra> palabras, double inicio,
                                                  double alto, int anchoPagina) {
        List<Palabra> candidatos = new ArrayList<>();
        for (Palabra p : palabras) {
            if (p.centroY() <= inicio) continue;
            if (esCodigo(p.texto())) candidatos.add(p);
        }
        if (candidatos.size() < 2) return List.of();
        candidatos.sort(Comparator.comparingInt(Palabra::x));
        double tolerancia = Math.max(2 * alto, 0.01 * anchoPagina);

        List<List<Palabra>> grupos = new ArrayList<>();
        List<Palabra> actual = new ArrayList<>();
        for (Palabra p : candidatos) {
            if (!actual.isEmpty() && p.x() - actual.get(actual.size() - 1).x() > tolerancia) {
                grupos.add(actual);
                actual = new ArrayList<>();
            }
            actual.add(p);
        }
        grupos.add(actual);

        List<List<Palabra>> corridas = new ArrayList<>();
        int mayor = 0;
        for (List<Palabra> grupo : grupos) {
            List<Palabra> corrida = corridaMasLarga(unaPorFila(grupo, alto), alto);
            corridas.add(corrida);
            mayor = Math.max(mayor, corrida.size());
        }
        // Una factura puede traer dos columnas de códigos, como «10206091 | 5288»:
        // el del proveedor es el de más a la izquierda.
        for (List<Palabra> corrida : corridas) {
            if (corrida.size() >= 2 && corrida.size() >= 0.7 * mayor) return corrida;
        }
        return List.of();
    }

    /**
     * Filas de una factura sin códigos: la primera palabra de cada línea bajo
     * la cabecera, cuando varias líneas parten a la misma distancia del borde.
     * Las líneas que siguen un nombre largo, como «(12 unidades)», parten más
     * a la derecha y quedan como parte de la descripción.
     */
    private static List<Palabra> columnaDeNombres(List<Palabra> palabras, double inicio,
                                                  double alto, int anchoPagina) {
        List<Palabra> candidatos = new ArrayList<>();
        for (Palabra p : palabras) {
            if (p.centroY() <= inicio || p.x() > 0.4 * anchoPagina) continue;
            if (!p.texto().matches("\\p{L}{2,}.*")) continue;
            if (FIN_DETALLE.contains(normalizar(p.texto()))) continue;
            boolean primeraDeLaLinea = true;
            for (Palabra otra : palabras) {
                if (otra != p && otra.x() < p.x() - 0.5 * alto
                        && Math.abs(otra.centroY() - p.centroY()) < 0.6 * alto
                        && otra.texto().matches(".*[\\p{L}\\p{N}].*")) {
                    primeraDeLaLinea = false;
                    break;
                }
            }
            if (primeraDeLaLinea) candidatos.add(p);
        }
        if (candidatos.size() < 2) return List.of();
        candidatos.sort(Comparator.comparingInt(Palabra::x));
        double tolerancia = Math.max(1.5 * alto, 0.008 * anchoPagina);

        List<Palabra> mejor = List.of();
        List<Palabra> actual = new ArrayList<>();
        for (int i = 0; i <= candidatos.size(); i++) {
            Palabra p = i < candidatos.size() ? candidatos.get(i) : null;
            if (p == null || (!actual.isEmpty() && p.x() - actual.get(actual.size() - 1).x() > tolerancia)) {
                List<Palabra> corrida = corridaMasLarga(unaPorFila(actual, alto), alto);
                if (corrida.size() > mejor.size()) mejor = corrida;
                actual = new ArrayList<>();
            }
            if (p != null) actual.add(p);
        }
        return mejor.size() >= 2 ? mejor : List.of();
    }

    /** Código del proveedor para un producto sin código: su nombre en mayúsculas, hasta 50 letras. */
    static String codigoDesdeNombre(String nombre) {
        if (nombre == null) return "";
        String limpio = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9()]+", " ")
                .trim()
                .replaceAll(" +", " ");
        if (limpio.length() <= 50) return limpio;
        int corte = limpio.lastIndexOf(' ', 50);
        return limpio.substring(0, corte > 20 ? corte : 50).trim();
    }

    /** Tramo más largo de filas seguidas, sin saltos grandes como el que separa la cabecera del detalle. */
    private static List<Palabra> corridaMasLarga(List<Palabra> filas, double alto) {
        if (filas.size() < 3) return filas;
        double paso = pasoEntreFilas(filas, alto);
        List<Palabra> mejor = new ArrayList<>();
        List<Palabra> actual = new ArrayList<>();
        for (Palabra p : filas) {
            if (!actual.isEmpty() && p.centroY() - actual.get(actual.size() - 1).centroY() > 2.5 * paso) {
                if (actual.size() > mejor.size()) mejor = actual;
                actual = new ArrayList<>();
            }
            actual.add(p);
        }
        if (actual.size() > mejor.size()) mejor = actual;
        return mejor;
    }

    private static List<Palabra> unaPorFila(List<Palabra> grupo, double alto) {
        List<Palabra> ordenado = new ArrayList<>(grupo);
        ordenado.sort(Comparator.comparingDouble(Palabra::centroY));
        List<Palabra> filas = new ArrayList<>();
        for (Palabra p : ordenado) {
            if (!filas.isEmpty() && p.centroY() - filas.get(filas.size() - 1).centroY() < 0.6 * alto) continue;
            filas.add(p);
        }
        return filas;
    }

    private static double pasoEntreFilas(List<Palabra> codigos, double alto) {
        if (codigos.size() < 3) return 1.6 * alto;
        double[] diferencias = new double[codigos.size() - 1];
        for (int i = 1; i < codigos.size(); i++) {
            diferencias[i - 1] = codigos.get(i).centroY() - codigos.get(i - 1).centroY();
        }
        return Math.max(alto, mediana(diferencias));
    }

    /** Corta la columna de códigos al fin del detalle o ante un salto grande entre filas. */
    private static List<Palabra> recortarFilas(List<Palabra> codigos, double fin, double paso) {
        List<Palabra> filas = new ArrayList<>();
        for (Palabra p : codigos) {
            if (p.centroY() >= fin) break;
            if (!filas.isEmpty() && p.centroY() - filas.get(filas.size() - 1).centroY() > 3.5 * paso) break;
            filas.add(p);
        }
        return filas;
    }

    /**
     * Columnas de números del cuerpo de la tabla, agrupadas por su borde
     * derecho. Cada columna conserva también las palabras ilegibles que caen
     * en ella, para que el orden de sus valores no se corra.
     */
    private static List<List<Palabra>> columnasNumericas(List<Palabra> cuerpo, int filas,
                                                        double alto, int anchoPagina) {
        List<Palabra> numericas = new ArrayList<>();
        for (Palabra p : cuerpo) {
            if (esNumerica(p.texto())) numericas.add(p);
        }
        if (numericas.isEmpty()) return List.of();
        numericas.sort(Comparator.comparingInt(Palabra::derecha));
        double tolerancia = Math.max(1.2 * alto, 0.008 * anchoPagina);

        List<List<Palabra>> grupos = new ArrayList<>();
        List<Palabra> actual = new ArrayList<>();
        for (Palabra p : numericas) {
            if (!actual.isEmpty() && p.derecha() - actual.get(actual.size() - 1).derecha() > tolerancia) {
                grupos.add(actual);
                actual = new ArrayList<>();
            }
            actual.add(p);
        }
        grupos.add(actual);

        int minimo = Math.max(2, (int) Math.ceil(0.4 * filas));
        List<List<Palabra>> columnas = new ArrayList<>();
        for (List<Palabra> grupo : grupos) {
            if (grupo.size() < minimo) continue;
            int bordeMin = grupo.get(0).derecha();
            int bordeMax = grupo.get(grupo.size() - 1).derecha();
            int izquierdaMin = grupo.stream().mapToInt(Palabra::x).min().orElse(0);
            List<Palabra> columna = new ArrayList<>();
            for (Palabra p : cuerpo) {
                boolean enBorde = p.derecha() >= bordeMin - tolerancia / 2 && p.derecha() <= bordeMax + tolerancia / 2;
                if (!enBorde || p.x() < izquierdaMin - 2 * alto) continue;
                if (grupo.contains(p) || esRuidoDeColumna(p, alto)) columna.add(p);
            }
            columnas.add(columna);
        }
        columnas.sort(Comparator.comparingDouble(c -> mediana(c.stream().mapToDouble(Palabra::derecha).toArray())));
        return columnas;
    }

    /** Palabra ilegible del ancho de un número, que ocupa un lugar en la columna. */
    private static boolean esRuidoDeColumna(Palabra p, double alto) {
        String t = p.texto().replaceAll("[^\\p{L}\\p{N}]", "");
        return !t.isEmpty() && t.length() <= 9 && p.alto() >= 0.5 * alto && !t.matches("\\p{L}{4,}");
    }

    /** Une los pedazos de un mismo valor, como «32» y «404», que el OCR separó. */
    private static List<Palabra> unirPorFila(List<Palabra> columna, double paso, double alto) {
        List<Palabra> ordenada = new ArrayList<>(columna);
        ordenada.sort(Comparator.comparingDouble(Palabra::centroY));
        List<Palabra> unidas = new ArrayList<>();
        for (Palabra p : ordenada) {
            if (!unidas.isEmpty()) {
                Palabra anterior = unidas.get(unidas.size() - 1);
                boolean mismaFila = Math.abs(p.centroY() - anterior.centroY()) < 0.5 * paso;
                boolean contiguas = Math.max(p.x(), anterior.x())
                        - Math.min(p.derecha(), anterior.derecha()) < 1.5 * alto;
                if (mismaFila && contiguas) {
                    Palabra izquierda = p.x() < anterior.x() ? p : anterior;
                    Palabra derecha = izquierda == p ? anterior : p;
                    int x = izquierda.x();
                    int y = Math.min(p.y(), anterior.y());
                    int ancho = Math.max(p.derecha(), anterior.derecha()) - x;
                    int altoUnido = Math.max(p.y() + p.alto(), anterior.y() + anterior.alto()) - y;
                    unidas.set(unidas.size() - 1, new Palabra(
                            izquierda.texto() + " " + derecha.texto(), x, y, ancho, altoUnido));
                    continue;
                }
            }
            unidas.add(p);
        }
        return unidas;
    }

    /**
     * Asigna los valores de una columna a las filas. Si hay tantos valores
     * como códigos, el i-ésimo valor es de la i-ésima fila. Si falta o sobra
     * alguno, se alinean por altura permitiendo un corrimiento que cambia
     * linealmente de arriba hacia abajo, como en una foto inclinada.
     */
    static int[] asignarAFilas(List<Palabra> codigos, List<Palabra> valores, double paso) {
        int n = codigos.size();
        int m = valores.size();
        int[] asignacion = new int[n];
        java.util.Arrays.fill(asignacion, -1);
        if (m == 0) return asignacion;
        if (m == n) {
            for (int i = 0; i < n; i++) asignacion[i] = i;
            return asignacion;
        }

        double y0 = codigos.get(0).centroY();
        double y1 = codigos.get(n - 1).centroY();
        double rango = Math.max(1, y1 - y0);
        double mejorCosto = Double.MAX_VALUE;
        int[] mejor = asignacion;
        for (int a = -16; a <= 16; a++) {
            for (int b = -16; b <= 16; b++) {
                double desplazamientoInicio = a * 0.1 * paso;
                double desplazamientoFin = b * 0.1 * paso;
                int[] candidata = new int[n];
                // Un corrimiento pequeño es más probable: ante dos alineaciones
                // igual de buenas gana la que mueve menos los valores.
                double costo = alinear(codigos, valores, paso, y0, rango,
                        desplazamientoInicio, desplazamientoFin, candidata)
                        + 0.15 * (Math.abs(a) + Math.abs(b)) / 10.0;
                if (costo < mejorCosto) {
                    mejorCosto = costo;
                    mejor = candidata;
                }
            }
        }
        return mejor;
    }

    private static double alinear(List<Palabra> codigos, List<Palabra> valores, double paso,
                                  double y0, double rango, double d0, double d1, int[] salida) {
        int n = codigos.size();
        int m = valores.size();
        double salto = 0.6;
        double[][] costo = new double[n + 1][m + 1];
        int[][] origen = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                if (i == 0 && j == 0) continue;
                double mejor = Double.MAX_VALUE;
                int desde = 0;
                if (i > 0 && j > 0) {
                    double yCodigo = codigos.get(i - 1).centroY();
                    double esperado = yCodigo + d0 + (d1 - d0) * (yCodigo - y0) / rango;
                    double diferencia = Math.min(1.0, Math.abs(valores.get(j - 1).centroY() - esperado) / paso);
                    double c = costo[i - 1][j - 1] + diferencia;
                    if (c < mejor) { mejor = c; desde = 1; }
                }
                if (i > 0 && costo[i - 1][j] + salto < mejor) { mejor = costo[i - 1][j] + salto; desde = 2; }
                if (j > 0 && costo[i][j - 1] + salto < mejor) { mejor = costo[i][j - 1] + salto; desde = 3; }
                costo[i][j] = mejor;
                origen[i][j] = desde;
            }
        }
        java.util.Arrays.fill(salida, -1);
        int i = n;
        int j = m;
        while (i > 0 || j > 0) {
            int d = origen[i][j];
            if (d == 1) { salida[i - 1] = j - 1; i--; j--; }
            else if (d == 2) i--;
            else j--;
        }
        return costo[n][m];
    }

    private static List<Double[]> valoresPorFila(List<List<Palabra>> unidas, List<int[]> asignaciones, int n) {
        List<Double[]> valores = new ArrayList<>();
        for (int c = 0; c < unidas.size(); c++) {
            Double[] fila = new Double[n];
            int[] asignacion = asignaciones.get(c);
            for (int i = 0; i < n; i++) {
                fila[i] = asignacion[i] < 0 ? null : interpretarNumero(unidas.get(c).get(asignacion[i]).texto());
            }
            valores.add(fila);
        }
        return valores;
    }

    /**
     * Cuando a una columna le faltan o le sobran uno o dos valores, la altura
     * no basta para saber dónde está el hueco: en una foto inclinada un valor
     * puede quedar a la altura de la fila vecina. Se prueba cada ubicación
     * posible del hueco y se queda la que hace cuadrar más filas con
     * cantidad × precio = subtotal; ante un empate, la más pareja en altura.
     */
    private static void ajustarPorAritmetica(List<Palabra> codigos, List<List<Palabra>> unidas,
                                             List<int[]> asignaciones, double paso) {
        int n = codigos.size();
        for (int ronda = 0; ronda < 2; ronda++) {
            for (int c = 0; c < unidas.size(); c++) {
                int m = unidas.get(c).size();
                int diferencia = Math.abs(m - n);
                if (diferencia == 0 || diferencia > 2 || m == 0) continue;
                int[] mejor = asignaciones.get(c);
                double[] mejorPuntaje = puntaje(codigos, unidas, asignaciones, c, mejor, paso);
                for (int[] candidata : ubicaciones(n, m)) {
                    double[] p = puntaje(codigos, unidas, asignaciones, c, candidata, paso);
                    if (p[0] > mejorPuntaje[0] + 1e-9
                            || (Math.abs(p[0] - mejorPuntaje[0]) <= 1e-9 && p[1] < mejorPuntaje[1] - 1e-9)) {
                        mejor = candidata;
                        mejorPuntaje = p;
                    }
                }
                asignaciones.set(c, mejor);
            }
        }
    }

    /** Todas las formas de repartir m valores en orden entre n filas cuando difieren en uno o dos. */
    private static List<int[]> ubicaciones(int n, int m) {
        List<int[]> lista = new ArrayList<>();
        if (m < n) {
            int faltan = n - m;
            for (int a = 0; a < n; a++) {
                for (int b = faltan == 2 ? a + 1 : n; b <= (faltan == 2 ? n - 1 : n); b++) {
                    int[] asignacion = new int[n];
                    int j = 0;
                    for (int i = 0; i < n; i++) {
                        asignacion[i] = (i == a || (faltan == 2 && i == b)) ? -1 : j++;
                    }
                    lista.add(asignacion);
                    if (faltan == 1) break;
                }
            }
        } else {
            int sobran = m - n;
            for (int a = 0; a < m; a++) {
                for (int b = sobran == 2 ? a + 1 : m; b <= (sobran == 2 ? m - 1 : m); b++) {
                    int[] asignacion = new int[n];
                    int i = 0;
                    for (int j = 0; j < m && i < n; j++) {
                        if (j == a || (sobran == 2 && j == b)) continue;
                        asignacion[i++] = j;
                    }
                    lista.add(asignacion);
                    if (sobran == 1) break;
                }
            }
        }
        return lista;
    }

    /** Filas que cuadran con la mejor combinación de columnas, y desorden en altura como desempate. */
    private static double[] puntaje(List<Palabra> codigos, List<List<Palabra>> unidas,
                                    List<int[]> asignaciones, int columna, int[] candidata, double paso) {
        List<int[]> prueba = new ArrayList<>(asignaciones);
        prueba.set(columna, candidata);
        int n = codigos.size();
        List<Double[]> valores = valoresPorFila(unidas, prueba, n);
        double mejor = 0;
        for (int a = 0; a < valores.size(); a++) {
            for (int b = a + 1; b < valores.size(); b++) {
                for (int c = b + 1; c < valores.size(); c++) {
                    if (a != columna && b != columna && c != columna) continue;
                    double cuenta = 0;
                    for (int i = 0; i < n; i++) {
                        Double q = valores.get(a)[i];
                        Double p = valores.get(b)[i];
                        Double s = valores.get(c)[i];
                        if (q != null && p != null && s != null && cuadra(q, p, s)) cuenta += 1;
                        else if (p != null && s != null && p > 0 && cantidadLimpia(s, p) != null) cuenta += 0.3;
                    }
                    mejor = Math.max(mejor, cuenta);
                }
            }
        }
        // Desorden: cuánto se aparta cada valor del corrimiento típico de su columna.
        List<Palabra> u = unidas.get(columna);
        List<Double> desplazamientos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (candidata[i] >= 0) desplazamientos.add(u.get(candidata[i]).centroY() - codigos.get(i).centroY());
        }
        double[] d = desplazamientos.stream().mapToDouble(Double::doubleValue).toArray();
        double tipico = mediana(d);
        double desorden = 0;
        for (double v : d) desorden += Math.abs(v - tipico) / paso;
        return new double[]{mejor, desorden};
    }

    /**
     * Elige qué columnas son cantidad, precio y subtotal: la combinación de
     * tres columnas, de izquierda a derecha, en que más filas cumplen
     * cantidad × precio = subtotal.
     */
    private static int[] elegirRoles(List<Double[]> valores, int n) {
        int[] mejor = null;
        int mejorCuenta = 0;
        for (int a = 0; a < valores.size(); a++) {
            for (int b = a + 1; b < valores.size(); b++) {
                for (int c = b + 1; c < valores.size(); c++) {
                    int cuenta = 0;
                    for (int i = 0; i < n; i++) {
                        Double q = valores.get(a)[i];
                        Double p = valores.get(b)[i];
                        Double s = valores.get(c)[i];
                        if (q != null && p != null && s != null && cuadra(q, p, s)) cuenta++;
                    }
                    if (cuenta > mejorCuenta || (cuenta == mejorCuenta && cuenta > 0 && mejor != null && c > mejor[2])) {
                        mejorCuenta = cuenta;
                        mejor = new int[]{a, b, c};
                    }
                }
            }
        }
        int minimo = Math.max(2, (int) Math.ceil(0.25 * n));
        // Sin columna de cantidad legible: precio y subtotal bastan, porque la
        // cantidad sale de dividirlos. Se usa solo si cuadran más filas así.
        int[] par = null;
        int cuentaPar = 0;
        for (int b = 0; b < valores.size(); b++) {
            for (int c = b + 1; c < valores.size(); c++) {
                int cuenta = 0;
                for (int i = 0; i < n; i++) {
                    Double p = valores.get(b)[i];
                    Double s = valores.get(c)[i];
                    if (p != null && s != null && p >= 10 && cantidadLimpia(s, p) != null) cuenta++;
                }
                if (cuenta > cuentaPar || (cuenta == cuentaPar && cuenta > 0 && par != null && c > par[2])) {
                    cuentaPar = cuenta;
                    par = new int[]{-1, b, c};
                }
            }
        }
        if (mejor != null && mejorCuenta >= minimo && mejorCuenta * 5 >= cuentaPar * 4) return mejor;
        if (par != null && cuentaPar >= minimo) return par;
        return null;
    }

    // --------------------------------------------------------- descripción

    /**
     * Fila cuyo código el OCR no pudo leer: hay una línea de descripción sin
     * código al lado. Se agrega una fila sin código para que los montos de las
     * filas siguientes no se corran.
     */
    private static List<Palabra> agregarFilasSinCodigo(List<Palabra> codigos, List<Palabra> cuerpo,
                                                      int limite, double paso, double alto) {
        List<Palabra> lineas = new ArrayList<>();
        for (Palabra p : cuerpo) {
            if (p.x() >= limite - 0.5 * alto || !p.texto().matches(".*\\p{L}{2,}.*")) continue;
            lineas.add(p);
        }
        lineas.sort(Comparator.comparingDouble(Palabra::centroY));
        double primera = codigos.get(0).centroY();
        double ultima = codigos.get(codigos.size() - 1).centroY();
        List<Palabra> resultado = new ArrayList<>(codigos);
        int i = 0;
        while (i < lineas.size()) {
            int j = i;
            while (j + 1 < lineas.size() && lineas.get(j + 1).centroY() - lineas.get(i).centroY() < 0.4 * paso) j++;
            double y = 0;
            for (int k = i; k <= j; k++) y += lineas.get(k).centroY();
            y /= (j - i + 1);
            boolean dentro = y > primera - 1.4 * paso && y < ultima + 1.4 * paso;
            double cercano = Double.MAX_VALUE;
            for (Palabra c : resultado) cercano = Math.min(cercano, Math.abs(c.centroY() - y));
            if (dentro && j - i + 1 >= 2 && cercano > 0.7 * paso) {
                resultado.add(new Palabra("", 0, (int) Math.round(y - alto / 2), 0, (int) Math.round(alto)));
            }
            i = j + 1;
        }
        resultado.sort(Comparator.comparingDouble(Palabra::centroY));
        return resultado;
    }

    private static Map<Integer, List<Palabra>> descripcionesPorFila(List<Palabra> cuerpo, List<Palabra> codigos,
                                                                  int limite, double paso, double alto) {
        List<Palabra> zona = new ArrayList<>();
        for (Palabra p : cuerpo) {
            if (p.x() >= limite - 0.5 * alto) continue;
            if (!p.texto().matches(".*[\\p{L}\\p{N}].*")) continue;
            zona.add(p);
        }
        // Las palabras se encadenan de izquierda a derecha siguiendo la inclinación
        // de la línea; la línea se asigna por su primera palabra, la más cercana al código.
        zona.sort(Comparator.comparingInt(Palabra::x));
        List<List<Palabra>> lineas = new ArrayList<>();
        for (Palabra p : zona) {
            List<Palabra> elegida = null;
            double mejor = 0.35 * paso;
            for (List<Palabra> linea : lineas) {
                Palabra ultima = linea.get(linea.size() - 1);
                double d = Math.abs(ultima.centroY() - p.centroY());
                if (p.x() >= ultima.x() && d < mejor) { mejor = d; elegida = linea; }
            }
            if (elegida == null) {
                elegida = new ArrayList<>();
                lineas.add(elegida);
            }
            elegida.add(p);
        }
        Map<Integer, List<Palabra>> porFila = new HashMap<>();
        for (List<Palabra> linea : lineas) {
            double y = linea.get(0).centroY();
            int cercana = -1;
            double distancia = Double.MAX_VALUE;
            for (int i = 0; i < codigos.size(); i++) {
                double d = Math.abs(codigos.get(i).centroY() - y);
                if (d < distancia) { distancia = d; cercana = i; }
            }
            if (cercana >= 0 && distancia <= 0.7 * paso) {
                porFila.computeIfAbsent(cercana, k -> new ArrayList<>()).addAll(linea);
            }
        }
        return porFila;
    }

    private static String unirDescripcion(List<Palabra> palabras) {
        if (palabras == null || palabras.isEmpty()) return "";
        List<Palabra> ordenadas = new ArrayList<>(palabras);
        ordenadas.sort(Comparator.comparingInt(Palabra::x));
        StringBuilder sb = new StringBuilder();
        for (Palabra p : ordenadas) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.texto());
        }
        return sb.toString().replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N})]+$", "").trim();
    }

    // ----------------------------------------------------- cabecera y pie

    /** Borde inferior de la cabecera de la tabla, o 0 si no se reconoce. */
    private static double finCabecera(List<Palabra> palabras, double alto) {
        List<Palabra> marcas = new ArrayList<>();
        for (Palabra p : palabras) {
            String n = normalizar(p.texto());
            if (coincide(n, CABECERA_IDENTIFICADOR) || coincide(n, CABECERA_DATOS)) marcas.add(p);
        }
        marcas.sort(Comparator.comparingDouble(Palabra::centroY));
        for (Palabra base : marcas) {
            boolean identificador = false;
            boolean datos = false;
            double fondo = 0;
            for (Palabra otra : marcas) {
                if (Math.abs(otra.centroY() - base.centroY()) > 1.5 * alto) continue;
                String n = normalizar(otra.texto());
                identificador |= coincide(n, CABECERA_IDENTIFICADOR);
                datos |= coincide(n, CABECERA_DATOS);
                fondo = Math.max(fondo, otra.y() + otra.alto());
            }
            if (identificador && datos) return fondo;
        }
        return 0;
    }

    /** Borde inferior de una línea con «CANTIDAD» y otro título de montos, o 0 si no hay. */
    private static double cabeceraDeMontos(List<Palabra> palabras, double alto) {
        for (Palabra base : palabras) {
            if (!coincide(normalizar(base.texto()), Set.of("CANTIDAD", "CANT"))) continue;
            double fondo = base.y() + base.alto();
            boolean otra = false;
            for (Palabra p : palabras) {
                if (p == base || Math.abs(p.centroY() - base.centroY()) > 1.5 * alto) continue;
                String n = normalizar(p.texto());
                if (coincide(n, CABECERA_DATOS) && !coincide(n, Set.of("CANTIDAD", "CANT"))) {
                    otra = true;
                    fondo = Math.max(fondo, p.y() + p.alto());
                }
            }
            if (otra) return fondo;
        }
        return 0;
    }

    /** Altura de la primera palabra de cierre (totales, timbre, pie) bajo la primera fila. */
    private static double finDetalle(List<Palabra> palabras, double primeraFila, double alto) {
        double fin = Double.MAX_VALUE;
        for (Palabra p : palabras) {
            if (p.centroY() <= primeraFila + 0.5 * alto) continue;
            if (!FIN_DETALLE.contains(normalizar(p.texto()))) continue;
            // «(12 unidades)» o «6 unidades» es parte del nombre de un producto, no el pie.
            if (vieneDespuesDeUnNumero(palabras, p, alto)) continue;
            fin = Math.min(fin, p.y());
        }
        return fin;
    }

    private static boolean vieneDespuesDeUnNumero(List<Palabra> palabras, Palabra p, double alto) {
        Palabra anterior = null;
        for (Palabra otra : palabras) {
            if (otra == p || otra.derecha() > p.x() + 0.3 * alto || p.x() - otra.derecha() > 1.5 * alto) continue;
            if (Math.abs(otra.centroY() - p.centroY()) > 0.6 * alto) continue;
            if (anterior == null || otra.derecha() > anterior.derecha()) anterior = otra;
        }
        return anterior != null && anterior.texto().matches("[(\\[]?\\d{1,4}[.,]?");
    }

    private static boolean coincide(String palabra, Set<String> claves) {
        if (palabra.length() < 3) return false;
        for (String clave : claves) {
            int permitido = clave.length() >= 7 ? 2 : clave.length() >= 5 ? 1 : 0;
            if (Math.abs(clave.length() - palabra.length()) > permitido) continue;
            if (distancia(palabra, clave) <= permitido) return true;
        }
        return false;
    }

    private static int distancia(String a, String b) {
        int[] previa = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previa[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1, previa[j] + 1), previa[j - 1] + costo);
            }
            int[] t = previa; previa = actual; actual = t;
        }
        return previa[b.length()];
    }

    // ------------------------------------------------------------ textos

    /** Separa las palabras que el OCR juntó con la línea de la tabla, como «10206091|5288». */
    private static List<Palabra> separarPalabras(List<Palabra> entrada) {
        List<Palabra> salida = new ArrayList<>();
        for (Palabra p : entrada) {
            if (p.texto() == null) continue;
            String texto = p.texto().trim();
            if (texto.isEmpty()) continue;
            String[] partes = texto.split("[|\\[\\]{}]+");
            if (partes.length == 1 && partes[0].equals(texto)) {
                salida.add(new Palabra(texto, p.x(), p.y(), p.ancho(), p.alto()));
                continue;
            }
            int largo = Math.max(1, texto.length());
            int posicion = 0;
            for (String parte : texto.split("(?<=[|\\[\\]{}])|(?=[|\\[\\]{}])")) {
                int desde = posicion;
                posicion += parte.length();
                String limpia = parte.replaceAll("[|\\[\\]{}]", "").trim();
                if (limpia.isEmpty()) continue;
                int x = p.x() + (int) Math.round((double) p.ancho() * desde / largo);
                int ancho = Math.max(1, (int) Math.round((double) p.ancho() * parte.length() / largo));
                salida.add(new Palabra(limpia, x, p.y(), ancho, p.alto()));
            }
        }
        return salida;
    }

    static boolean esCodigo(String texto) {
        String t = limpiarCodigo(texto);
        if (t.length() < 3 || t.length() > 20) return false;
        if (!t.matches("[A-Za-z0-9][A-Za-z0-9._/-]*")) return false;
        if (!t.matches(".*\\d.*")) return false;
        if (MONTO_MILES.matcher(t).matches() || FECHA.matcher(t).matches()) return false;
        if (texto.matches(".*\\d{1,2}\\.\\d{3}\\.\\d{3}.*")) return false; // RUT o monto grande
        if (t.matches("\\d+[.,]\\d{1,2}")) return false;
        // «20s», «18s» o «10s» son tamaños de envase dentro de la descripción.
        return !t.matches("\\d{1,3}[A-Za-z]{1,2}");
    }

    private static String limpiarCodigo(String texto) {
        return texto == null ? "" : texto.replaceAll("^[^A-Za-z0-9]+|[^A-Za-z0-9]+$", "");
    }

    private static boolean esNumerica(String texto) {
        String t = texto.replaceAll("[\\s$]", "");
        if (t.isEmpty() || !t.matches(".*\\d.*")) return false;
        long digitos = t.chars().filter(Character::isDigit).count();
        return digitos * 10 >= t.length() * 6 && t.matches("[\\d.,'’:;oOlI|-]+");
    }

    /**
     * Interpreta un número de la tabla: «45.375», «47.723,33», «0,6»,
     * «26.081.67» o «118 551». Las letras que el OCR confunde con dígitos se
     * corrigen antes.
     */
    static Double interpretarNumero(String texto) {
        if (texto == null) return null;
        String t = texto.trim()
                .replace('O', '0').replace('o', '0').replace('Q', '0').replace('D', '0')
                .replace('l', '1').replace('I', '1').replace('i', '1').replace('!', '1')
                .replace('Z', '2').replace('z', '2').replace('S', '5').replace('s', '5')
                .replace('B', '8').replace('’', ',').replace('\'', ',').replace(':', '.').replace(';', ',');
        t = t.replaceAll("[^0-9.,\\s-]", "").trim();
        if (t.isEmpty() || t.startsWith("-")) return null;
        t = t.replaceAll("(?<=\\d)\\s+(?=\\d{3}(?!\\d))", "").replaceAll("\\s", "");
        if (!t.matches("\\d[\\d.,]*")) return null;
        try {
            if (t.matches("\\d+")) return Double.parseDouble(t);
            int ultimo = Math.max(t.lastIndexOf('.'), t.lastIndexOf(','));
            String entera = t.substring(0, ultimo).replaceAll("[.,]", "");
            String fraccion = t.substring(ultimo + 1);
            if (fraccion.length() == 3) return Double.parseDouble(entera + fraccion);
            if (fraccion.length() == 1 || fraccion.length() == 2) {
                return Double.parseDouble((entera.isEmpty() ? "0" : entera) + "." + fraccion);
            }
            return null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizar(String texto) {
        String n = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
    }

    // --------------------------------------------------------- estadística

    private static double mediana(double[] valores) {
        return percentil(valores, 0.5);
    }

    private static double percentil(double[] valores, double fraccion) {
        if (valores.length == 0) return 0;
        double[] copia = valores.clone();
        java.util.Arrays.sort(copia);
        int indice = (int) Math.floor(fraccion * (copia.length - 1));
        return copia[Math.max(0, Math.min(copia.length - 1, indice))];
    }
}
