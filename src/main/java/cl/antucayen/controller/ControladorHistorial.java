package cl.antucayen.controller;

import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.service.ServicioInventario;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VHistorial;

import javax.swing.JOptionPane;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ControladorHistorial {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

    private final VHistorial vista;
    private final ServicioInventario servicio = new ServicioInventario();

    public ControladorHistorial(VHistorial vista) {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        this.vista = vista;
        cargarTodos();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnFiltrar().addActionListener(e -> filtrar());
        vista.getBtnLimpiar().addActionListener(e -> {
            vista.limpiarFiltros();
            cargarTodos();
        });
    }

    private void cargarTodos() {
        try {
            vista.limpiarTabla();
            for (MovimientoInventario movimiento : servicio.listarMovimientos()) {
                vista.agregarFila(toFila(movimiento));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void filtrar() {
        try {
            String sku = vista.getSku().isBlank() ? null : vista.getSku();
            String tipo = "Todos".equals(vista.getTipo()) ? null : vista.getTipo();
            LocalDate desde = parsearFecha(vista.getDesde());
            LocalDate hasta = parsearFecha(vista.getHasta());

            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                JOptionPane.showMessageDialog(null, "La fecha Desde no puede ser posterior a Hasta");
                return;
            }

            Timestamp desdeTs = desde == null ? null : Timestamp.valueOf(desde.atStartOfDay());
            Timestamp hastaExclusivo = hasta == null
                    ? null : Timestamp.valueOf(hasta.plusDays(1).atStartOfDay());

            vista.limpiarTabla();
            for (MovimientoInventario movimiento
                    : servicio.filtrarMovimientos(sku, tipo, desdeTs, hastaExclusivo)) {
                vista.agregarFila(toFila(movimiento));
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(null, "Fecha inválida. Usa el formato dd/mm/aaaa");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank() || "dd/mm/aaaa".equalsIgnoreCase(texto.trim())) return null;
        return LocalDate.parse(texto.trim(), FORMATO_FECHA);
    }

    private Object[] toFila(MovimientoInventario movimiento) {
        String usuario = movimiento.getNombreUsuario();
        String username = movimiento.getUsernameUsuario();
        if (usuario == null || usuario.isBlank()) usuario = username == null ? "" : username;
        if (username != null && !username.isBlank() && !username.equals(usuario)) {
            usuario = usuario + " (" + username + ")";
        }
        return new Object[]{
                movimiento.getTipoMovimiento(),
                movimiento.getFechaHora() != null
                        ? movimiento.getFechaHora().format(FORMATO_FECHA_HORA) : "",
                movimiento.getSku(),
                movimiento.getNombreProducto(),
                movimiento.getStockAnterior(),
                movimiento.getCantidadAplicada(),
                movimiento.getStockResultante(),
                movimiento.getIdUsuario(),
                usuario
        };
    }
}
