package cl.antucayen.controller;

import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.service.ServicioExportacionDatos;
import cl.antucayen.model.service.ServicioExportacionDatos.FormatoExportacion;
import cl.antucayen.model.service.ServicioProducto;
import cl.antucayen.model.service.ServicioProveedor;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.SesionActual;
import cl.antucayen.view.VBuscadorProductos;
import cl.antucayen.view.VFormularioProducto;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ControladorProducto {

    private static final DateTimeFormatter FORMATO_NOMBRE_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final VBuscadorProductos vista;
    private final ServicioProducto servicio = new ServicioProducto();
    private final ServicioProveedor servicioProveedor = new ServicioProveedor();
    private final ServicioExportacionDatos servicioExportacion = new ServicioExportacionDatos();

    public ControladorProducto(VBuscadorProductos vista) {
        Autorizacion.verificarConsultaStock();
        this.vista = vista;
        vista.getBtnNuevo().setVisible(!SesionActual.esCajero());
        cargarTodos();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnBuscar().addActionListener(e -> buscar());
        vista.getBtnNuevo().addActionListener(e -> abrirNuevo());
        vista.getBtnExportar().addActionListener(e -> exportar());
        vista.getTblProductos().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) abrirEdicion();
            }
        });
    }

    private void cargarTodos() {
        try {
            cargarTabla(servicio.listarTodos());
        } catch (SQLException ex) {
            mostrarError("Error al cargar productos: " + ex.getMessage());
        }
    }

    private void cargarTabla(List<Producto> productos) {
        vista.limpiarTabla();
        for (Producto p : productos) {
            vista.agregarFila(new Object[]{
                    p.getSku(), p.getNombre(), p.getCodigoBarras(),
                    p.getUnidadMedida(), p.getPrecioVenta(), p.getStockActual(), p.getEstado()
            });
        }
    }

    private void buscar() {
        try {
            List<Producto> lista = obtenerProductosConFiltrosActuales();
            cargarTabla(lista);
            if (lista.isEmpty() && !vista.getTextoBusqueda().isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                        "No se encontraron productos para la búsqueda: " + vista.getTextoBusqueda());
            }
        } catch (SQLException ex) {
            mostrarError("Error al buscar: " + ex.getMessage());
        }
    }

    private List<Producto> obtenerProductosConFiltrosActuales() throws SQLException {
        String texto = vista.getTextoBusqueda();
        String tipo = vista.getTipoBusqueda();

        if (texto.isEmpty()) {
            return servicio.listarTodos();
        }
        if ("SKU".equals(tipo)) {
            Producto p = servicio.buscarPorSku(texto);
            return p != null ? List.of(p) : List.of();
        }
        if ("Código de barras".equals(tipo)) {
            Producto p = servicio.buscarPorCodigoBarras(texto);
            return p != null ? List.of(p) : List.of();
        }
        return servicio.buscarPorNombre(texto);
    }

    private void exportar() {
        try {
            List<Producto> productos = obtenerProductosConFiltrosActuales();
            if (productos.isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                        "No hay productos para exportar con los filtros actuales.");
                return;
            }

            FormatoExportacion formato = seleccionarFormato();
            if (formato == null) return;

            Path destino = seleccionarDestino(
                    "inventario_stock_" + LocalDateTime.now().format(FORMATO_NOMBRE_ARCHIVO),
                    formato);
            if (destino == null) return;

            Path archivo = servicioExportacion.exportarInventario(productos, destino, formato);
            JOptionPane.showMessageDialog(vista,
                    "Inventario exportado correctamente:\n" + archivo.toAbsolutePath());
        } catch (SecurityException ex) {
            mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error al consultar inventario para exportar: " + ex.getMessage());
        } catch (IOException ex) {
            mostrarError("No se pudo generar el archivo: " + ex.getMessage());
        }
    }

    private FormatoExportacion seleccionarFormato() {
        Object[] opciones = {"CSV", "Excel (.xlsx)", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(
                vista,
                "Selecciona el formato de exportación:",
                "Exportar inventario",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (seleccion == 0) return FormatoExportacion.CSV;
        if (seleccion == 1) return FormatoExportacion.EXCEL;
        return null;
    }

    private Path seleccionarDestino(String nombreBase, FormatoExportacion formato) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar exportación de inventario");
        chooser.setSelectedFile(new java.io.File(
                nombreBase + "." + formato.getExtension()));
        chooser.setFileFilter(new FileNameExtensionFilter(
                formato.getDescripcion(), formato.getExtension()));

        return chooser.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().toPath()
                : null;
    }

    private void abrirNuevo() {
        try {
            Autorizacion.verificarGestionProductos();
            VFormularioProducto form = new VFormularioProducto(null, false);
            form.cargarProveedores(servicioProveedor.listarTodos(), List.of());
            form.getBtnGuardar().addActionListener(e -> guardarNuevo(form));
            form.setVisible(true);
        } catch (SQLException | SecurityException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void guardarNuevo(VFormularioProducto form) {
        try {
            Producto p = new Producto(
                    form.getSku(), form.getNombre(), form.getCodigoBarras(),
                    form.getUnidad(), Integer.parseInt(form.getPrecioVenta()),
                    Integer.parseInt(form.getStock()), "Activo"
            );
            servicio.registrar(p, form.getIdsProveedoresSeleccionados());
            form.dispose();
            cargarTodos();
            JOptionPane.showMessageDialog(vista, "Producto registrado correctamente");
        } catch (NumberFormatException ex) {
            form.mostrarError("El stock y el precio deben ser números enteros");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error al guardar: " + ex.getMessage());
        }
    }

    private void abrirEdicion() {
        int fila = vista.getTblProductos().getSelectedRow();
        if (fila < 0) return;
        String sku = String.valueOf(vista.getModeloTabla().getValueAt(fila, 0));

        try {
            Producto p = servicio.buscarPorSku(sku);
            if (p == null) return;

            VFormularioProducto form = new VFormularioProducto(null, true);
            form.setDatos(p.getSku(), p.getNombre(), p.getCodigoBarras(),
                    p.getUnidadMedida(), p.getPrecioVenta(), p.getStockActual(), p.getEstado());

            if (SesionActual.esCajero()) {
                // El Cajero sólo consulta la ficha/stock necesaria para la operación de caja.
                // No se cargan asociaciones de proveedores ni controles administrativos.
                form.cargarProveedores(List.of(), List.of());
                form.configurarSoloLectura();
            } else {
                form.cargarProveedores(servicioProveedor.listarTodos(),
                        servicio.listarIdsProveedores(sku));
                form.getBtnGuardar().addActionListener(e -> guardarEdicion(form, p));
                form.getBtnInactivar().addActionListener(e -> inactivar(form, sku));
            }
            form.setVisible(true);
        } catch (SQLException | SecurityException ex) {
            mostrarError("Error al cargar producto: " + ex.getMessage());
        }
    }

    private void guardarEdicion(VFormularioProducto form, Producto original) {
        try {
            original.setNombre(form.getNombre());
            original.setCodigoBarras(form.getCodigoBarras());
            original.setUnidadMedida(form.getUnidad());
            original.setPrecioVenta(Integer.parseInt(form.getPrecioVenta()));
            original.setEstado(form.getEstado());
            // El stock no se modifica desde la ficha: los cambios de inventario
            // deben pasar por movimientos/ajustes auditables.
            servicio.modificar(original, form.getIdsProveedoresSeleccionados());
            form.dispose();
            cargarTodos();
            JOptionPane.showMessageDialog(vista, "Producto actualizado correctamente");
        } catch (NumberFormatException ex) {
            form.mostrarError("El precio de venta debe ser un número entero");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error al actualizar: " + ex.getMessage());
        }
    }

    private void inactivar(VFormularioProducto form, String sku) {
        int confirm = JOptionPane.showConfirmDialog(
                form, "¿Inactivar el producto " + sku + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            servicio.inactivar(sku);
            form.dispose();
            cargarTodos();
            JOptionPane.showMessageDialog(vista,
                    "Producto inactivado. Su historial se conserva íntegramente.");
        } catch (SecurityException ex) {
            mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error al inactivar: " + ex.getMessage());
        }
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(vista, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
