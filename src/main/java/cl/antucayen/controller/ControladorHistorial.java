package cl.antucayen.controller;

import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.service.ServicioExportacionDatos;
import cl.antucayen.model.service.ServicioExportacionDatos.FormatoExportacion;
import cl.antucayen.model.service.ServicioInventario;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VHistorial;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ControladorHistorial {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");
    private static final DateTimeFormatter FORMATO_NOMBRE_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final VHistorial vista;
    private final ServicioInventario servicio = new ServicioInventario();
    private final ServicioExportacionDatos servicioExportacion = new ServicioExportacionDatos();

    public ControladorHistorial(VHistorial vista) {
        Autorizacion.verificarHistorialInventario();
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
        vista.getBtnExportar().addActionListener(e -> exportar());
    }

    private void cargarTodos() {
        try {
            cargarTabla(servicio.listarMovimientos());
        } catch (SQLException ex) {
            mostrarError("Error al cargar movimientos: " + ex.getMessage());
        }
    }

    private void cargarTabla(List<MovimientoInventario> movimientos) {
        vista.limpiarTabla();
        for (MovimientoInventario movimiento : movimientos) {
            vista.agregarFila(toFila(movimiento));
        }
    }

    private void filtrar() {
        try {
            List<MovimientoInventario> movimientos = obtenerMovimientosConFiltrosActuales();
            cargarTabla(movimientos);
            if (movimientos.isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                        "No se encontraron movimientos con los filtros indicados.");
            }
        } catch (DateTimeParseException ex) {
            mostrarError("Fecha inválida. Usa el formato dd/mm/aaaa");
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error al filtrar movimientos: " + ex.getMessage());
        }
    }

    private List<MovimientoInventario> obtenerMovimientosConFiltrosActuales()
            throws SQLException, DateTimeParseException {
        String sku = vista.getSku().isBlank() ? null : vista.getSku();
        String tipo = "Todos".equals(vista.getTipo()) ? null : vista.getTipo();
        LocalDate desde = parsearFecha(vista.getDesde());
        LocalDate hasta = parsearFecha(vista.getHasta());

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha Desde no puede ser posterior a Hasta");
        }

        Timestamp desdeTs = desde == null ? null : Timestamp.valueOf(desde.atStartOfDay());
        Timestamp hastaExclusivo = hasta == null
                ? null : Timestamp.valueOf(hasta.plusDays(1).atStartOfDay());

        boolean sinFiltros = sku == null && tipo == null && desdeTs == null && hastaExclusivo == null;
        return sinFiltros
                ? servicio.listarMovimientos()
                : servicio.filtrarMovimientos(sku, tipo, desdeTs, hastaExclusivo);
    }

    private void exportar() {
        try {
            List<MovimientoInventario> movimientos = obtenerMovimientosConFiltrosActuales();
            if (movimientos.isEmpty()) {
                JOptionPane.showMessageDialog(vista,
                        "No hay movimientos para exportar con los filtros actuales.");
                return;
            }

            FormatoExportacion formato = seleccionarFormato();
            if (formato == null) return;

            Path destino = seleccionarDestino(
                    "historial_movimientos_" + LocalDateTime.now().format(FORMATO_NOMBRE_ARCHIVO),
                    formato);
            if (destino == null) return;

            Path archivo = servicioExportacion.exportarMovimientos(movimientos, destino, formato);
            JOptionPane.showMessageDialog(vista,
                    "Historial exportado correctamente:\n" + archivo.toAbsolutePath());
        } catch (DateTimeParseException ex) {
            mostrarError("Fecha inválida. Usa el formato dd/mm/aaaa");
        } catch (IllegalArgumentException | SecurityException ex) {
            mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error al consultar movimientos para exportar: " + ex.getMessage());
        } catch (IOException ex) {
            mostrarError("No se pudo generar el archivo: " + ex.getMessage());
        }
    }

    private FormatoExportacion seleccionarFormato() {
        Object[] opciones = {"CSV", "Excel (.xlsx)", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(
                vista,
                "Selecciona el formato de exportación:",
                "Exportar historial",
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
        chooser.setDialogTitle("Guardar exportación de historial");
        chooser.setSelectedFile(new java.io.File(
                nombreBase + "." + formato.getExtension()));
        chooser.setFileFilter(new FileNameExtensionFilter(
                formato.getDescripcion(), formato.getExtension()));

        return chooser.showSaveDialog(vista) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().toPath()
                : null;
    }

    private LocalDate parsearFecha(String texto) {
        if (texto == null || texto.isBlank() || "dd/mm/aaaa".equalsIgnoreCase(texto.trim())) {
            return null;
        }
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

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                vista, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
