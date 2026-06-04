package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Equivalencia;
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

    // ── EQUIVALENCIAS ────────────────────────────────────────────
    public void insertarEquivalencia(Equivalencia e) throws SQLException {
        String sql = "INSERT INTO equivalencia (id_proveedor, codigo_interno_proveedor, sku) VALUES (?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, e.getIdProveedor());
            ps.setString(2, e.getCodigoInternoProveedor());
            ps.setString(3, e.getSku());
            ps.executeUpdate();
        }
    }

    public void eliminarEquivalencia(int idProveedor, String codigoInterno) throws SQLException {
        String sql = "DELETE FROM equivalencia WHERE id_proveedor=? AND codigo_interno_proveedor=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, idProveedor);
            ps.setString(2, codigoInterno);
            ps.executeUpdate();
        }
    }

    public List<Equivalencia> listarEquivalencias(int idProveedor) throws SQLException {
        String sql = "SELECT * FROM equivalencia WHERE id_proveedor=?";
        List<Equivalencia> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Equivalencia(
                        rs.getInt   ("id_proveedor"),
                        rs.getString("codigo_interno_proveedor"),
                        rs.getString("sku")
                ));
            }
        }
        return lista;
    }

    public boolean existeEquivalencia(int idProveedor, String codigoInterno) throws SQLException {
        String sql = "SELECT COUNT(*) FROM equivalencia WHERE id_proveedor=? AND codigo_interno_proveedor=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, idProveedor);
            ps.setString(2, codigoInterno);
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