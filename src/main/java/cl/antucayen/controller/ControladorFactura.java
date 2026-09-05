package cl.antucayen.controller;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.model.service.ServicioFactura;
import cl.antucayen.model.service.ServicioProveedor;
import cl.antucayen.util.SesionActual;
import cl.antucayen.view.VDetalleFactura;
import cl.antucayen.view.VFacturas;
import cl.antucayen.view.VFormularioFactura;
import cl.antucayen.view.VProcesamientoFactura;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ControladorFactura {

    private final ServicioFactura   servicio          = new ServicioFactura();
    private final ServicioProveedor servicioProveedor = new ServicioProveedor();

    private final VFacturas vista;

    public ControladorFactura(VFacturas vista) {
        this.vista = vista;
        cargarProveedoresEnFiltro();
        cargarTodas();
        iniciarEventos();
    }

    /** Constructor "standalone": se usa desde el menú "Procesar Factura" para
     *  abrir directamente el formulario de ingreso manual, sin lista de fondo. */
    public ControladorFactura() {
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
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) abrirDetalle();
            }
        });
    }

    private void cargarProveedoresEnFiltro() {
        try {
            List<Proveedor> proveedores = servicioProveedor.listarTodos();
            vista.cargarProveedores(proveedores);
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void cargarTodas() {
        if (vista == null) return; // no hay lista de fondo (uso standalone desde el menú)
        try {
            vista.limpiarTabla();
            for (Factura f : servicio.listarTodas())
                vista.agregarFila(new Object[]{
                        f.getIdFactura(), f.getNumeroFactura(), f.getFechaEmision(),
                        f.getNombreProveedor(), f.getEstado()
                });
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
                        f.getIdFactura(), f.getNumeroFactura(), f.getFechaEmision(),
                        f.getNombreProveedor(), f.getEstado()
                });
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(null,
                    "Formato de fecha inválido, usa aaaa-mm-dd");
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return LocalDate.parse(texto.trim());
    }

    public void abrirNuevaFactura() {
        try {
            VFormularioFactura form = new VFormularioFactura(null);
            form.cargarProveedores(servicioProveedor.listarTodos());
            form.getBtnGuardar().addActionListener(e -> guardarFactura(form));
            form.setVisible(true);
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void guardarFactura(VFormularioFactura form) {
        try {
            LocalDate fecha = LocalDate.parse(form.getFechaTexto().trim());
            int valorTotal  = Integer.parseInt(form.getValorTotalTexto());

            Factura f = new Factura();
            f.setNumeroFactura(form.getNumero());
            f.setFechaEmision(fecha);
            f.setIdProveedor(form.getIdProveedorSeleccionado());
            f.setValorTotal(valorTotal);

            List<ItemFactura> items = leerItemsDelFormulario(form);

            servicio.registrar(f, items);
            form.dispose();
            cargarTodas();
            JOptionPane.showMessageDialog(null, "Factura registrada correctamente");
        } catch (DateTimeParseException ex) {
            form.mostrarError("Fecha inválida, usa el formato aaaa-mm-dd");
        } catch (NumberFormatException ex) {
            form.mostrarError("Valor total o cantidad inválida en algún producto");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error al guardar: " + ex.getMessage());
        }
    }

    private List<ItemFactura> leerItemsDelFormulario(VFormularioFactura form) {
        DefaultTableModel modelo = form.getModeloItems();
        List<ItemFactura> items = new ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            String descripcion = String.valueOf(modelo.getValueAt(i, 0)).trim();
            int cantidad       = Integer.parseInt(String.valueOf(modelo.getValueAt(i, 1)).trim());

            ItemFactura item = new ItemFactura();
            item.setDescripcion(descripcion.isBlank() ? null : descripcion);
            item.setCantidadFacturada(cantidad);
            item.setPrecioUnitarioCompra(0); // no se ingresa por producto en este formulario
            item.setEstadoItem("Observado");  // sin SKU asignado aún, requiere equivalencia manual
            items.add(item);
        }
        return items;
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
            detalle.getBtnProcesar().setEnabled(puedeGestionar && !"Procesada".equals(f.getEstado()));
            detalle.getBtnObservar().setEnabled(puedeGestionar && !"Procesada".equals(f.getEstado()));

            detalle.getBtnProcesar().addActionListener(e -> cambiarEstado(idFactura, "Procesada", detalle));
            detalle.getBtnObservar().addActionListener(e -> cambiarEstado(idFactura, "Observada", detalle));
            detalle.getBtnProcesarItems().addActionListener(e -> abrirProcesamiento(idFactura, f.getIdProveedor()));

            detalle.setVisible(true);
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void abrirProcesamiento(int idFactura, int idProveedor) {
        VProcesamientoFactura vistaProc = new VProcesamientoFactura(null);
        new ControladorProcesamientoFactura(vistaProc, idFactura, idProveedor);
        vistaProc.setVisible(true);
        cargarTodas(); // por si el reproceso cambió estados visibles en la tabla principal
    }

    private void cambiarEstado(int idFactura, String nuevoEstado, VDetalleFactura detalle) {
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Confirmas cambiar el estado de la factura a '" + nuevoEstado + "'?"
                        + ("Procesada".equals(nuevoEstado)
                        ? "\nEsto ingresará el stock de los ítems válidos de forma permanente."
                        : ""),
                "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try {
            servicio.cambiarEstado(idFactura, nuevoEstado);
            detalle.dispose();
            cargarTodas();
            JOptionPane.showMessageDialog(null, "Estado actualizado a " + nuevoEstado);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
    }
}