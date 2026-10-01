package cl.antucayen.model.dao;

import cl.antucayen.model.dto.ReporteTabular;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAO {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    /** RF-57: todos los productos activos. */
    public ReporteTabular reporteStockActual() throws SQLException {
        String sql = """
                SELECT sku, nombre, unidad_medida, stock_actual
                FROM producto
                WHERE estado='Activo'
                ORDER BY nombre, sku
                """;
        List<List<Object>> filas = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(List.of(
                        rs.getString("sku"),
                        rs.getString("nombre"),
                        rs.getString("unidad_medida"),
                        rs.getInt("stock_actual")));
            }
        }
        return new ReporteTabular("Reporte de stock actual",
                List.of("SKU", "Nombre", "Unidad de medida", "Stock actual"), filas);
    }

    /**
     * RF-58: códigos internos detectados en facturas que no tienen una
     * equivalencia vigente para el mismo proveedor. La descripción del ítem se
     * utiliza como nombre de producto pendiente de homologación.
     */
    public ReporteTabular reporteEquivalenciasFaltantes() throws SQLException {
        String sql = """
                SELECT DISTINCT p.nombre AS proveedor,
                       i.codigo_interno_proveedor AS codigo_interno,
                       COALESCE(NULLIF(i.descripcion,''), '(Sin descripción)') AS nombre_producto
                FROM item_factura i
                JOIN factura f ON f.id_factura=i.id_factura
                JOIN proveedor p ON p.id_proveedor=f.id_proveedor
                LEFT JOIN equivalencia e
                  ON e.id_proveedor=f.id_proveedor
                 AND e.codigo_interno_proveedor=i.codigo_interno_proveedor
                WHERE i.codigo_interno_proveedor IS NOT NULL
                  AND TRIM(i.codigo_interno_proveedor) <> ''
                  AND e.sku IS NULL
                ORDER BY p.nombre, i.codigo_interno_proveedor
                """;
        List<List<Object>> filas = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(List.of(
                        rs.getString("proveedor"),
                        rs.getString("codigo_interno"),
                        rs.getString("nombre_producto")));
            }
        }
        return new ReporteTabular("Reporte de equivalencias faltantes",
                List.of("Proveedor", "Código interno", "Nombre del producto"), filas);
    }

    /** RF-59: estado y rango de fechas combinables + observados en tiempo real. */
    public ReporteTabular reporteFacturas(String estado, LocalDate desde, LocalDate hasta) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id_factura, f.numero_factura, p.nombre AS proveedor,
                       f.fecha_emision, f.estado,
                       SUM(CASE WHEN i.estado_item='Observado' THEN 1 ELSE 0 END) AS observados
                FROM factura f
                JOIN proveedor p ON p.id_proveedor=f.id_proveedor
                LEFT JOIN item_factura i ON i.id_factura=f.id_factura
                WHERE 1=1
                """);
        if (estado != null && !estado.isBlank() && !"Todos".equalsIgnoreCase(estado)) {
            sql.append(" AND f.estado=?");
        }
        if (desde != null) sql.append(" AND f.fecha_emision>=?");
        if (hasta != null) sql.append(" AND f.fecha_emision<=?");
        sql.append(" GROUP BY f.id_factura, f.numero_factura, p.nombre, f.fecha_emision, f.estado");
        sql.append(" ORDER BY f.fecha_emision DESC, f.id_factura DESC");

        List<List<Object>> filas = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql.toString())) {
            int idx = 1;
            if (estado != null && !estado.isBlank() && !"Todos".equalsIgnoreCase(estado)) {
                ps.setString(idx++, estado);
            }
            if (desde != null) ps.setDate(idx++, Date.valueOf(desde));
            if (hasta != null) ps.setDate(idx, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date fecha = rs.getDate("fecha_emision");
                    filas.add(List.of(
                            rs.getString("numero_factura"),
                            rs.getString("proveedor"),
                            fecha == null ? "" : fecha.toLocalDate().format(FECHA),
                            rs.getString("estado"),
                            rs.getInt("observados")));
                }
            }
        }
        return new ReporteTabular("Reporte de facturas",
                List.of("Número", "Proveedor", "Fecha", "Estado", "Ítems observados"), filas);
    }

    /** RF-60: rango obligatorio y todos los tipos de movimiento. */
    public ReporteTabular reporteMovimientos(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = """
                SELECT m.fecha_hora, m.tipo_movimiento, m.sku, p.nombre AS producto,
                       m.cantidad_aplicada,
                       COALESCE(NULLIF(a.nombre_usuario,''), NULLIF(u.nombre_completo,''), u.username) AS usuario
                FROM movimiento_inventario m
                JOIN producto p ON p.sku=m.sku
                JOIN usuario u ON u.id_usuario=m.id_usuario
                LEFT JOIN ajuste_inventario a ON a.id_ajuste=m.id_ajuste
                WHERE m.fecha_hora>=? AND m.fecha_hora<?
                ORDER BY m.fecha_hora DESC, m.id_movimiento DESC
                """;
        List<List<Object>> filas = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    filas.add(List.of(
                            ts == null ? "" : ts.toLocalDateTime().format(FECHA_HORA),
                            rs.getString("tipo_movimiento"),
                            rs.getString("sku"),
                            rs.getString("producto"),
                            rs.getInt("cantidad_aplicada"),
                            rs.getString("usuario")));
                }
            }
        }
        return new ReporteTabular("Reporte de movimientos",
                List.of("Fecha/Hora", "Tipo", "SKU", "Producto", "Cantidad aplicada", "Usuario"), filas);
    }
}
