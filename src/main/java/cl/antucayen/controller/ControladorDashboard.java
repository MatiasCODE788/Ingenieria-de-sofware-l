package cl.antucayen.controller;

import cl.antucayen.model.dao.FacturaDAO;
import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.view.VDashboard;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

public class ControladorDashboard {

    private final VDashboard              vista;
    private final ProductoDAO             productoDAO  = new ProductoDAO();
    private final FacturaDAO              facturaDAO   = new FacturaDAO();
    private final MovimientoInventarioDAO movDAO       = new MovimientoInventarioDAO();

    public ControladorDashboard(VDashboard vista) {
        this.vista = vista;
        cargarEstadisticas();
    }

    private void cargarEstadisticas() {
        try {
            long activos = productoDAO.listarTodos().stream()
                    .filter(p -> "Activo".equals(p.getEstado())).count();
            vista.setProductosActivos((int) activos);

            int pendientes = facturaDAO.listarPorEstado("Pendiente").size();
            vista.setFacturasPendientes(pendientes);

            Timestamp inicioDia = Timestamp.valueOf(LocalDate.now().atStartOfDay());
            Timestamp finDia    = Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay());
            int movHoy = movDAO.filtrar(null, null, inicioDia, finDia).size();
            vista.setMovimientosHoy(movHoy);

        } catch (SQLException ex) {
            System.err.println("Error al cargar dashboard: " + ex.getMessage());
        }
    }
}