package cl.antucayen.controller;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.model.service.ServicioArchivoFactura;
import cl.antucayen.model.service.ServicioExtraccionFacturaDigital;
import cl.antucayen.model.service.ServicioFactura;
import cl.antucayen.model.service.ServicioProveedor;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.SesionActual;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

public class ControladorFactura {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final ServicioFactura servicio = new ServicioFactura();
    private final ServicioProveedor servicioProveedor = new ServicioProveedor();
    private final ServicioArchivoFactura servicioArchivo = new ServicioArchivoFactura();
    private final ServicioExtraccionFacturaDigital servicioExtraccion =
            new ServicioExtraccionFacturaDigital();
    private final VFacturas vista;

    public ControladorFactura(VFacturas vista) {
        Autorizacion.verificarGestionFacturas();
        this.vista = vista;
        vista.setModoSoloLectura(false);
        cargarProveedoresEnFiltro();
        cargarTodas();
        iniciarEventos();
    }

    /** Abre directamente el registro de factura sin una lista de fondo. */
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
                        f.getIdFactura(), f.getNumeroFactura(), FORMATO_FECHA.format(f.getFechaEmision()),
                        f.getNombreProveedor(), f.getEstado()
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
                    numero, idProveedor > 0 ? idProveedor : null, desde, hasta);

            vista.limpiarTabla();
            for (Factura f : resultados) {
                if (!"Todos".equals(estado) && !f.getEstado().equals(estado)) continue;
                vista.agregarFila(new Object[]{
                        f.getIdFactura(), f.getNumeroFactura(), FORMATO_FECHA.format(f.getFechaEmision()),
                        f.getNombreProveedor(), f.getEstado()
                });
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(vista, "Formato de fecha inválido, usa dd-mm-aaaa");
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return LocalDate.parse(texto.trim(), FORMATO_FECHA);
    }

    public void abrirNuevaFactura() {
        try {
            Autorizacion.verificarGestionFacturas();
            VFormularioFactura form = new VFormularioFactura(null);
            form.cargarProveedores(servicioProveedor.listarTodos());
            form.getBtnSeleccionarArchivo().addActionListener(e -> seleccionarArchivo(form));
            form.getBtnVistaPrevia().addActionListener(e -> mostrarVistaPrevia(form));
            form.getBtnExtraerItems().addActionListener(e -> extraerItems(form, true));
            form.getBtnGuardar().addActionListener(e -> guardarFactura(form));
            form.setVisible(true);
        } catch (SQLException | SecurityException ex) {
            mostrarError(new SQLException(ex.getMessage(), ex));
        }
    }

    private void seleccionarArchivo(VFormularioFactura form) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar factura digital");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Facturas PDF/JPG/PNG", "pdf", "jpg", "jpeg", "png"));
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
        try {
            BufferedImage imagen = servicioExtraccion.renderizarVistaPrevia(archivo);
            int maxAncho = 900;
            int maxAlto = 650;
            double escala = Math.min(1d, Math.min(
                    (double) maxAncho / imagen.getWidth(), (double) maxAlto / imagen.getHeight()));
            int ancho = Math.max(1, (int) Math.round(imagen.getWidth() * escala));
            int alto = Math.max(1, (int) Math.round(imagen.getHeight() * escala));
            Image escalada = imagen.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
            JLabel etiqueta = new JLabel(new ImageIcon(escalada));
            JScrollPane scroll = new JScrollPane(etiqueta);
            scroll.setPreferredSize(new Dimension(Math.min(maxAncho + 30, ancho + 30),
                    Math.min(maxAlto + 30, alto + 30)));
            JOptionPane.showMessageDialog(form, scroll,
                    "Vista previa - " + archivo.getName(), JOptionPane.PLAIN_MESSAGE);
        } catch (IOException | IllegalArgumentException ex) {
            form.mostrarError("No se pudo generar la vista previa: " + ex.getMessage());
        }
    }

    private boolean extraerItems(VFormularioFactura form, boolean informar) {
        File archivo = form.getArchivoSeleccionado();
        if (archivo == null) {
            form.mostrarError("Debes seleccionar un archivo digital");
            return false;
        }
        try {
            var items = servicioExtraccion.extraer(archivo);
            form.cargarItemsExtraidos(items);
            form.limpiarError();
            if (informar) {
                long noProcesados = items.stream()
                        .filter(i -> "No Procesado".equals(i.estado())).count();
                JOptionPane.showMessageDialog(form,
                        "Extracción terminada: " + items.size() + " ítem(s)."
                                + (noProcesados > 0
                                ? "\nLos registros no legibles quedan marcados como No Procesado." : ""));
            }
            return true;
        } catch (IOException | IllegalArgumentException ex) {
            form.mostrarError("Error de extracción: " + ex.getMessage());
            return false;
        }
    }

    private void guardarFactura(VFormularioFactura form) {
        String copiaGuardada = null;
        try {
            LocalDate fecha = LocalDate.parse(form.getFechaTexto().trim(), FORMATO_FECHA);
            int valorTotal = Integer.parseInt(form.getValorTotalTexto());

            if (form.esModalidadDigital()) {
                if (form.getArchivoSeleccionado() == null) {
                    throw new IllegalArgumentException(
                            "En modalidad digital debes adjuntar un archivo PDF, JPG o PNG");
                }
                if (form.getModeloItems().getRowCount() == 0 && !extraerItems(form, false)) return;
                copiaGuardada = servicioArchivo.guardarCopia(form.getArchivoSeleccionado());
            }

            Factura factura = new Factura();
            factura.setNumeroFactura(form.getNumero());
            factura.setFechaEmision(fecha);
            factura.setIdProveedor(form.getIdProveedorSeleccionado());
            factura.setValorTotal(valorTotal);
            factura.setRutaArchivoDigital(copiaGuardada);

            List<ItemFactura> items = leerItemsDelFormulario(form);
            servicio.registrar(factura, items);
            form.dispose();
            cargarTodas();
            JOptionPane.showMessageDialog(vista,
                    "Factura registrada correctamente. Los ítems quedan listos para procesamiento.");
        } catch (DateTimeParseException ex) {
            servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            form.mostrarError("Fecha inválida, usa el formato dd-mm-aaaa");
        } catch (NumberFormatException ex) {
            servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            form.mostrarError("Valor total o cantidad inválida en algún ítem");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            form.mostrarError(ex.getMessage());
        } catch (IOException ex) {
            servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            form.mostrarError("No se pudo almacenar el archivo digital: " + ex.getMessage());
        } catch (SQLException ex) {
            servicioArchivo.eliminarCopiaSilenciosamente(copiaGuardada);
            form.mostrarError("Error al guardar: " + ex.getMessage());
        }
    }

    private List<ItemFactura> leerItemsDelFormulario(VFormularioFactura form) {
        DefaultTableModel modelo = form.getModeloItems();
        if (modelo.getRowCount() == 0) {
            throw new IllegalArgumentException("La factura debe tener al menos un ítem");
        }

        List<ItemFactura> items = new ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            String codigo = valor(modelo.getValueAt(i, 0));
            String descripcion = valor(modelo.getValueAt(i, 1));
            String textoCantidad = valor(modelo.getValueAt(i, 2));
            String estado = valor(modelo.getValueAt(i, 3));
            if (estado.isBlank()) estado = codigo.isBlank() ? "No Procesado" : "Observado";

            int cantidad = textoCantidad.isBlank() && "No Procesado".equals(estado)
                    ? 0 : Integer.parseInt(textoCantidad);
            if ("No Procesado".equals(estado)) {
                if (cantidad < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa");
            } else {
                if (codigo.isBlank()) {
                    throw new IllegalArgumentException(
                            "El código interno del proveedor es obligatorio en los ítems legibles");
                }
                if (cantidad <= 0) {
                    throw new IllegalArgumentException("La cantidad de cada ítem legible debe ser mayor a cero");
                }
                estado = "Observado";
            }

            ItemFactura item = new ItemFactura();
            item.setCodigoInternoProveedor(codigo.isBlank() ? null : codigo);
            item.setDescripcion(descripcion.isBlank() ? null : descripcion);
            item.setCantidadFacturada(cantidad);
            item.setPrecioUnitarioCompra(0);
            item.setEstadoItem(estado);
            items.add(item);
        }
        return items;
    }

    private String valor(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private void abrirDetalle() {
        int fila = vista.getTblFacturas().getSelectedRow();
        if (fila < 0) return;
        int idFactura = (int) vista.getModeloTabla().getValueAt(fila, 0);

        try {
            Factura f = servicio.buscarPorId(idFactura);
            if (f == null) return;
            VDetalleFactura detalle = new VDetalleFactura(null);
            detalle.cargarCabecera(f);
            detalle.cargarItems(servicio.obtenerItems(idFactura));

            boolean puedeGestionar = SesionActual.esAdministrador() || SesionActual.esBodeguero();
            boolean noProcesada = !"Procesada".equals(f.getEstado());
            detalle.getBtnProcesar().setEnabled(puedeGestionar && noProcesada);
            detalle.getBtnObservar().setEnabled(puedeGestionar && noProcesada);
            detalle.getBtnProcesarItems().setEnabled(puedeGestionar);

            detalle.getBtnProcesar().addActionListener(e -> cambiarEstado(idFactura, "Procesada", detalle));
            detalle.getBtnObservar().addActionListener(e -> cambiarEstado(idFactura, "Observada", detalle));
            detalle.getBtnProcesarItems().addActionListener(e -> abrirProcesamiento(idFactura, f.getIdProveedor()));
            detalle.getBtnAbrirArchivo().addActionListener(e -> abrirArchivoAdjunto(detalle));
            detalle.setVisible(true);
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void abrirArchivoAdjunto(VDetalleFactura detalle) {
        String ruta = detalle.getRutaArchivo();
        if (ruta == null || ruta.isBlank()) {
            JOptionPane.showMessageDialog(detalle, "La factura no tiene archivo adjunto");
            return;
        }
        File archivo = new File(ruta);
        if (!archivo.isFile()) {
            JOptionPane.showMessageDialog(detalle,
                    "El archivo adjunto no existe en esta ubicación:\n" + ruta);
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(detalle,
                    "Este equipo no permite abrir archivos con la aplicación predeterminada");
            return;
        }
        try {
            Desktop.getDesktop().open(archivo);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(detalle,
                    "No se pudo abrir el archivo adjunto: " + ex.getMessage());
        }
    }

    private void abrirProcesamiento(int idFactura, int idProveedor) {
        VProcesamientoFactura vistaProc = new VProcesamientoFactura(null);
        new ControladorProcesamientoFactura(vistaProc, idFactura, idProveedor);
        vistaProc.setVisible(true);
        cargarTodas();
    }

    private void cambiarEstado(int idFactura, String nuevoEstado, VDetalleFactura detalle) {
        int confirmar = JOptionPane.showConfirmDialog(detalle,
                "¿Confirmas cambiar el estado de la factura a '" + nuevoEstado + "'?"
                        + ("Procesada".equals(nuevoEstado)
                        ? "\nEsto ingresará el stock únicamente de los ítems validados." : ""),
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try {
            servicio.cambiarEstado(idFactura, nuevoEstado);
            detalle.dispose();
            cargarTodas();
            JOptionPane.showMessageDialog(vista, "Estado actualizado a " + nuevoEstado);
        } catch (IllegalStateException | IllegalArgumentException | SecurityException ex) {
            JOptionPane.showMessageDialog(detalle, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(SQLException ex) {
        JOptionPane.showMessageDialog(vista, "Error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
