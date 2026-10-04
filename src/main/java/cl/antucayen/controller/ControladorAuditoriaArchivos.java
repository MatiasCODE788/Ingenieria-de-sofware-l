package cl.antucayen.controller;

import cl.antucayen.model.entity.LogArchivo;
import cl.antucayen.model.service.ServicioAuditoriaArchivos;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VAuditoriaArchivos;

import javax.swing.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/** Controlador administrativo para consultar log_archivo. */
public class ControladorAuditoriaArchivos {

    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

    private final VAuditoriaArchivos vista;
    private final ServicioAuditoriaArchivos servicio = new ServicioAuditoriaArchivos();

    public ControladorAuditoriaArchivos(VAuditoriaArchivos vista) {
        Autorizacion.verificarAdministrador(
                "Solo el Administrador puede consultar la bitácora de archivos");
        this.vista = vista;
        vista.getBtnActualizar().addActionListener(e -> cargar());
        cargar();
    }

    private void cargar() {
        try {
            vista.limpiar();
            for (LogArchivo log : servicio.listarRecientes(vista.getLimite())) {
                vista.agregarFila(toFila(log));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    vista,
                    "No se pudo consultar la bitácora: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Object[] toFila(LogArchivo log) {
        String usuario = log.getNombreUsuario();
        if (usuario == null || usuario.isBlank()) usuario = "ID " + log.getIdUsuario();
        return new Object[]{
                log.getFechaHora() == null ? "" : log.getFechaHora().format(FECHA_HORA),
                usuario,
                log.getTipoOperacion(),
                log.getNombreArchivo(),
                log.getFormato(),
                log.getResultado(),
                log.getDetalle() == null ? "" : log.getDetalle()
        };
    }
}
