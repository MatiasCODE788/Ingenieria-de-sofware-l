package cl.antucayen.controller;

import cl.antucayen.model.domain.EstadoProducto;
import cl.antucayen.model.entity.ErrorImportacion;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.service.ServicioAuditoriaArchivos;
import cl.antucayen.model.service.ServicioExportacionDatos.FormatoExportacion;
import cl.antucayen.model.service.ServicioExportacionDatos;
import cl.antucayen.model.service.ServicioImportacionInventario.FilaCruda;
import cl.antucayen.model.service.ServicioImportacionInventario.ResultadoLectura;
import cl.antucayen.model.service.ServicioImportacionInventario;
import cl.antucayen.model.service.ServicioInventario;
import cl.antucayen.model.service.ServicioProducto;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VAjusteInventario;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ControladorAjusteInventario {

    private final VAjusteInventario vista;
    private final ServicioInventario servicio = new ServicioInventario();
    private final ServicioImportacionInventario servicioImportacion = new ServicioImportacionInventario();
    private final ServicioProducto servicioProducto = new ServicioProducto();
    private final ServicioExportacionDatos servicioExportacion = new ServicioExportacionDatos();
    private final ServicioAuditoriaArchivos auditoriaArchivos = new ServicioAuditoriaArchivos();
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static class ItemPreview {
        String sku, nombre, estado;
        int stockActual, cantidad, stockProyectado, numeroFila;
        boolean esError;
    }

    private enum DecisionDuplicado { CONSOLIDAR, RECHAZAR }

    private List<ItemPreview> itemsCargados = new ArrayList<>();
    private Map<String, List<Integer>> duplicadosPendientes = new LinkedHashMap<>();
    private final List<ErrorImportacion> erroresConsolidados = new ArrayList<>();

    public ControladorAjusteInventario(VAjusteInventario vista) {
        Autorizacion.verificarAjustesInventario();
        this.vista = vista;
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnSeleccionar().addActionListener(e -> seleccionarArchivo());
        vista.getBtnPlantilla().addActionListener(e -> descargarPlantilla());
        vista.getBtnCargar().addActionListener(e -> cargarPreview());
        vista.getBtnConfirmar().addActionListener(e -> confirmarAjuste());
        vista.getBtnResolverDuplicados().addActionListener(e -> resolverDuplicadosPorGrupo());
        vista.getBtnCancelar().addActionListener(e -> cancelarAjuste());
    }

    private void cancelarAjuste() {
        vista.mostrarPaso1();
        vista.limpiarPreview();
        vista.limpiarModalidad();
        vista.limpiarError();
        vista.getPanelErroresEstructura().limpiar();
        itemsCargados.clear();
        duplicadosPendientes.clear();
        erroresConsolidados.clear();
        vista.mostrarAvisoDuplicados(Map.of());
    }

    private void descargarPlantilla() {
        Object[] opciones = {"Excel (.xlsx)", "CSV", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(vista,
                "Selecciona el formato de la plantilla:",
                "Descargar plantilla",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[0]);
        if (seleccion < 0 || seleccion == 2) return;

        FormatoExportacion formato = seleccion == 0
                ? FormatoExportacion.EXCEL : FormatoExportacion.CSV;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar plantilla de importación");
        chooser.setSelectedFile(new File("plantilla_inventario_"
                + LocalDateTime.now().format(FORMATO_ARCHIVO) + "." + formato.getExtension()));
        chooser.setFileFilter(new FileNameExtensionFilter(
                formato.getDescripcion(), formato.getExtension()));
        if (chooser.showSaveDialog(vista) != JFileChooser.APPROVE_OPTION) return;

        try {
            Path archivo = servicioExportacion.generarPlantillaImportacion(
                    chooser.getSelectedFile().toPath(), formato);
            JOptionPane.showMessageDialog(vista,
                    "Plantilla generada correctamente:\n" + archivo.toAbsolutePath());
        } catch (IOException | SQLException | SecurityException ex) {
            vista.mostrarError("No se pudo generar la plantilla: " + ex.getMessage());
        }
    }

    private void seleccionarArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo Excel o CSV");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Excel o CSV (*.xlsx, *.xls, *.csv)", "xlsx", "xls", "csv"));
        chooser.setAcceptAllFileFilterUsed(false);
        int resultado = chooser.showOpenDialog(null);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();
            if (archivo.length() > ServicioImportacionInventario.TAMANO_MAXIMO_BYTES) {
                vista.mostrarError("El archivo supera el tamaño máximo permitido de 10 MB");
                return;
            }
            vista.setNombreArchivo(archivo.getAbsolutePath());
            vista.limpiarError();
            vista.getPanelErroresEstructura().limpiar();
        }
    }

    private void cargarPreview() {
        String ruta = vista.getNombreArchivo();
        if (ruta == null || ruta.isBlank()) {
            vista.mostrarError("Selecciona un archivo primero");
            return;
        }
        String modalidad = vista.getModalidad();
        if (modalidad == null || modalidad.isBlank()) {
            vista.mostrarError("Debes seleccionar una modalidad de ajuste");
            return;
        }

        itemsCargados.clear();
        duplicadosPendientes.clear();
        erroresConsolidados.clear();
        vista.limpiarPreview();
        vista.getPanelErroresEstructura().limpiar();
        vista.mostrarAvisoDuplicados(Map.of());

        boolean correccionAutorizada = vista.isCorreccionAutorizada();
        vista.setProcesandoArchivo(true);
        vista.mostrarError("Leyendo y validando archivo...");

        new SwingWorker<ResultadoLectura, Void>() {
            @Override
            protected ResultadoLectura doInBackground() throws Exception {
                return servicioImportacion.leerYValidar(ruta);
            }

            @Override
            protected void done() {
                vista.setProcesandoArchivo(false);
                try {
                    ResultadoLectura resultado = get();
                    registrarLecturaEnAuditoria(ruta, resultado);
                    procesarResultadoLectura(resultado, modalidad, correccionAutorizada);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    vista.mostrarError("La lectura del archivo fue interrumpida");
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    String detalle = causa == null ? ex.getMessage() : causa.getMessage();
                    registrarErrorLecturaSilencioso(ruta, detalle);
                    vista.mostrarError("Error al leer el archivo: " + detalle);
                } catch (SQLException ex) {
                    vista.mostrarError("No se pudo registrar la bitácora de importación: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void registrarLecturaEnAuditoria(String ruta, ResultadoLectura resultado)
            throws SQLException {
        String nombre = Path.of(ruta).getFileName().toString();
        String formato = nombre.contains(".")
                ? nombre.substring(nombre.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT)
                : "DESCONOCIDO";
        auditoriaArchivos.registrar(nombre, "IMPORTACION", formato,
                resultado.estructuraValida() ? "EXITOSO" : "ERROR",
                resultado.estructuraValida() ? "Archivo leído y validado" : "Estructura inválida");
    }

    private void registrarErrorLecturaSilencioso(String ruta, String detalle) {
        try {
            String nombre = Path.of(ruta).getFileName().toString();
            auditoriaArchivos.registrar(
                    nombre, "IMPORTACION", "DESCONOCIDO", "ERROR", detalle);
        } catch (Exception ignored) {
            // El error principal de lectura no se reemplaza por un fallo de bitácora.
        }
    }

    private void procesarResultadoLectura(ResultadoLectura resultado,
                                          String modalidad,
                                          boolean correccionAutorizada) {

        erroresConsolidados.addAll(resultado.erroresLectura());
        if (!resultado.estructuraValida()) {
            vista.mostrarError("Archivo con estructura inválida, revisa el detalle abajo");
            vista.getPanelErroresEstructura().cargarErrores(erroresConsolidados);
            return;
        }

        for (FilaCruda fila : resultado.filas()) {
            ItemPreview item = new ItemPreview();
            item.numeroFila = fila.numeroFila();
            item.sku = fila.sku();

            int cantidad;
            try {
                cantidad = Integer.parseInt(fila.cantidadTexto());
            } catch (NumberFormatException ex) {
                item.estado = "ERROR: cantidad no numérica";
                item.esError = true;
                itemsCargados.add(item);
                erroresConsolidados.add(new ErrorImportacion(
                        fila.numeroFila(), "cantidad", fila.sku(),
                        "Cantidad no numérica: '" + fila.cantidadTexto() + "'"));
                continue;
            }
            item.cantidad = cantidad;

            if (cantidad == 0) {
                item.estado = "ERROR: cantidad cero no permitida";
                item.esError = true;
                itemsCargados.add(item);
                erroresConsolidados.add(new ErrorImportacion(
                        fila.numeroFila(), "cantidad", fila.sku(), "Cantidad no puede ser cero"));
                continue;
            }
            if (cantidad < 0 && !correccionAutorizada) {
                item.estado = "ERROR: negativo (requiere Corrección autorizada)";
                item.esError = true;
                itemsCargados.add(item);
                erroresConsolidados.add(new ErrorImportacion(
                        fila.numeroFila(), "cantidad", fila.sku(),
                        "Cantidad negativa sin autorización de Administrador"));
                continue;
            }

            try {
                Producto p = servicioProducto.buscarPorSku(fila.sku());
                if (p == null) {
                    item.estado = "ERROR: SKU no encontrado en el sistema";
                    item.esError = true;
                    itemsCargados.add(item);
                    erroresConsolidados.add(new ErrorImportacion(
                            fila.numeroFila(), "SKU", fila.sku(), "SKU no encontrado en el sistema"));
                    continue;
                }
                if (!EstadoProducto.ACTIVO.coincide(p.getEstado())) {
                    item.estado = "ERROR: producto Inactivo";
                    item.esError = true;
                    itemsCargados.add(item);
                    erroresConsolidados.add(new ErrorImportacion(
                            fila.numeroFila(), "SKU", fila.sku(),
                            "El producto está Inactivo y no puede usarse en nuevas importaciones"));
                    continue;
                }

                item.nombre = p.getNombre();
                item.stockActual = p.getStockActual();
                try {
                    item.stockProyectado = calcularStockProyectado(
                            item.stockActual, cantidad, modalidad);
                } catch (ArithmeticException ex) {
                    item.estado = "ERROR: stock fuera de rango";
                    item.esError = true;
                    itemsCargados.add(item);
                    erroresConsolidados.add(new ErrorImportacion(
                            fila.numeroFila(), "cantidad", fila.sku(),
                            "El stock proyectado excede el rango permitido"));
                    continue;
                }
                item.estado = item.stockProyectado < 0 ? "ADVERTENCIA: stock negativo" : "OK";
                itemsCargados.add(item);
            } catch (SQLException ex) {
                item.estado = "ERROR: error de base de datos";
                item.esError = true;
                itemsCargados.add(item);
                erroresConsolidados.add(new ErrorImportacion(
                        fila.numeroFila(), "procesamiento", fila.sku(),
                        "Error de BD: " + ex.getMessage()));
            }
        }

        marcarDuplicados();
        refrescarTabla();

        if (!erroresConsolidados.isEmpty()) {
            vista.mostrarError(erroresConsolidados.size() + " error(es) encontrados");
        } else {
            vista.limpiarError();
        }
        vista.getPanelErroresEstructura().cargarErrores(erroresConsolidados);
        vista.mostrarPaso2();
    }

    private void marcarDuplicados() {
        List<FilaCruda> filasValidas = itemsCargados.stream()
                .filter(it -> !it.esError)
                .map(it -> new FilaCruda(it.numeroFila, it.sku, String.valueOf(it.cantidad)))
                .toList();
        duplicadosPendientes = new LinkedHashMap<>(servicioImportacion.detectarDuplicados(filasValidas));

        for (Map.Entry<String, List<Integer>> entrada : duplicadosPendientes.entrySet()) {
            String sku = entrada.getKey();
            List<Integer> filas = entrada.getValue();
            String descripcion = "SKU duplicado: aparece en " + filas.size()
                    + " filas (" + String.join(", ", filas.stream().map(String::valueOf).toList()) + ")";
            for (Integer fila : filas) {
                erroresConsolidados.add(new ErrorImportacion(fila, "SKU", sku, descripcion));
            }
        }

        for (ItemPreview it : itemsCargados) {
            if (it.esError) continue;
            String skuNormalizado = normalizarSku(it.sku);
            List<Integer> filas = duplicadosPendientes.get(skuNormalizado);
            if (filas != null) {
                it.estado = "DUPLICADO (" + filas.size() + " filas: "
                        + String.join(", ", filas.stream().map(String::valueOf).toList()) + ")";
            }
        }
        vista.mostrarAvisoDuplicados(duplicadosPendientes);
    }

    /** Permite decidir Consolidar o Rechazar para cada grupo de SKU duplicado. */
    private void resolverDuplicadosPorGrupo() {
        if (duplicadosPendientes.isEmpty()) return;

        Map<String, DecisionDuplicado> decisiones = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entrada : duplicadosPendientes.entrySet()) {
            String sku = entrada.getKey();
            List<Integer> filas = entrada.getValue();
            Object[] opciones = {"Consolidar", "Rechazar grupo", "Cancelar"};
            int seleccion = JOptionPane.showOptionDialog(
                    vista,
                    "El SKU " + sku + " aparece en " + filas.size() + " filas: " + filas
                            + ".\nSelecciona cómo resolver este grupo.",
                    "Resolver SKU duplicado",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]);
            if (seleccion == 2 || seleccion == JOptionPane.CLOSED_OPTION) return;
            decisiones.put(sku,
                    seleccion == 0 ? DecisionDuplicado.CONSOLIDAR : DecisionDuplicado.RECHAZAR);
        }

        Map<String, List<ItemPreview>> grupos = new LinkedHashMap<>();
        List<ItemPreview> nuevaLista = new ArrayList<>();
        for (ItemPreview it : itemsCargados) {
            String clave = it.sku == null ? "" : normalizarSku(it.sku);
            if (!it.esError && duplicadosPendientes.containsKey(clave)) {
                grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(it);
            } else {
                nuevaLista.add(it);
            }
        }

        for (Map.Entry<String, List<ItemPreview>> entrada : grupos.entrySet()) {
            if (decisiones.get(entrada.getKey()) == DecisionDuplicado.RECHAZAR) continue;

            List<ItemPreview> grupo = entrada.getValue();
            ItemPreview base = grupo.get(0);
            int total = 0;
            try {
                for (ItemPreview item : grupo) total = Math.addExact(total, item.cantidad);
                base.cantidad = total;
                base.stockProyectado = calcularStockProyectado(
                        base.stockActual, total, vista.getModalidad());
                base.estado = base.stockProyectado < 0
                        ? "ADVERTENCIA: stock negativo"
                        : "OK (consolidado)";
            } catch (ArithmeticException ex) {
                base.esError = true;
                base.estado = "ERROR: cantidad consolidada fuera de rango";
                erroresConsolidados.add(new ErrorImportacion(
                        base.numeroFila, "cantidad", base.sku,
                        "La cantidad consolidada excede el rango permitido"));
            }
            nuevaLista.add(base);
        }

        itemsCargados = nuevaLista;
        duplicadosPendientes.clear();
        vista.mostrarAvisoDuplicados(Map.of());
        vista.getPanelErroresEstructura().cargarErrores(erroresConsolidados);
        refrescarTabla();
    }

    private String normalizarSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private int calcularStockProyectado(int stockActual, int cantidad, String modalidad) {
        if (ServicioInventario.MODALIDAD_SUMAR.equals(modalidad)) {
            return Math.addExact(stockActual, cantidad);
        }
        if (ServicioInventario.MODALIDAD_REEMPLAZAR.equals(modalidad)) {
            return cantidad;
        }
        throw new IllegalArgumentException("Debes seleccionar una modalidad de ajuste válida");
    }

    private void refrescarTabla() {
        vista.limpiarPreview();
        for (ItemPreview it : itemsCargados) {
            vista.agregarFilaPreview(new Object[]{
                    it.sku,
                    it.nombre == null ? "" : it.nombre,
                    it.stockActual,
                    it.cantidad,
                    it.stockProyectado,
                    it.estado
            });
        }
    }

    private void confirmarAjuste() {
        String modalidad = vista.getModalidad();
        if (modalidad == null || modalidad.isBlank()) {
            JOptionPane.showMessageDialog(vista, "Debes seleccionar una modalidad de ajuste");
            return;
        }
        if (!duplicadosPendientes.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Debes resolver todos los SKU duplicados antes de confirmar");
            return;
        }

        long errores = itemsCargados.stream().filter(i -> i.esError).count();
        long advertencias = itemsCargados.stream()
                .filter(i -> i.estado != null && i.estado.startsWith("ADVERTENCIA")).count();
        if (errores > 0 || advertencias > 0) {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Hay " + (errores + advertencias)
                            + " ítem(s) con error o advertencia. ¿Continuar solo con los válidos?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        boolean correccionAutorizada = vista.isCorreccionAutorizada();
        try {
            List<ServicioInventario.SolicitudAjuste> solicitudes = itemsCargados.stream()
                    .filter(item -> item.estado != null && item.estado.startsWith("OK"))
                    .map(item -> new ServicioInventario.SolicitudAjuste(item.sku, item.cantidad))
                    .toList();
            int aplicados = servicio.aplicarAjuste(modalidad, correccionAutorizada, solicitudes);
            JOptionPane.showMessageDialog(null,
                    "Ajuste aplicado correctamente a " + aplicados + " producto(s).");
            cancelarAjuste();
        } catch (SQLException | IllegalArgumentException | SecurityException ex) {
            JOptionPane.showMessageDialog(null, "Error al aplicar ajuste: " + ex.getMessage());
        }
    }
}
