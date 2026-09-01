package cl.antucayen.model.dao;

import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemFacturaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(ItemFactura item) throws SQLException {
        String sql = """
            INSERT INTO item_factura (id_factura, codigo_interno_proveedor, sku,
            cantidad_facturada, precio_unitario_compra, estado_item)
            VALUES (?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, item.getIdFactura());
            if (item.getCodigoInternoProveedor() != null)
                ps.setString(2, item.getCodigoInternoProveedor());
            else ps.setNull(2, Types.VARCHAR);
            if (item.getSku() != null) ps.setString(3, item.getSku());
            else ps.setNull(3, Types.VARCHAR);
            ps.setInt   (4, item.getCantidadFacturada());
            ps.setInt   (5, item.getPrecioUnitarioCompra());
            ps.setString(6, item.getEstadoItem());
            ps.executeUpdate();
        }
    }

    /** Actualiza el SKU resuelto y el estado de un ítem (usado al corregir o reprocesar). */
    public void actualizarSkuYEstado(int idItem, String sku, String estadoItem) throws SQLException {
        String sql = "UPDATE item_factura SET sku=?, estado_item=? WHERE id_item=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            if (sku != null) ps.setString(1, sku); else ps.setNull(1, Types.VARCHAR);
            ps.setString(2, estadoItem);
            ps.setInt   (3, idItem);
            ps.executeUpdate();
        }
    }

    public List<ItemFactura> listarPorFactura(int idFactura) throws SQLException {
        String sql = "SELECT * FROM item_factura WHERE id_factura=? ORDER BY id_item";
        List<ItemFactura> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public ItemFactura buscarPorId(int idItem) throws SQLException {
        String sql = "SELECT * FROM item_factura WHERE id_item=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idItem);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    private ItemFactura mapear(ResultSet rs) throws SQLException {
        return new ItemFactura(
                rs.getInt   ("id_item"),
                rs.getInt   ("id_factura"),
                rs.getString("codigo_interno_proveedor"),
                rs.getString("sku"),
                rs.getInt   ("cantidad_facturada"),
                rs.getInt   ("precio_unitario_compra"),
                rs.getString("estado_item")
        );
    }
}