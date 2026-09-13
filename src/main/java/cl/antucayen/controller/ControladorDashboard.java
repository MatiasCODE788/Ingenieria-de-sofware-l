package cl.antucayen.controller;

import cl.antucayen.model.dto.ProductoVendido;
import cl.antucayen.model.service.ServicioDashboard;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VDashboard;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ControladorDashboard {

    private final VDashboard vista;
    private final ServicioDashboard servicio = new ServicioDashboard();

    public ControladorDashboard(VDashboard vista) {
        Autorizacion.verificarAdministrador("Solo el Administrador puede acceder al Dashboard y a reportes globales");
        this.vista = vista;
        cargarEstadisticas();
    }

    public void refrescar() {
        cargarEstadisticas();
    }

    private void cargarEstadisticas() {
        try {
            vista.setProductosActivos(servicio.productosActivos());
            vista.setFacturasPendientes(servicio.facturasPendientes());
            vista.setMovimientosHoy(servicio.movimientosHoy());

            Map<String, Integer> ventasHoyPorMedio = servicio.ventasHoyPorMedioPago();
            int totalHoy = ventasHoyPorMedio.values().stream().mapToInt(Integer::intValue).sum();
            vista.setVentasHoy(
                    totalHoy,
                    ventasHoyPorMedio.getOrDefault("Efectivo", 0),
                    ventasHoyPorMedio.getOrDefault("Débito", 0),
                    ventasHoyPorMedio.getOrDefault("Crédito", 0)
            );

            vista.setVentasMes(servicio.ventasMesActual());
            vista.setStockBajo(servicio.stockBajo());
            List<ProductoVendido> top5 = servicio.productosMasVendidosDelMes(5);
            vista.setTopProductos(top5);
        } catch (SQLException ex) {
            System.err.println("Error al cargar dashboard: " + ex.getMessage());
        }
    }
}
