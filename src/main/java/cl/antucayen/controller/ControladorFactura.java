package cl.antucayen.controller;

import cl.antucayen.model.domain.EstadoFactura;
import cl.antucayen.model.domain.EstadoItemFactura;
import cl.antucayen.model.dto.ResumenProcesamiento;
import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.model.service.ServicioArchivoFactura;
import cl.antucayen.model.service.ServicioExportacionDatos.FormatoExportacion;
import cl.antucayen.model.service.ServicioExportacionDatos;
import cl.antucayen.model.service.ServicioExtraccionFacturaDigital;
import cl.antucayen.model.service.ServicioFactura;
import cl.antucayen.model.service.ServicioProcesamientoFactura;
import cl.antucayen.model.service.ServicioProveedor;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.security.SesionActual;
import cl.antucayen.view.VDetalleFactura;
import cl.antucayen.view.VFacturas;
import cl.antucayen.view.VFormularioFactura;
import cl.antucayen.view.VProcesamientoFactura;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ControladorFactura {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter NOMBRE_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ServicioFactura servicio = new ServicioFactura();
    private final ServicioExportacionDatos servicioExportacion = new ServicioExportacionDatos();
    private final ServicioProveedor servicioProveedor = new ServicioProveedor();
    private final ServicioArchivoFactura servicioArchivo = new ServicioArchivoFactura();
    private final ServicioExtraccionFacturaDigital servicioExtraccion =
            new ServicioExtraccionFacturaDigital();
    private final ServicioProcesamientoFactura servicioProcesamiento =
            new ServicioProcesamientoFactura();

    private final VFacturas vista;

    public ControladorFactura(VFacturas vista) {
        Autorizacion.verificarGestionFacturas();
        this.vista = vista;
        cargarProveedoresEnFiltro();
        cargarTodas();
        iniciarEventos();
    }

    public ControladorFactura() {
        Autorizacion.verificarGestionFacturas();
        this.vista = null;
    }

    private void iniciarEventos() {
        vista.getBtnBuscar().addActionListener(e -> buscar());

        vista.getBtnLimpiar().addActionListener(e -> {
            vista.limpiarFiltros();
            cargarTodas();
        });

        vista.getBtnNueva().addActionListener(e -> abrirNuevaFactura());

        vista.getTblFacturas().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) abrirDetalle();
            }
        });
    }

    private void cargarProveedoresEnFiltro() {
        try {
            vista.cargarProveedores(servicioProveedor.listarTodos());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void cargarTodas() {
        if (vista == null) return;

        try {
            vista.limpiarTabla();

            for (Factura f : servicio.listarTodas()) {
                vista.agregarFila(new Object[]{
                        f.getIdFactura(),
                        f.getNumeroFactura(),
                        FORMATO_FECHA.format(f.getFechaEmision()),
                        f.getNombreProveedor(),
                        f.getEstado()
                });
            }

        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void buscar() {
        try {
            String numero = vista.getFiltroNumero();
            int idProveedor = vista.getFiltroIdProveedor();
            LocalDate desde = parsearFecha(vista.getFiltroDesde());
            LocalDate hasta = parsearFecha(vista.getFiltroHasta());
            String estado = vista.getFiltroEstado();

            List<Factura> resultados = servicio.consultar(
                    numero,
                    idProveedor > 0 ? idProveedor : null,
                    desde,
                    hasta
            );

            vista.limpiarTabla();

            for (Factura f : resultados) {
                if (!"Todos".equals(estado) && !f.getEstado().equals(estado)) continue;

                vista.agregarFila(new Object[]{
                        f.getIdFactura(),
                        f.getNumeroFactura(),
                        FORMATO_FECHA.format(f.getFechaEmision()),
                        f.getNombreProveedor(),
                        f.getEstado()
                });
            }

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(
                    vista,
                    "Formato de fecha inválido, usa dd-mm-aaaa"
            );

        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return LocalDate.parse(texto.trim(), FORMATO_FECHA);
    }

    public void abrirNuevaFactura() {
        abrirNuevaFactura(null);
    }

    /**
     * Abre el formulario de registro y, cuando la factura se guarda, ejecuta
     * opcionalmente un callback para refrescar/navegar la pantalla que lo abrió.
     */
    public void abrirNuevaFactura(Runnable alGuardar) {
        try {
            Autorizacion.verificarGestionFacturas();

            VFormularioFactura form = new VFormularioFactura(null);

            form.cargarProveedores(servicioProveedor.listarTodos());

            form.getBtnSeleccionarArchivo()
                    .addActionListener(e -> seleccionarArchivo(form));

            form.getBtnVistaPrevia()
                    .addActionListener(e -> mostrarVistaPrevia(form));

            form.getBtnExtraerItems()
                    .addActionListener(e -> extraerItemsAsync(form, true, null));

            form.getBtnGuardar()
                    .addActionListener(e -> guardarFactura(form, alGuardar));

            form.setVisible(true);

        } catch (SQLException | SecurityException ex) {
            mostrarError(new SQLException(ex.getMessage(), ex));
        }
    }

    private void seleccionarArchivo(VFormularioFactura form) {
        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Seleccionar factura digital");

        chooser.setFileFilter(new FileNameExtensionFilter(
                "Facturas PDF/JPG/PNG",
                "pdf",
                "jpg",
                "jpeg",
                "png"
        ));

        if (chooser.showOpenDialog(form) != JFileChooser.APPROVE_OPTION) return;

        try {
            File archivo = chooser.getSelectedFile();

            servicioArchivo.validar(archivo);

            form.setArchivoSeleccionado(archivo);
            form.limpiarError();

        } catch (IllegalArgumentException ex) {
            form.mostrarError(ex.getMessage());
        }
    }

    private void mostrarVistaPrevia(VFormularioFactura form) {
        File archivo = form.getArchivoSeleccionado();

        if (archivo == null) {
            form.mostrarError("Debes seleccionar un archivo digital");
            return;
        }

        form.setProcesandoDocumento(true);
        form.limpiarError();

        new SwingWorker<PreviewFactura, Void>() {
            @Override
            protected PreviewFactura doInBackground() throws Exception {
                BufferedImage imagen = servicioExtraccion.renderizarVistaPrevia(archivo);
                int maxAncho = 900;
                int maxAlto = 650;
                double escala = Math.min(1d, Math.min(
                        (double) maxAncho / imagen.getWidth(),
                        (double) maxAlto / imagen.getHeight()));
                int ancho = Math.max(1, (int) Math.round(imagen.getWidth() * escala));
                int alto = Math.max(1, (int) Math.round(imagen.getHeight() * escala));
                Image escalada = imagen.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                return new PreviewFactura(new ImageIcon(escalada), ancho, alto, maxAncho, maxAlto);
            }

            @Override
            protected void done() {
                if (!form.isDisplayable()) return;
                form.setProcesandoDocumento(false);
                try {
                    PreviewFactura preview = get();
                    JLabel etiqueta = new JLabel(preview.icono());
                    JScrollPane scroll = new JScrollPane(etiqueta);
                    scroll.setPreferredSize(new Dimension(
                            Math.min(preview.maxAncho() + 30, preview.ancho() + 30),
                            Math.min(preview.maxAlto() + 30, preview.alto() + 30)));
                    JOptionPane.showMessageDialog(
                            form,
                            scroll,
                            "Vista previa - " + archivo.getName(),
                            JOptionPane.PLAIN_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    form.mostrarError("La vista previa fue interrumpida");
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    form.mostrarError("No se pudo generar la vista previa: "
                            + (causa == null ? ex.getMessage() : causa.getMessage()));
                }
            }
        }.execute();
    }

    private void extraerItemsAsync(VFormularioFactura form,
                                   boolean informar,
                                   Runnable alCompletar) {
        File archivo = form.getArchivoSeleccionado();

        if (archivo == null) {
            form.mostrarError("Debes seleccionar un archivo digital");
            return;
        }

        form.setProcesandoDocumento(true);
        form.limpiarError();

        new SwingWorker<ServicioExtraccionFacturaDigital.ResultadoExtraccion, Void>() {
            @Override
            protected ServicioExtraccionFacturaDigital.ResultadoExtraccion doInBackground()
                    throws Exception {
                return servicioExtraccion.analizar(archivo);
            }

            @Override
            protected void done() {
                if (!form.isDisplayable()) return;
                form.setProcesandoDocumento(false);
                try {
                    ServicioExtraccionFacturaDigital.ResultadoExtraccion resultado = get();
                    aplicarResultadoExtraccion(form, resultado, informar);
                    if (alCompletar != null) alCompletar.run();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    form.mostrarError("El análisis fue interrumpido");
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause();
                    form.mostrarError("Error de análisis: "
                            + (causa == null ? ex.getMessage() : causa.getMessage()));
                }
            }
        }.execute();
    }

    private void aplicarResultadoExtraccion(
            VFormularioFactura form,
            ServicioExtraccionFacturaDigital.ResultadoExtraccion resultado,
            boolean informar) {
        form.cargarDatosExtraidos(
                resultado.numeroFactura(),
                resultado.fechaEmision(),
                resultado.valorTotal());
        form.cargarItemsExtraidos(resultado.items());
        form.marcarAnalisisRealizado();
        form.limpiarError();

        if (!informar) return;

        long noProcesados = resultado.items().stream()
                .filter(i -> EstadoItemFactura.NO_PROCESADO.coincide(i.estado()))
                .count();
        String folio = resultado.numeroFactura() == null
                ? "No detectado" : resultado.numeroFactura();
        String fecha = resultado.fechaEmision() == null
                ? "No detectada" : FORMATO_FECHA.format(resultado.fechaEmision());
        String total = resultado.valorTotal() == null
                ? "No detectado" : "$" + formatearMonto(resultado.valorTotal());

        JOptionPane.showMessageDialog(
                form,
                "Análisis terminado."
                        + "\nFolio detectado: " + folio
                        + "\nFecha de emisión detectada: " + fecha
                        + "\nTotal detectado: " + total
                        + "\nÍtems detectados: " + resultado.items().size()
                        + (noProcesados > 0
                        ? "\nLos registros no legibles quedan marcados como No Procesado."
                        : "")
                        + "\n\nRevisa los valores antes de guardar; todos los campos siguen siendo editables.");
    }

    private record PreviewFactura(ImageIcon icono,
                                  int ancho,
                                  int alto,
                                  int maxAncho,
                                  int maxAlto) { }

    private void guardarFactura(VFormularioFactura form, Runnable alGuardar) {
        String copiaGuardada = null;
        boolean facturaRegistrada = false;

        try {
            if (form.esModalidadDigital()) {
                if (form.getArchivoSeleccionado() == null) {
                    throw new IllegalArgumentException(
                            "En modalidad digital debes adjuntar un archivo PDF, JPG o PNG"
                    );
                }

                // Si el usuario pulsa Guardar sin analizar previamente, se analiza
                // una vez para intentar completar folio, total e ítems.
                if (!form.isAnalisisRealizado() || form.getModeloItems().getRowCount() == 0) {
                    extraerItemsAsync(form, false, () -> guardarFactura(form, alGuardar));
                    return;
                }
            }

            LocalDate fecha =
                    LocalDate.parse(
                            form.getFechaTexto().trim(),
                            FORMATO_FECHA
                    );

            int valorTotal =
                    Integer.parseInt(
                            form.getValorTotalTexto()
                    );

            if (form.esModalidadDigital()) {
                copiaGuardada =
                        servicioArchivo.guardarCopia(
                                form.getArchivoSeleccionado()
                        );
            }

            Factura factura = new Factura();

            factura.setNumeroFactura(form.getNumero());
            factura.setFechaEmision(fecha);
            factura.setIdProveedor(form.getIdProveedorSeleccionado());
            factura.setValorTotal(valorTotal);
            factura.setRutaArchivoDigital(copiaGuardada);

            List<ItemFactura> items =
                    leerItemsDelFormulario(form);

            int idFactura =
                    servicio.registrar(factura, items);

            facturaRegistrada = true;

            ResumenProcesamiento resumen =
                    servicioProcesamiento.resolverEquivalenciasAlRegistrar(
                            idFactura,
                            factura.getIdProveedor()
                    );

            form.dispose();
            cargarTodas();
            ejecutarCallback(alGuardar);

            JOptionPane.showMessageDialog(
                    vista,
                    "Factura registrada correctamente.\n\n"
                            + "Ítems leídos: " + resumen.leidos()
                            + "\nSKU resueltos: " + resumen.validos()
                            + "\nObservados: " + resumen.observados()
                            + "\nNo procesados: " + resumen.noProcesados()
            );

        } catch (DateTimeParseException ex) {
            if (!facturaRegistrada) {
                servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            }

            form.mostrarError(
                    "Fecha inválida, usa el formato dd-mm-aaaa"
            );

        } catch (NumberFormatException ex) {
            if (!facturaRegistrada) {
                servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            }

            form.mostrarError(
                    "Valor total, cantidad o precio unitario inválido en algún ítem"
            );

        } catch (IllegalArgumentException
                 | IllegalStateException
                 | SecurityException ex) {

            if (!facturaRegistrada) {
                servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            }

            form.mostrarError(ex.getMessage());

        } catch (IOException ex) {
            if (!facturaRegistrada) {
                servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            }

            form.mostrarError(
                    "No se pudo almacenar el archivo digital: "
                            + ex.getMessage()
            );

        } catch (SQLException ex) {
            if (!facturaRegistrada) {
                servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);

                form.mostrarError(
                        "Error al guardar: " + ex.getMessage()
                );

            } else {
                form.dispose();
                cargarTodas();
                ejecutarCallback(alGuardar);

                JOptionPane.showMessageDialog(
                        vista,
                        "La factura fue registrada, pero no fue posible "
                                + "resolver automáticamente los SKU.\n\n"
                                + "Detalle: " + ex.getMessage()
                                + "\n\nPuedes abrir la factura y usar "
                                + "'Procesar Ítems' para reintentar.",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }

    private List<ItemFactura> leerItemsDelFormulario(VFormularioFactura form) {
        DefaultTableModel modelo = form.getModeloItems();

        if (modelo.getRowCount() == 0) {
            throw new IllegalArgumentException(
                    "La factura debe tener al menos un ítem"
            );
        }

        List<ItemFactura> items = new ArrayList<>();

        for (int i = 0; i < modelo.getRowCount(); i++) {
            String codigo =
                    valor(modelo.getValueAt(i, 0));

            String descripcion =
                    valor(modelo.getValueAt(i, 1));

            String textoCantidad =
                    valor(modelo.getValueAt(i, 2));

            String textoPrecio =
                    valor(modelo.getValueAt(i, 3));

            String estado =
                    valor(modelo.getValueAt(i, 4));

            if (estado.isBlank()) {
                estado = codigo.isBlank()
                        ? EstadoItemFactura.NO_PROCESADO.valorDb()
                        : EstadoItemFactura.OBSERVADO.valorDb();
            }

            int cantidad =
                    textoCantidad.isBlank()
                            && EstadoItemFactura.NO_PROCESADO.coincide(estado)
                            ? 0
                            : Integer.parseInt(textoCantidad);

            int precioUnitario = textoPrecio.isBlank()
                    ? 0
                    : Integer.parseInt(textoPrecio.replaceAll("[^0-9]", ""));
            if (precioUnitario < 0) {
                throw new IllegalArgumentException(
                        "El precio unitario no puede ser negativo"
                );
            }

            if (EstadoItemFactura.NO_PROCESADO.coincide(estado)) {
                if (cantidad < 0) {
                    throw new IllegalArgumentException(
                            "La cantidad no puede ser negativa"
                    );
                }

            } else {
                if (codigo.isBlank()) {
                    throw new IllegalArgumentException(
                            "El código interno del proveedor es obligatorio en los ítems legibles"
                    );
                }

                if (cantidad <= 0) {
                    throw new IllegalArgumentException(
                            "La cantidad de cada ítem legible debe ser mayor a cero"
                    );
                }

                estado = EstadoItemFactura.OBSERVADO.valorDb();
            }

            ItemFactura item = new ItemFactura();

            item.setCodigoInternoProveedor(
                    codigo.isBlank() ? null : codigo
            );

            item.setDescripcion(
                    descripcion.isBlank() ? null : descripcion
            );

            item.setCantidadFacturada(cantidad);
            item.setPrecioUnitarioCompra(precioUnitario);
            item.setEstadoItem(estado);

            items.add(item);
        }

        return items;
    }

    private String valor(Object o) {
        return o == null
                ? ""
                : String.valueOf(o).trim();
    }

    private void abrirDetalle() {
        int fila =
                vista.getTblFacturas()
                        .getSelectedRow();

        if (fila < 0) return;

        int idFactura =
                (int) vista.getModeloTabla()
                        .getValueAt(fila, 0);

        try {
            Factura f =
                    servicio.buscarPorId(idFactura);

            if (f == null) return;

            VDetalleFactura detalle =
                    new VDetalleFactura(null);

            refrescarDetalle(detalle, f);

            detalle.getBtnProcesar()
                    .addActionListener(
                            e -> cambiarEstado(
                                    idFactura,
                                    EstadoFactura.PROCESADA.valorDb(),
                                    detalle
                            )
                    );

            detalle.getBtnObservar()
                    .addActionListener(
                            e -> cambiarEstado(
                                    idFactura,
                                    EstadoFactura.OBSERVADA.valorDb(),
                                    detalle
                            )
                    );

            detalle.getBtnProcesarItems()
                    .addActionListener(
                            e -> abrirProcesamiento(
                                    idFactura,
                                    f.getIdProveedor(),
                                    detalle
                            )
                    );

            detalle.getBtnAbrirArchivo()
                    .addActionListener(
                            e -> abrirArchivoAdjunto(
                                    detalle
                            )
                    );

            detalle.getBtnExportarValidos()
                    .addActionListener(
                            e -> exportarItemsValidos(
                                    idFactura,
                                    detalle
                            )
                    );

            detalle.setVisible(true);

        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void exportarItemsValidos(int idFactura, VDetalleFactura detalle) {
        try {
            List<ItemFactura> items = servicio.obtenerItems(idFactura);
            Object[] opciones = {"Excel (.xlsx)", "CSV", "Cancelar"};
            int seleccion = JOptionPane.showOptionDialog(
                    detalle,
                    "Selecciona el formato de exportación:",
                    "Exportar ítems válidos",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );
            if (seleccion < 0 || seleccion == 2) return;

            FormatoExportacion formato = seleccion == 0
                    ? FormatoExportacion.EXCEL
                    : FormatoExportacion.CSV;

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Guardar ítems válidos");
            chooser.setSelectedFile(new File(
                    "items_validos_factura_" + idFactura + "_"
                            + LocalDateTime.now().format(NOMBRE_ARCHIVO)
                            + "." + formato.getExtension()
            ));
            chooser.setFileFilter(new FileNameExtensionFilter(
                    formato.getDescripcion(),
                    formato.getExtension()
            ));
            if (chooser.showSaveDialog(detalle) != JFileChooser.APPROVE_OPTION) return;

            Path archivo = servicioExportacion.exportarItemsValidosFactura(
                    items,
                    chooser.getSelectedFile().toPath(),
                    formato
            );
            JOptionPane.showMessageDialog(
                    detalle,
                    "Ítems válidos exportados correctamente:\n" + archivo.toAbsolutePath()
            );
        } catch (IllegalArgumentException | SecurityException ex) {
            JOptionPane.showMessageDialog(detalle, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    detalle,
                    "No se pudo generar el archivo: " + ex.getMessage()
            );
        }
    }

    private void ejecutarCallback(Runnable callback) {
        if (callback == null) return;
        try {
            callback.run();
        } catch (RuntimeException ex) {
            // La factura ya fue persistida; un fallo visual de refresco no debe
            // convertir el registro exitoso en un error de negocio.
        }
    }

    private String formatearMonto(int monto) {
        return String.format("%,d", monto).replace(',', '.');
    }

    private void abrirArchivoAdjunto(VDetalleFactura detalle) {
        String ruta = detalle.getRutaArchivo();

        if (ruta == null || ruta.isBlank()) {
            JOptionPane.showMessageDialog(
                    detalle,
                    "La factura no tiene archivo adjunto"
            );
            return;
        }

        File archivo = new File(ruta);

        if (!archivo.isFile()) {
            JOptionPane.showMessageDialog(
                    detalle,
                    "El archivo adjunto no existe en esta ubicación:\n"
                            + ruta
            );
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(
                    detalle,
                    "Este equipo no permite abrir archivos con la aplicación predeterminada"
            );
            return;
        }

        try {
            Desktop.getDesktop().open(archivo);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    detalle,
                    "No se pudo abrir el archivo adjunto: "
                            + ex.getMessage()
            );
        }
    }

    private void abrirProcesamiento(
            int idFactura,
            int idProveedor,
            VDetalleFactura detalle) {

        VProcesamientoFactura vistaProc =
                new VProcesamientoFactura(null);

        new ControladorProcesamientoFactura(
                vistaProc,
                idFactura,
                idProveedor
        );

        vistaProc.setVisible(true);

        try {
            Factura facturaActualizada =
                    servicio.buscarPorId(idFactura);

            if (facturaActualizada == null) return;

            refrescarDetalle(detalle, facturaActualizada);

        } catch (SQLException ex) {
            mostrarError(ex);
        }

        cargarTodas();
    }

    /**
     * Recarga el contenido y los permisos visuales del detalle desde una
     * factura ya consultada. Centraliza la misma regla usada al abrir el
     * detalle y al volver desde el procesamiento de ítems.
     */
    private void refrescarDetalle(VDetalleFactura detalle, Factura factura)
            throws SQLException {
        detalle.cargarCabecera(factura);
        detalle.cargarItems(servicio.obtenerItems(factura.getIdFactura()));

        boolean puedeGestionar =
                SesionActual.esAdministrador() || SesionActual.esBodeguero();
        boolean noProcesada = !EstadoFactura.PROCESADA.coincide(factura.getEstado());

        detalle.getBtnProcesar().setEnabled(puedeGestionar && noProcesada);
        detalle.getBtnObservar().setEnabled(puedeGestionar && noProcesada);
        detalle.getBtnProcesarItems().setEnabled(puedeGestionar);
    }

    private void cambiarEstado(
            int idFactura,
            String nuevoEstado,
            VDetalleFactura detalle) {

        int confirmar =
                JOptionPane.showConfirmDialog(
                        detalle,
                        "¿Confirmas cambiar el estado de la factura a '"
                                + nuevoEstado
                                + "'?"
                                + (EstadoFactura.PROCESADA.coincide(nuevoEstado)
                                ? "\nEsto ingresará el stock únicamente de los ítems validados."
                                : ""),
                        "Confirmar cambio de estado",
                        JOptionPane.YES_NO_OPTION
                );

        if (confirmar != JOptionPane.YES_OPTION) return;

        try {
            servicio.cambiarEstado(
                    idFactura,
                    nuevoEstado
            );

            detalle.dispose();

            cargarTodas();

            JOptionPane.showMessageDialog(
                    vista,
                    "Estado actualizado a "
                            + nuevoEstado
            );

        } catch (IllegalStateException
                 | IllegalArgumentException
                 | SecurityException ex) {

            JOptionPane.showMessageDialog(
                    detalle,
                    ex.getMessage()
            );

        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(SQLException ex) {
        JOptionPane.showMessageDialog(
                vista,
                "Error: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
