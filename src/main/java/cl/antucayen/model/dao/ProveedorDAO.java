package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(Proveedor p) throws SQLException {
        String sql = "INSERT INTO proveedor (rut, nombre, telefono, correo_electronico) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getRut());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getCorreoElectronico());
            ps.executeUpdate();
        }
    }

    public void actualizar(Proveedor p) throws SQLException {
        String sql = "UPDATE proveedor SET rut=?, nombre=?, telefono=?, correo_electronico=? WHERE id_proveedor=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getRut());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getCorreoElectronico());
            ps.setInt   (5, p.getIdProveedor());
            ps.executeUpdate();
        }
    }

    public Proveedor buscarPorId(int idProveedor) throws SQLException {
        String sql = "SELECT * FROM proveedor WHERE id_proveedor=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public List<Proveedor> listarTodos() throws SQLException {
        String sql = "SELECT * FROM proveedor ORDER BY nombre";
        List<Proveedor> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public boolean existeNombre(String nombre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM proveedor WHERE nombre=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        return new Proveedor(
                rs.getInt   ("id_proveedor"),
                rs.getString("rut"),
                rs.getString("nombre"),
                rs.getString("telefono"),
                rs.getString("correo_electronico")
        );
    }
}