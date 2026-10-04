package cl.antucayen.model.dao;

import cl.antucayen.model.dto.ProductoVendido;
import cl.antucayen.model.entity.ItemVenta;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ItemVentaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    /** Inserta el ítem y devuelve su PK para enlazar el movimiento de inventario. */
    public int insertar(ItemVenta item) throws SQLException {
        String sql = """
                INSERT INTO item_venta
                (id_venta, sku, cantidad, precio_unitario_venta, subtotal)
                VALUES (?,?,?,?,?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getIdVenta());
            ps.setString(2, item.getSku());
            ps.setInt(3, item.getCantidad());
            ps.setInt(4, item.getPrecioUnitarioVenta());
            ps.setInt(5, item.getSubtotal());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el ID del ítem de venta generado");
    }

    public List<ItemVenta> listarPorVenta(int idVenta) throws SQLException {
        String sql = """
                SELECT iv.*, p.nombre AS nombre_producto
                FROM item_venta iv
                JOIN producto p ON iv.sku = p.sku
                WHERE iv.id_venta=?
                ORDER BY iv.id_item
                """;
        List<ItemVenta> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Top N productos más vendidos en el mes actual, usando rango indexable. */
    public List<ProductoVendido> productosMasVendidosDelMes(int limite) throws SQLException {
        LocalDate hoy = LocalDate.now();
        Timestamp desde = Timestamp.valueOf(hoy.withDayOfMonth(1).atStartOfDay());
        Timestamp hasta = Timestamp.valueOf(hoy.withDayOfMonth(1).plusMonths(1).atStartOfDay());

        String sql = """
                SELECT iv.sku, p.nombre,
                       SUM(iv.cantidad) AS cantidad_total,
                       SUM(iv.subtotal) AS monto_total
                FROM venta v
                JOIN item_venta iv ON iv.id_venta = v.id_venta
                JOIN producto p ON iv.sku = p.sku
                WHERE v.estado = 'Pagada'
                  AND v.fecha_confirmacion >= ?
                  AND v.fecha_confirmacion < ?
                GROUP BY iv.sku, p.nombre
                ORDER BY cantidad_total DESC
                LIMIT ?
                """;
        List<ProductoVendido> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setTimestamp(1, desde);
            ps.setTimestamp(2, hasta);
            ps.setInt(3, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ProductoVendido(
                            rs.getString("sku"),
                            rs.getString("nombre"),
                            rs.getInt("cantidad_total"),
                            rs.getInt("monto_total")));
                }
            }
        }
        return lista;
    }

    private ItemVenta mapear(ResultSet rs) throws SQLException {
        return new ItemVenta(
                rs.getInt("id_item"),
                rs.getInt("id_venta"),
                rs.getString("sku"),
                rs.getInt("cantidad"),
                rs.getInt("precio_unitario_venta"),
                rs.getInt("subtotal"),
                rs.getString("nombre_producto"));
    }
}
