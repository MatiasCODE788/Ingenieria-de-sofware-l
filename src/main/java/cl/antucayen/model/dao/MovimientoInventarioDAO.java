package cl.antucayen.model.dao;

import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAO {

    private static final String SELECT_BASE = """
            SELECT m.*, p.nombre AS nombre_producto,
                   COALESCE(NULLIF(a.nombre_usuario,''), NULLIF(u.nombre_completo,''), u.username) AS nombre_usuario,
                   u.username AS username_usuario
            FROM movimiento_inventario m
            JOIN producto p ON m.sku = p.sku
            JOIN usuario u ON m.id_usuario = u.id_usuario
            LEFT JOIN ajuste_inventario a ON m.id_ajuste = a.id_ajuste
            """;

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(MovimientoInventario m) throws SQLException {
        String sql = """
            INSERT INTO movimiento_inventario
            (sku, id_usuario, id_factura, id_item_factura, id_venta, id_ajuste, tipo_movimiento,
             stock_anterior, cantidad_aplicada, stock_resultante, modalidad_ajuste, vigente)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, m.getSku());
            ps.setInt(2, m.getIdUsuario());
            setNullableInt(ps, 3, m.getIdFactura());
            setNullableInt(ps, 4, m.getIdItemFactura());
            setNullableInt(ps, 5, m.getIdVenta());
            setNullableInt(ps, 6, m.getIdAjuste());
            ps.setString(7, m.getTipoMovimiento());
            ps.setInt(8, m.getStockAnterior());
            ps.setInt(9, m.getCantidadAplicada());
            ps.setInt(10, m.getStockResultante());
            ps.setString(11, m.getModalidadAjuste());
            ps.setBoolean(12, m.isVigente());
            ps.executeUpdate();
        }
    }

    private void setNullableInt(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor != null) ps.setInt(indice, valor); else ps.setNull(indice, Types.INTEGER);
    }

    public boolean existeIngresoPorItemFactura(int idItemFactura) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM movimiento_inventario
            WHERE id_item_factura=? AND tipo_movimiento='Ingreso por compra' AND vigente=1
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idItemFactura);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public MovimientoInventario buscarIngresoVigentePorItemFactura(int idItemFactura) throws SQLException {
        String sql = SELECT_BASE + """
            WHERE m.id_item_factura=?
              AND m.tipo_movimiento='Ingreso por compra'
              AND m.vigente=1
            ORDER BY m.id_movimiento DESC
            LIMIT 1
            FOR UPDATE
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idItemFactura);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void marcarNoVigente(int idMovimiento) throws SQLException {
        String sql = "UPDATE movimiento_inventario SET vigente=0 WHERE id_movimiento=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idMovimiento);
            ps.executeUpdate();
        }
    }

    public int contarEntre(Timestamp desde, Timestamp hastaExclusivo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimiento_inventario WHERE fecha_hora >= ? AND fecha_hora < ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setTimestamp(1, desde);
            ps.setTimestamp(2, hastaExclusivo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<MovimientoInventario> listarTodos() throws SQLException {
        String sql = SELECT_BASE + " ORDER BY m.fecha_hora DESC, m.id_movimiento DESC";
        List<MovimientoInventario> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<MovimientoInventario> filtrar(String sku, String tipo,
                                              Timestamp desde, Timestamp hasta) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1=1");
        if (sku != null && !sku.isEmpty()) sql.append(" AND m.sku = ?");
        if (tipo != null && !tipo.isEmpty()) sql.append(" AND m.tipo_movimiento = ?");
        if (desde != null) sql.append(" AND m.fecha_hora >= ?");
        if (hasta != null) sql.append(" AND m.fecha_hora < ?");
        sql.append(" ORDER BY m.fecha_hora DESC, m.id_movimiento DESC");

        List<MovimientoInventario> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql.toString())) {
            int i = 1;
            if (sku != null && !sku.isEmpty()) ps.setString(i++, sku);
            if (tipo != null && !tipo.isEmpty()) ps.setString(i++, tipo);
            if (desde != null) ps.setTimestamp(i++, desde);
            if (hasta != null) ps.setTimestamp(i, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private MovimientoInventario mapear(ResultSet rs) throws SQLException {
        MovimientoInventario movimiento = new MovimientoInventario();
        Timestamp ts = rs.getTimestamp("fecha_hora");
        movimiento.setIdMovimiento(rs.getInt("id_movimiento"));
        movimiento.setSku(rs.getString("sku"));
        movimiento.setIdUsuario(rs.getInt("id_usuario"));
        movimiento.setIdFactura(getNullableInt(rs, "id_factura"));
        movimiento.setIdItemFactura(getNullableInt(rs, "id_item_factura"));
        movimiento.setIdVenta(getNullableInt(rs, "id_venta"));
        movimiento.setIdAjuste(getNullableInt(rs, "id_ajuste"));
        movimiento.setTipoMovimiento(rs.getString("tipo_movimiento"));
        movimiento.setFechaHora(ts != null ? ts.toLocalDateTime() : null);
        movimiento.setStockAnterior(rs.getInt("stock_anterior"));
        movimiento.setCantidadAplicada(rs.getInt("cantidad_aplicada"));
        movimiento.setStockResultante(rs.getInt("stock_resultante"));
        movimiento.setModalidadAjuste(rs.getString("modalidad_ajuste"));
        movimiento.setVigente(rs.getBoolean("vigente"));
        movimiento.setNombreProducto(rs.getString("nombre_producto"));
        movimiento.setNombreUsuario(rs.getString("nombre_usuario"));
        movimiento.setUsernameUsuario(rs.getString("username_usuario"));
        return movimiento;
    }

    private Integer getNullableInt(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }
}
