package cl.antucayen.model.dao;

import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.util.DBConexion;

import java.sql.*;

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
}
