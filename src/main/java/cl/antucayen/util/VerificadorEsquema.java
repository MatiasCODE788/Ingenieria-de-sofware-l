package cl.antucayen.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifica el contrato estructural real de la base de datos.
 *
 * <p>Para una instalación basada en un único SQL es más robusto comprobar
 * directamente las tablas, columnas y roles que la aplicación necesita, sin
 * mantener metadatos de versionado separados.</p>
 */
public final class VerificadorEsquema {

    private static volatile boolean verificado;
    private static final Map<String, List<String>> COLUMNAS_REQUERIDAS = crearContrato();
    private VerificadorEsquema() {}

    public static synchronized void verificar() throws SQLException {
        if (verificado) return;
        Connection conn = DBConexion.getInstancia().getConexion();


        for (Map.Entry<String, List<String>> entry : COLUMNAS_REQUERIDAS.entrySet()) {
            if (!existeTabla(conn, entry.getKey())) {
                throw esquemaIncompatible("falta la tabla " + entry.getKey());
            }
            for (String columna : entry.getValue()) {
                if (!existeColumna(conn, entry.getKey(), columna)) {
                    throw esquemaIncompatible("falta " + entry.getKey() + "." + columna);
                }
            }
        }

        verificarRoles(conn);
        verificado = true;
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
                throw esquemaIncompatible(
                        "el catálogo de perfiles no coincide con Administrador/Bodeguero/Cajero");
            }
        }
    }

    private static boolean existeTabla(Connection conn, String tabla) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private static boolean existeColumna(Connection conn, String tabla, String columna)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private static SQLException esquemaIncompatible(String detalle) {
        return new SQLException(
                "La base de datos no corresponde al esquema requerido: " + detalle
                        + ". Para una instalación limpia ejecuta únicamente "
                        + "database/Antucayen_Instalacion_Unica.sql.");
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
        c.put("venta", List.of("id_venta", "fecha_inicio", "fecha_confirmacion", "id_usuario",
                "medio_pago", "monto_total", "monto_recibido", "vuelto", "estado"));
        c.put("item_venta", List.of("id_item", "id_venta", "sku", "cantidad",
                "precio_unitario_venta", "subtotal"));
        c.put("pago_venta", List.of("id_pago", "id_venta", "medio_pago", "monto"));
        c.put("ajuste_inventario", List.of("id_ajuste", "fecha_hora", "modalidad_ajuste",
                "estado_ajuste", "id_usuario", "nombre_usuario"));
        c.put("movimiento_inventario", List.of("id_movimiento", "sku", "id_usuario",
                "id_item_factura", "id_item_venta", "id_ajuste", "tipo_movimiento", "fecha_hora",
                "stock_anterior", "cantidad_aplicada", "stock_resultante", "motivo", "vigente"));
        c.put("log_archivo", List.of("id_log", "fecha_hora", "id_usuario", "nombre_usuario",
                "nombre_archivo", "tipo_operacion", "formato", "resultado", "detalle"));
        return Map.copyOf(c);
    }
}
