package cl.antucayen.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Verifica tempranamente que la aplicación use el esquema definitivo. */
public final class VerificadorEsquema {

    public static final int VERSION_ESQUEMA = 20260913;
    private static volatile boolean verificado;
    private static final Map<String, List<String>> COLUMNAS_REQUERIDAS = crearContrato();

    private VerificadorEsquema() {}

    public static synchronized void verificar() throws SQLException {
        if (verificado) return;
        Connection conn = DBConexion.getInstancia().getConexion();

        if (!existeTabla(conn, "app_schema_version")) {
            throw desactualizada("falta la tabla app_schema_version");
        }
        int version = leerVersion(conn);
        if (version < VERSION_ESQUEMA) {
            throw desactualizada("versión instalada " + version
                    + ", versión requerida " + VERSION_ESQUEMA);
        }

        for (Map.Entry<String, List<String>> entry : COLUMNAS_REQUERIDAS.entrySet()) {
            if (!existeTabla(conn, entry.getKey())) {
                throw desactualizada("falta la tabla " + entry.getKey());
            }
            for (String columna : entry.getValue()) {
                if (!existeColumna(conn, entry.getKey(), columna)) {
                    throw desactualizada("falta " + entry.getKey() + "." + columna);
                }
            }
        }
        verificarRoles(conn);
        verificado = true;
    }

    private static int leerVersion(Connection conn) throws SQLException {
        String sql = "SELECT MAX(version) FROM app_schema_version";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || rs.getObject(1) == null) {
                throw desactualizada("app_schema_version no contiene una versión");
            }
            return rs.getInt(1);
        }
    }

    private static void verificarRoles(Connection conn) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total,
                       SUM(nombre_perfil IN ('Administrador','Bodeguero','Cajero')) AS validos
                FROM perfil
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || rs.getInt("total") != 3 || rs.getInt("validos") != 3) {
                throw desactualizada("el catálogo de perfiles no coincide con Administrador/Bodeguero/Cajero");
            }
        }
    }

    private static boolean existeTabla(Connection conn, String tabla) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private static boolean existeColumna(Connection conn, String tabla, String columna) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private static SQLException desactualizada(String detalle) {
        return new SQLException("La base de datos no corresponde a la versión actual: " + detalle
                + ". Para una instalación nueva ejecute database/00_instalacion_completa.sql; "
                + "para una base existente ejecute "
                + "database/migrations/001_actualizacion_v5_definitiva.sql.");
    }

    private static Map<String, List<String>> crearContrato() {
        Map<String, List<String>> c = new LinkedHashMap<>();
        c.put("perfil", List.of("id_perfil", "nombre_perfil"));
        c.put("usuario", List.of("id_usuario", "nombre_completo", "username", "password_hash",
                "estado_activo", "id_perfil"));
        c.put("producto", List.of("sku", "nombre", "codigo_barras", "unidad_medida",
                "precio_venta", "stock_actual", "estado"));
        c.put("proveedor", List.of("id_proveedor", "rut", "nombre", "telefono", "correo_electronico"));
        c.put("producto_proveedor", List.of("sku", "id_proveedor"));
        c.put("auditoria_proveedor", List.of("id_auditoria", "id_proveedor", "id_usuario",
                "fecha_hora", "campo_modificado", "valor_anterior", "valor_nuevo"));
        c.put("equivalencia", List.of("id_proveedor", "codigo_interno_proveedor", "sku"));
        c.put("factura", List.of("id_factura", "numero_factura", "fecha_emision", "estado",
                "ruta_archivo_digital", "valor_total", "id_proveedor", "id_usuario"));
        c.put("item_factura", List.of("id_item", "id_factura", "codigo_interno_proveedor",
                "descripcion", "sku", "cantidad_facturada", "precio_unitario_compra", "estado_item"));
        c.put("venta", List.of("id_venta", "fecha_hora", "id_usuario", "medio_pago", "monto_total", "estado"));
        c.put("item_venta", List.of("id_item", "id_venta", "sku", "cantidad",
                "precio_unitario_venta", "subtotal"));
        c.put("pago_venta", List.of("id_pago", "id_venta", "medio_pago", "monto"));
        c.put("ajuste_inventario", List.of("id_ajuste", "fecha_hora", "modalidad_ajuste",
                "estado_ajuste", "id_usuario", "nombre_usuario"));
        c.put("item_ajuste", List.of("id_item_ajuste", "id_ajuste", "sku", "cantidad_aplicada",
                "stock_anterior", "stock_resultante"));
        c.put("movimiento_inventario", List.of("id_movimiento", "sku", "id_usuario", "id_factura",
                "id_item_factura", "id_venta", "id_ajuste", "tipo_movimiento", "fecha_hora",
                "stock_anterior", "cantidad_aplicada", "stock_resultante", "modalidad_ajuste", "vigente"));
        return Map.copyOf(c);
    }
}
