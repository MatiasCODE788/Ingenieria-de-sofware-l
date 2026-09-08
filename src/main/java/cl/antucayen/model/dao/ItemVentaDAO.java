package cl.antucayen.model.dao;

import cl.antucayen.model.entity.ItemVenta;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemVentaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(ItemVenta item) throws SQLException {
        String sql = "INSERT INTO item_venta (id_venta, sku, cantidad, precio_unitario_venta, subtotal) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, item.getIdVenta());
            ps.setString(2, item.getSku());
            ps.setInt   (3, item.getCantidad());
            ps.setInt   (4, item.getPrecioUnitarioVenta());
            ps.setInt   (5, item.getSubtotal());
            ps.executeUpdate();
        }
    }

    public List<ItemVenta> listarPorVenta(int idVenta) throws SQLException {
        String sql = """
            SELECT iv.*, p.nombre AS nombre_producto
            FROM item_venta iv
            JOIN producto p ON iv.sku = p.sku
            WHERE iv.id_venta=?
            """;
        List<ItemVenta> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Registro simple para el ranking de productos más vendidos del dashboard. */
    public record ProductoVendido(String sku, String nombre, int cantidadTotal, int montoTotal) {}

    /** Top N productos más vendidos en el mes actual (solo ventas Pagadas). */
    public List<ProductoVendido> productosMasVendidosDelMes(int limite) throws SQLException {
        String sql = """
            SELECT iv.sku, p.nombre, SUM(iv.cantidad) AS cantidad_total, SUM(iv.subtotal) AS monto_total
            FROM item_venta iv
            JOIN venta v ON iv.id_venta = v.id_venta
            JOIN producto p ON iv.sku = p.sku
            WHERE v.estado = 'Pagada'
              AND YEAR(v.fecha_hora) = YEAR(CURDATE()) AND MONTH(v.fecha_hora) = MONTH(CURDATE())
            GROUP BY iv.sku, p.nombre
            ORDER BY cantidad_total DESC
            LIMIT ?
            """;
        List<ProductoVendido> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(new ProductoVendido(
                        rs.getString("sku"), rs.getString("nombre"),
                        rs.getInt("cantidad_total"), rs.getInt("monto_total")));
        }
        return lista;
    }

    private ItemVenta mapear(ResultSet rs) throws SQLException {
        return new ItemVenta(
                rs.getInt   ("id_item"),
                rs.getInt   ("id_venta"),
                rs.getString("sku"),
                rs.getInt   ("cantidad"),
                rs.getInt   ("precio_unitario_venta"),
                rs.getInt   ("subtotal"),
                rs.getString("nombre_producto")
        );
    }
}