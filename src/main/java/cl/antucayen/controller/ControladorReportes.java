package cl.antucayen.controller;

import cl.antucayen.model.dto.ReporteTabular;
import cl.antucayen.model.service.ServicioExportacionDatos;
import cl.antucayen.model.service.ServicioReportes;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VReportes;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ControladorReportes {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final VReportes vista;
    private final ServicioReportes servicio = new ServicioReportes();
    private final ServicioExportacionDatos exportacion = new ServicioExportacionDatos();
    private final Runnable abrirEquivalencias;
    private ReporteTabular reporteActual;

    public ControladorReportes(VReportes vista, Runnable abrirEquivalencias) {
        Autorizacion.verificarReportes();
        this.vista = vista;
        this.abrirEquivalencias = abrirEquivalencias;
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnGenerar().addActionListener(e -> generar());
        vista.getBtnExportar().addActionListener(e -> exportar());
        vista.getBtnEquivalencias().addActionListener(e -> {
            if (abrirEquivalencias != null) abrirEquivalencias.run();
        });
    }

    private void generar() {
        try {
            String seleccion = vista.getReporteSeleccionado();
            LocalDate desde = parsear(vista.getDesde());
            LocalDate hasta = parsear(vista.getHasta());

            if (seleccion.startsWith("RF-57")) {
                reporteActual = servicio.generarStockActual();
            } else if (seleccion.startsWith("RF-58")) {
                reporteActual = servicio.generarEquivalenciasFaltantes();
            } else if (seleccion.startsWith("RF-59")) {
                reporteActual = servicio.generarFacturas(vista.getEstadoFactura(), desde, hasta);
            } else {
                reporteActual = servicio.generarMovimientos(desde, hasta);
            }

            vista.cargarReporte(reporteActual.columnas(), reporteActual.filas());
            vista.setEquivalenciasVisible(seleccion.startsWith("RF-58"));
            vista.setEstado(reporteActual.estaVacio()
                    ? "El reporte no arrojó resultados."
                    : "Reporte generado: " + reporteActual.filas().size() + " fila(s).", false);
        } catch (DateTimeParseException ex) {
            error("Fecha inválida. Usa dd/mm/aaaa.");
        } catch (IllegalArgumentException | SecurityException ex) {
            error(ex.getMessage());
        } catch (SQLException ex) {
            error("Error al generar reporte: " + ex.getMessage());
        }
    }

    private void exportar() {
        if (reporteActual == null || reporteActual.estaVacio()) {
            error("Primero genera un reporte con resultados.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar reporte a Excel");
        chooser.setSelectedFile(new java.io.File(
                "reporte_" + LocalDateTime.now().format(NOMBRE) + ".xlsx"));
        chooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        if (chooser.showSaveDialog(vista) != JFileChooser.APPROVE_OPTION) return;

        try {
            Path archivo = exportacion.exportarReporteExcel(
                    reporteActual, chooser.getSelectedFile().toPath());
            JOptionPane.showMessageDialog(vista,
                    "Reporte exportado correctamente:\n" + archivo.toAbsolutePath());
        } catch (IOException | SQLException | IllegalArgumentException | SecurityException ex) {
            error("No se pudo exportar el reporte: " + ex.getMessage());
        }
    }

    private LocalDate parsear(String texto) {
        if (texto == null || texto.isBlank() || "dd/mm/aaaa".equalsIgnoreCase(texto)) return null;
        return LocalDate.parse(texto, FECHA);
    }

    private void error(String mensaje) {
        vista.setEstado(mensaje, true);
        JOptionPane.showMessageDialog(vista, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
