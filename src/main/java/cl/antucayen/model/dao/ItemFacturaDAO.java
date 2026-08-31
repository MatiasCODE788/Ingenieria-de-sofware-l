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
            INSERT INTO item_factura (id_factura, sku, cantidad_facturada,
            precio_unitario_compra, estado_item)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, item.getIdFactura());
            if (item.getSku() != null) ps.setString(2, item.getSku());
            else ps.setNull(2, Types.VARCHAR);
            ps.setInt   (3, item.getCantidadFacturada());
            ps.setInt   (4, item.getPrecioUnitarioCompra());
            ps.setString(5, item.getEstadoItem());
            ps.executeUpdate();
        }
    }

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

    private ItemFactura mapear(ResultSet rs) throws SQLException {
        return new ItemFactura(
                rs.getInt   ("id_item"),
                rs.getInt   ("id_factura"),
                rs.getString("sku"),
                rs.getInt   ("cantidad_facturada"),
                rs.getInt   ("precio_unitario_compra"),
                rs.getString("estado_item")
        );
    }
}
