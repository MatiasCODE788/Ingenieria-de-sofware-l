package cl.antucayen.model.service;

import cl.antucayen.model.dao.AjusteInventarioDAO;
import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.AjusteInventario;
import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class ServicioInventario {

    private final ProductoDAO            productoDAO     = new ProductoDAO();
    private final MovimientoInventarioDAO movDAO         = new MovimientoInventarioDAO();
    private final AjusteInventarioDAO    ajusteDAO       = new AjusteInventarioDAO();

    public void ajustarStock(String sku, int cantidad,
                             String modalidad) throws SQLException {
        Producto p = productoDAO.buscarPorSku(sku);
        if (p == null) throw new IllegalArgumentException("SKU no encontrado: " + sku);

        int stockAnterior = p.getStockActual();
        int stockResultante;

        if (modalidad.contains("Sumar")) {
            stockResultante = stockAnterior + cantidad;
        } else if (modalidad.contains("Restar")) {
            stockResultante = stockAnterior - cantidad;
        } else {
            stockResultante = cantidad;
        }

        if (stockResultante < 0)
            throw new IllegalArgumentException("El stock resultante no puede ser negativo para SKU: " + sku);

        productoDAO.actualizarStock(sku, stockResultante);

        MovimientoInventario mov = new MovimientoInventario();
        mov.setSku(sku);
        mov.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
        mov.setTipoMovimiento(cantidad >= 0 ? "Ajuste positivo" : "Ajuste negativo");
        mov.setStockAnterior(stockAnterior);
        mov.setCantidadAplicada(cantidad);
        mov.setStockResultante(stockResultante);
        mov.setModalidadAjuste(modalidad);
        movDAO.insertar(mov);
    }

    public int crearCabeceraAjuste(String modalidad) throws SQLException {
        AjusteInventario ajuste = new AjusteInventario();
        ajuste.setModalidadAjuste(modalidad);
        ajuste.setEstadoAjuste("Aplicado");
        ajuste.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
        return ajusteDAO.insertar(ajuste);
    }

    public void registrarItemAjuste(int idAjuste, String sku, int cantidad,
                                    int stockAnterior, int stockResultante) throws SQLException {
        ajusteDAO.insertarItem(idAjuste, sku, cantidad, stockAnterior, stockResultante);
    }

    public List<MovimientoInventario> listarMovimientos() throws SQLException {
        return movDAO.listarTodos();
    }

    public List<MovimientoInventario> filtrarMovimientos(String sku, String tipo,
                                                         Timestamp desde, Timestamp hasta) throws SQLException {
        return movDAO.filtrar(sku, tipo, desde, hasta);
    }
}