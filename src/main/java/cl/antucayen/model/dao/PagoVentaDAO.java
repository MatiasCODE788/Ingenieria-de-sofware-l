package cl.antucayen.model.dao;

import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoVentaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(PagoVenta p) throws SQLException {
        String sql = "INSERT INTO pago_venta (id_venta, medio_pago, monto) VALUES (?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, p.getIdVenta());
            ps.setString(2, p.getMedioPago());
            ps.setInt   (3, p.getMonto());
            ps.executeUpdate();
        }
    }

    /** Detalle de pagos (medio + monto) de una venta específica, ej. para mostrarlo en un detalle. */
    public List<PagoVenta> listarPorVenta(int idVenta) throws SQLException {
        String sql = "SELECT * FROM pago_venta WHERE id_venta=? ORDER BY id_pago";
        List<PagoVenta> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private PagoVenta mapear(ResultSet rs) throws SQLException {
        return new PagoVenta(
                rs.getInt   ("id_pago"),
                rs.getInt   ("id_venta"),
                rs.getString("medio_pago"),
                rs.getInt   ("monto")
        );
    }
}