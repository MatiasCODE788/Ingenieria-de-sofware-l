package cl.antucayen.model.service;

import cl.antucayen.model.dao.FacturaDAO;
import cl.antucayen.model.dao.ItemVentaDAO;
import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.dao.VentaDAO;
import cl.antucayen.model.dto.ProductoVendido;
import cl.antucayen.security.Autorizacion;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Estadísticas agregadas reservadas al Dashboard administrativo. */
public class ServicioDashboard {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final ItemVentaDAO itemVentaDAO = new ItemVentaDAO();

    public int productosActivos() throws SQLException {
        verificarAccesoDashboard();
        return (int) productoDAO.listarTodos().stream()
                .filter(p -> "Activo".equals(p.getEstado()))
                .count();
    }

    public int facturasPendientes() throws SQLException {
        verificarAccesoDashboard();
        return facturaDAO.listarPorEstado("Pendiente").size();
    }

    public int movimientosHoy() throws SQLException {
        verificarAccesoDashboard();
        LocalDate hoy = LocalDate.now();
        Timestamp inicio = Timestamp.valueOf(hoy.atStartOfDay());
        Timestamp finExclusivo = Timestamp.valueOf(hoy.plusDays(1).atStartOfDay());
        return movimientoDAO.contarEntre(inicio, finExclusivo);
    }

    public Map<String, Integer> ventasHoyPorMedioPago() throws SQLException {
        verificarAccesoDashboard();
        return ventaDAO.totalHoyPorMedioPago();
    }

    public int ventasMesActual() throws SQLException {
        verificarAccesoDashboard();
        return ventaDAO.totalMesActual();
    }

    public int stockBajo() throws SQLException {
        verificarAccesoDashboard();
        return productoDAO.contarStockBajo(ProductoDAO.UMBRAL_STOCK_BAJO);
    }

    public List<ProductoVendido> productosMasVendidosDelMes(int limite) throws SQLException {
        verificarAccesoDashboard();
        return itemVentaDAO.productosMasVendidosDelMes(limite);
    }
    private void verificarAccesoDashboard() {
        Autorizacion.verificarAdministrador(
                "Solo el Administrador puede acceder al Dashboard y a reportes globales");
    }

}
