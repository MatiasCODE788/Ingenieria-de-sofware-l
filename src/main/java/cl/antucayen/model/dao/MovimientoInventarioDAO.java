package cl.antucayen.model.dao;

import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(MovimientoInventario m) throws SQLException {
        String sql = """
            INSERT INTO movimiento_inventario
            (sku, id_usuario, id_factura, id_venta, tipo_movimiento,
             stock_anterior, cantidad_aplicada, stock_resultante, modalidad_ajuste)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, m.getSku());
            ps.setInt   (2, m.getIdUsuario());
            if (m.getIdFactura() != null) ps.setInt(3, m.getIdFactura());
            else ps.setNull(3, Types.INTEGER);
            if (m.getIdVenta() != null) ps.setInt(4, m.getIdVenta());
            else ps.setNull(4, Types.INTEGER);
            ps.setString(5, m.getTipoMovimiento());
            ps.setInt   (6, m.getStockAnterior());
            ps.setInt   (7, m.getCantidadAplicada());
            ps.setInt   (8, m.getStockResultante());
            ps.setString(9, m.getModalidadAjuste());
            ps.executeUpdate();
        }
    }

    public List<MovimientoInventario> listarTodos() throws SQLException {
        String sql = """
            SELECT m.*, p.nombre AS nombre_producto
            FROM movimiento_inventario m
            JOIN producto p ON m.sku = p.sku
            ORDER BY m.fecha_hora DESC
            """;
        List<MovimientoInventario> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<MovimientoInventario> filtrar(String sku, String tipo,
                                              Timestamp desde, Timestamp hasta) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, p.nombre AS nombre_producto
            FROM movimiento_inventario m
            JOIN producto p ON m.sku = p.sku
            WHERE 1=1
            """);
        if (sku  != null && !sku.isEmpty())  sql.append(" AND m.sku = ?");
        if (tipo != null && !tipo.isEmpty()) sql.append(" AND m.tipo_movimiento = ?");
        if (desde != null)                   sql.append(" AND m.fecha_hora >= ?");
        if (hasta != null)                   sql.append(" AND m.fecha_hora <= ?");
        sql.append(" ORDER BY m.fecha_hora DESC");

        List<MovimientoInventario> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql.toString())) {
            int i = 1;
            if (sku  != null && !sku.isEmpty())  ps.setString   (i++, sku);
            if (tipo != null && !tipo.isEmpty()) ps.setString   (i++, tipo);
            if (desde != null)                   ps.setTimestamp(i++, desde);
            if (hasta != null)                   ps.setTimestamp(i,   hasta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private MovimientoInventario mapear(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_hora");
        return new MovimientoInventario(
                rs.getInt   ("id_movimiento"),
                rs.getString("sku"),
                rs.getInt   ("id_usuario"),
                rs.getObject("id_factura") != null ? rs.getInt("id_factura") : null,
                rs.getObject("id_venta") != null ? rs.getInt("id_venta") : null,
                rs.getString("tipo_movimiento"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getInt   ("stock_anterior"),
                rs.getInt   ("cantidad_aplicada"),
                rs.getInt   ("stock_resultante"),
                rs.getString("modalidad_ajuste"),
                rs.getString("nombre_producto")
        );
    }
}