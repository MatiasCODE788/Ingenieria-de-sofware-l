package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacturaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public int insertar(Factura f) throws SQLException {
        String sql = """
            INSERT INTO factura (numero_factura, fecha_emision, estado,
            ruta_archivo_digital, id_proveedor, id_usuario)
            VALUES (?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNumeroFactura());
            ps.setDate  (2, Date.valueOf(f.getFechaEmision()));
            ps.setString(3, f.getEstado());
            ps.setString(4, f.getRutaArchivoDigital());
            ps.setInt   (5, f.getIdProveedor());
            ps.setInt   (6, f.getIdUsuario());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public void actualizarEstado(int idFactura, String estado) throws SQLException {
        String sql = "UPDATE factura SET estado=? WHERE id_factura=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt   (2, idFactura);
            ps.executeUpdate();
        }
    }

    public Factura buscarPorId(int idFactura) throws SQLException {
        String sql = """
            SELECT f.*, p.nombre AS nombre_proveedor
            FROM factura f
            JOIN proveedor p ON f.id_proveedor = p.id_proveedor
            WHERE f.id_factura=?
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public List<Factura> listarTodas() throws SQLException {
        String sql = """
            SELECT f.*, p.nombre AS nombre_proveedor
            FROM factura f
            JOIN proveedor p ON f.id_proveedor = p.id_proveedor
            ORDER BY f.fecha_emision DESC
            """;
        List<Factura> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Factura> listarPorEstado(String estado) throws SQLException {
        String sql = """
            SELECT f.*, p.nombre AS nombre_proveedor
            FROM factura f
            JOIN proveedor p ON f.id_proveedor = p.id_proveedor
            WHERE f.estado=?
            ORDER BY f.fecha_emision DESC
            """;
        List<Factura> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Factura mapear(ResultSet rs) throws SQLException {
        return new Factura(
                rs.getInt   ("id_factura"),
                rs.getString("numero_factura"),
                rs.getDate  ("fecha_emision").toLocalDate(),
                rs.getString("estado"),
                rs.getString("ruta_archivo_digital"),
                rs.getInt   ("id_proveedor"),
                rs.getInt   ("id_usuario"),
                rs.getString("nombre_proveedor")
        );
    }
}