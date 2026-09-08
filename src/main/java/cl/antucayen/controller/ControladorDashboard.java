package cl.antucayen.controller;

import cl.antucayen.model.dao.FacturaDAO;
import cl.antucayen.model.dao.ItemVentaDAO;
import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.dao.VentaDAO;
import cl.antucayen.view.VDashboard;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ControladorDashboard {

    private final VDashboard              vista;
    private final ProductoDAO             productoDAO  = new ProductoDAO();
    private final FacturaDAO              facturaDAO   = new FacturaDAO();
    private final MovimientoInventarioDAO movDAO       = new MovimientoInventarioDAO();
    private final VentaDAO                ventaDAO     = new VentaDAO();
    private final ItemVentaDAO            itemVentaDAO = new ItemVentaDAO();

    public ControladorDashboard(VDashboard vista) {
        this.vista = vista;
        cargarEstadisticas();
    }

    /** Permite refrescar el dashboard (por ejemplo, al volver a mostrar el panel). */
    public void refrescar() {
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

            // --- Ventas de hoy, por medio de pago ---
            Map<String, Integer> ventasHoyPorMedio = ventaDAO.totalHoyPorMedioPago();
            int totalHoy = ventasHoyPorMedio.values().stream().mapToInt(Integer::intValue).sum();
            vista.setVentasHoy(
                    totalHoy,
                    ventasHoyPorMedio.getOrDefault("Efectivo", 0),
                    ventasHoyPorMedio.getOrDefault("Débito", 0),
                    ventasHoyPorMedio.getOrDefault("Crédito", 0)
            );

            // --- Ventas del mes ---
            vista.setVentasMes(ventaDAO.totalMesActual());

            // --- Stock bajo ---
            int stockBajo = productoDAO.contarStockBajo(ProductoDAO.UMBRAL_STOCK_BAJO);
            vista.setStockBajo(stockBajo);

            // --- Top 5 productos más vendidos del mes ---
            List<ItemVentaDAO.ProductoVendido> top5 = itemVentaDAO.productosMasVendidosDelMes(5);
            vista.setTopProductos(top5);

        } catch (SQLException ex) {
            System.err.println("Error al cargar dashboard: " + ex.getMessage());
        }
    }
}