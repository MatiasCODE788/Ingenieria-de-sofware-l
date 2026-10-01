package cl.antucayen.controller;

import cl.antucayen.model.entity.ItemVenta;
import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.entity.Venta;
import cl.antucayen.model.service.ServicioProducto;
import cl.antucayen.model.service.ServicioVenta;
import cl.antucayen.model.service.ServicioComprobanteVenta;
import cl.antucayen.util.SesionActual;
import cl.antucayen.view.VVentas;
import cl.antucayen.view.VComprobanteVenta;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ControladorVenta {

    private final VVentas          vista;
    private final ServicioVenta    servicio          = new ServicioVenta();
    private final ServicioProducto servicioProducto  = new ServicioProducto();
    private final ServicioComprobanteVenta servicioComprobante = new ServicioComprobanteVenta();

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private int idVentaEnCurso;

    public ControladorVenta(VVentas vista) {
        this.vista = vista;
        iniciarEventos();
        iniciarVentaPersistida();
        cargarVentasDelDia();
        actualizarEstadoPago();
    }

    private void iniciarVentaPersistida() {
        try {
            idVentaEnCurso = servicio.iniciarVenta();
            vista.setVentaEnCurso(idVentaEnCurso);
        } catch (SQLException | SecurityException ex) {
            idVentaEnCurso = 0;
            vista.setVentaEnCurso(0);
            JOptionPane.showMessageDialog(vista,
                    "No se pudo iniciar una nueva venta: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Se llama cada vez que se vuelve a mostrar este panel (sin perder el carrito ni los pagos). */
    public void alMostrar() {
        cargarVentasDelDia();
    }

    private void iniciarEventos() {
        vista.getBtnAgregar().addActionListener(e -> agregarAlCarrito());
        vista.getTxtBusqueda().addActionListener(e -> agregarAlCarrito()); // Enter también agrega

        vista.getBtnLimpiarCarrito().addActionListener(e -> {
            vista.limpiarCarrito();
            vista.limpiarPagos();
            actualizarEstadoPago();
        });

        vista.getBtnCobrar().addActionListener(e -> cobrar());

        // Botones de pago único: llenan el 100% del total en un solo medio
        vista.getBtnChipEfectivo().addActionListener(e -> pagarTodoCon("Efectivo"));
        vista.getBtnChipDebito().addActionListener(e -> pagarTodoCon("Débito"));
        vista.getBtnChipCredito().addActionListener(e -> pagarTodoCon("Crédito"));
        vista.getBtnLimpiarPagos().addActionListener(e -> {
            vista.limpiarPagos();
            actualizarEstadoPago();
        });

        // Recalcula "falta/sobra" en vivo mientras se editan los montos de pago
        DocumentListener recalcPago = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { actualizarEstadoPago(); }
            public void removeUpdate(DocumentEvent e) { actualizarEstadoPago(); }
            public void changedUpdate(DocumentEvent e) { actualizarEstadoPago(); }
        };
        vista.getTxtPagoEfectivo().getDocument().addDocumentListener(recalcPago);
        vista.getTxtPagoDebito().getDocument().addDocumentListener(recalcPago);
        vista.getTxtPagoCredito().getDocument().addDocumentListener(recalcPago);

        vista.getModeloCarrito().addTableModelListener(evt -> {
            if (evt.getColumn() == 3 && evt.getType() == TableModelEvent.UPDATE) {
                recalcularFila(evt.getFirstRow());
            }
        });

        vista.getTblVentasDia().addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) intentarAnular();
            }
        });
    }

    private void agregarAlCarrito() {
        String texto = vista.getTextoBusqueda();
        if (texto.isEmpty()) return;

        try {
            Producto p = servicioProducto.buscarPorSku(texto);
            if (p == null) p = servicioProducto.buscarPorCodigoBarras(texto);

            if (p == null) {
                List<Producto> coincidencias = servicioProducto.buscarPorNombre(texto);
                coincidencias.removeIf(prod -> !"Activo".equals(prod.getEstado()));
                if (coincidencias.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "No se encontró ningún producto activo para: " + texto);
                    vista.limpiarBusqueda();
                    return;
                } else if (coincidencias.size() == 1) {
                    p = coincidencias.get(0);
                } else {
                    p = elegirEntreVarios(coincidencias);
                    if (p == null) { vista.limpiarBusqueda(); return; }
                }
            }

            if (!"Activo".equals(p.getEstado())) {
                JOptionPane.showMessageDialog(null, "El producto '" + p.getNombre() + "' está inactivo");
                vista.limpiarBusqueda();
                return;
            }
            if (p.getStockActual() <= 0) {
                JOptionPane.showMessageDialog(null, "Sin stock disponible de '" + p.getNombre() + "'");
                vista.limpiarBusqueda();
                return;
            }

            agregarOIncrementar(p);
            vista.limpiarBusqueda();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al buscar producto: " + ex.getMessage());
        }
    }

    private Producto elegirEntreVarios(List<Producto> opciones) {
        String[] nombres = opciones.stream()
                .map(p -> p.getSku() + " — " + p.getNombre() + " ($" + p.getPrecioVenta() + ")")
                .toArray(String[]::new);
        String elegido = (String) JOptionPane.showInputDialog(null,
                "Se encontraron varios productos, elige uno:", "Seleccionar producto",
                JOptionPane.PLAIN_MESSAGE, null, nombres, nombres[0]);
        if (elegido == null) return null;
        int idx = java.util.Arrays.asList(nombres).indexOf(elegido);
        return opciones.get(idx);
    }

    private void agregarOIncrementar(Producto p) {
        DefaultTableModel modelo = vista.getModeloCarrito();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (modelo.getValueAt(i, 0).equals(p.getSku())) {
                int cantidadActual = (int) modelo.getValueAt(i, 3);
                int nuevaCantidad = cantidadActual + 1;
                if (nuevaCantidad > p.getStockActual()) {
                    JOptionPane.showMessageDialog(null,
                            "No hay más stock disponible de '" + p.getNombre() + "' (stock: " + p.getStockActual() + ")");
                    return;
                }
                modelo.setValueAt(nuevaCantidad, i, 3);
                return;
            }
        }
        int precio = p.getPrecioVenta();
        vista.agregarFilaCarrito(new Object[]{p.getSku(), p.getNombre(), precio, 1, precio});
        recalcularTotal();
    }

    private void recalcularFila(int fila) {
        DefaultTableModel modelo = vista.getModeloCarrito();
        if (fila < 0 || fila >= modelo.getRowCount()) return;
        try {
            int cantidad = (int) modelo.getValueAt(fila, 3);
            String sku = (String) modelo.getValueAt(fila, 0);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(null, "La cantidad debe ser mayor a cero");
                modelo.setValueAt(1, fila, 3);
                return;
            }
            Producto p = servicioProducto.buscarPorSku(sku);
            if (p != null && cantidad > p.getStockActual()) {
                JOptionPane.showMessageDialog(null,
                        "Stock insuficiente (disponible: " + p.getStockActual() + ")");
                modelo.setValueAt(p.getStockActual(), fila, 3);
                cantidad = p.getStockActual();
            }
            int precioUnit = (int) modelo.getValueAt(fila, 2);
            modelo.setValueAt(precioUnit * cantidad, fila, 4);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
        recalcularTotal();
    }

    private void recalcularTotal() {
        vista.actualizarTotal(totalCarritoActual());
        actualizarEstadoPago();
    }

    private int totalCarritoActual() {
        DefaultTableModel modelo = vista.getModeloCarrito();
        int total = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) total += (int) modelo.getValueAt(i, 4);
        return total;
    }

    /** Llena el 100% del total en el medio de pago elegido y deja los demás en cero (pago único). */
    private void pagarTodoCon(String medio) {
        int total = totalCarritoActual();
        vista.setMontoEfectivo(medio.equals("Efectivo") ? total : 0);
        vista.setMontoDebito(medio.equals("Débito") ? total : 0);
        vista.setMontoCredito(medio.equals("Crédito") ? total : 0);
    }

    /**
     * Recalcula el pago en vivo. El cobro se habilita cuando el total recibido
     * cubre la venta y cualquier excedente puede devolverse íntegramente como
     * vuelto desde el efectivo recibido.
     */
    private void actualizarEstadoPago() {
        int total = totalCarritoActual();
        int efectivo = vista.getMontoEfectivo();
        int debito = vista.getMontoDebito();
        int credito = vista.getMontoCredito();

        int pagado;
        try {
            pagado = Math.addExact(Math.addExact(efectivo, debito), credito);
        } catch (ArithmeticException ex) {
            vista.setEstadoPagoInvalido("Los montos ingresados son demasiado altos");
            vista.setCobrarHabilitado(false);
            return;
        }

        if (total <= 0) {
            vista.setEstadoPago(0, 0, total);
            vista.setCobrarHabilitado(false);
            return;
        }

        if (pagado < total) {
            vista.setEstadoPago(total - pagado, 0, total);
            vista.setCobrarHabilitado(false);
            return;
        }

        List<PagoVenta> pagos = construirPagos(efectivo, debito, credito);
        try {
            int vuelto = servicio.calcularVuelto(pagos, total);
            vista.setEstadoPago(0, vuelto, total);
            vista.setCobrarHabilitado(true);
        } catch (IllegalArgumentException ex) {
            vista.setEstadoPagoInvalido(ex.getMessage());
            vista.setCobrarHabilitado(false);
        }
    }

    private void cobrar() {
        DefaultTableModel modelo = vista.getModeloCarrito();
        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "El carrito está vacío");
            return;
        }

        List<ItemVenta> items = new ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            ItemVenta item = new ItemVenta();
            item.setSku((String) modelo.getValueAt(i, 0));
            item.setCantidad((int) modelo.getValueAt(i, 3));
            item.setPrecioUnitarioVenta((int) modelo.getValueAt(i, 2));
            item.setSubtotal((int) modelo.getValueAt(i, 4));
            items.add(item);
        }
        int total = items.stream().mapToInt(ItemVenta::getSubtotal).sum();

        List<PagoVenta> pagos = construirPagos(
                vista.getMontoEfectivo(),
                vista.getMontoDebito(),
                vista.getMontoCredito());

        if (pagos.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingresa el monto pagado en al menos un medio de pago");
            return;
        }

        int pagado;
        try {
            pagado = pagos.stream()
                    .map(PagoVenta::getMonto)
                    .reduce(0, Math::addExact);
        } catch (ArithmeticException ex) {
            JOptionPane.showMessageDialog(null, "La suma de los pagos excede el rango permitido");
            return;
        }

        if (pagado < total) {
            JOptionPane.showMessageDialog(null, "El monto pagado ($" + formatear(pagado)
                    + ") es insuficiente para el total de la venta ($" + formatear(total) + ")");
            return;
        }

        final int vuelto;
        try {
            vuelto = servicio.calcularVuelto(pagos, total);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            return;
        }

        String detallePago = pagos.stream()
                .map(pago -> pago.getMedioPago() + " $" + formatear(pago.getMonto()))
                .collect(Collectors.joining(" + "));

        String mensajeConfirmacion = "Confirmar cobro de $" + formatear(total)
                + " (" + detallePago + ")"
                + (vuelto > 0 ? "\nVuelto a entregar: $" + formatear(vuelto) : "");

        int confirmar = JOptionPane.showConfirmDialog(null,
                mensajeConfirmacion,
                "Confirmar venta", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try {
            if (idVentaEnCurso <= 0) throw new IllegalStateException("No existe una venta en curso activa");
            int idVenta = servicio.registrar(idVentaEnCurso, pagos, items);
            vista.limpiarCarrito();
            vista.limpiarPagos();
            actualizarEstadoPago();
            cargarVentasDelDia();

            // RF-72: el comprobante se genera automáticamente después del
            // commit exitoso de la venta y se reconstruye desde la BD.
            var comprobante = servicioComprobante.obtener(idVenta, pagado, vuelto);
            java.awt.Window owner = SwingUtilities.getWindowAncestor(vista);
            VComprobanteVenta dialogo = new VComprobanteVenta(owner, comprobante);
            dialogo.setVisible(true);

            // RF-64: al terminar una venta se abre inmediatamente una nueva
            // operación en curso para el siguiente cliente.
            iniciarVentaPersistida();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al registrar la venta: " + ex.getMessage());
        }
    }

    private List<PagoVenta> construirPagos(int efectivo, int debito, int credito) {
        List<PagoVenta> pagos = new ArrayList<>();
        agregarPagoSiCorresponde(pagos, "Efectivo", efectivo);
        agregarPagoSiCorresponde(pagos, "Débito", debito);
        agregarPagoSiCorresponde(pagos, "Crédito", credito);
        return pagos;
    }

    private void agregarPagoSiCorresponde(List<PagoVenta> pagos, String medio, int monto) {
        if (monto > 0) pagos.add(new PagoVenta(medio, monto));
    }

    private String formatear(int monto) {
        return String.format("%,d", monto).replace(',', '.');
    }

    private void cargarVentasDelDia() {
        try {
            vista.limpiarVentasDia();
            List<Venta> ventas = servicio.listarDelDia();
            int totalDia = 0;
            for (Venta v : ventas) {
                vista.agregarFilaVentaDia(new Object[]{
                        v.getIdVenta(),
                        v.getFechaHora() != null ? v.getFechaHora().format(HORA) : "-",
                        v.getNombreUsuario(), v.getMedioPago(),
                        "$" + formatear(v.getMontoTotal()),
                        v.getEstado()
                });
                if ("Pagada".equals(v.getEstado())) totalDia += v.getMontoTotal();
            }
            String alcance = SesionActual.esAdministrador() ? "Ventas del día" : "Mis ventas del día";
            vista.setResumenDia(alcance + ": " + ventas.size() + " venta(s) — total $" + formatear(totalDia));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al cargar ventas del día: " + ex.getMessage());
        }
    }

    private void intentarAnular() {
        int fila = vista.getTblVentasDia().getSelectedRow();
        if (fila < 0) return;
        int idVenta = (int) vista.getModeloVentasDia().getValueAt(fila, 0);
        String estado = (String) vista.getModeloVentasDia().getValueAt(fila, 5);

        if (!"Pagada".equals(estado)) {
            JOptionPane.showMessageDialog(null, "Esta venta ya está " + estado.toLowerCase());
            return;
        }
        if (!SesionActual.esAdministrador()) {
            JOptionPane.showMessageDialog(null, "Solo un Administrador puede anular ventas");
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(null,
                "¿Anular la venta #" + idVenta + "? Esto devolverá el stock de todos sus ítems.",
                "Confirmar anulación", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try {
            servicio.anular(idVenta);
            JOptionPane.showMessageDialog(null, "Venta anulada, stock devuelto correctamente");
            cargarVentasDelDia();
        } catch (SecurityException | IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al anular: " + ex.getMessage());
        }
    }
}