package cl.antucayen.model.service;

import cl.antucayen.model.dao.ReporteDAO;
import cl.antucayen.model.dto.ReporteTabular;
import cl.antucayen.security.Autorizacion;

import java.sql.SQLException;
import java.time.LocalDate;

/** RF-57 a RF-60: reglas y autorización de generación de reportes. */
public class ServicioReportes {

    private final ReporteDAO reporteDAO = new ReporteDAO();

    public ReporteTabular generarStockActual() throws SQLException {
        Autorizacion.verificarReportes();
        return reporteDAO.reporteStockActual();
    }

    public ReporteTabular generarEquivalenciasFaltantes() throws SQLException {
        Autorizacion.verificarReportes();
        return reporteDAO.reporteEquivalenciasFaltantes();
    }

    public ReporteTabular generarFacturas(String estado, LocalDate desde, LocalDate hasta) throws SQLException {
        Autorizacion.verificarReportes();
        validarRangoOpcional(desde, hasta);
        return reporteDAO.reporteFacturas(estado, desde, hasta);
    }

    public ReporteTabular generarMovimientos(LocalDate desde, LocalDate hasta) throws SQLException {
        Autorizacion.verificarReportes();
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("El rango de fechas es obligatorio para el reporte de movimientos");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha Desde no puede ser posterior a Hasta");
        }
        return reporteDAO.reporteMovimientos(desde, hasta);
    }

    private void validarRangoOpcional(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha Desde no puede ser posterior a Hasta");
        }
    }
}
