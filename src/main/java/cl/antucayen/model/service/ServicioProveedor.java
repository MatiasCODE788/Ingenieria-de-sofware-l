package cl.antucayen.model.service;

import cl.antucayen.model.dao.ProveedorDAO;
import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.model.entity.Proveedor;

import java.sql.SQLException;
import java.util.List;

public class ServicioProveedor {

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    public void registrar(Proveedor p) throws SQLException {
        if (p.getNombre() == null || p.getNombre().isEmpty())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (p.getRut() == null || p.getRut().isEmpty())
            throw new IllegalArgumentException("El RUT es obligatorio");
        if (proveedorDAO.existeNombre(p.getNombre()))
            throw new IllegalStateException("Ya existe un proveedor con ese nombre");
        proveedorDAO.insertar(p);
    }

    public void modificar(Proveedor p) throws SQLException {
        if (p.getNombre() == null || p.getNombre().isEmpty())
            throw new IllegalArgumentException("El nombre es obligatorio");
        proveedorDAO.actualizar(p);
    }

    public Proveedor buscarPorId(int id) throws SQLException {
        return proveedorDAO.buscarPorId(id);
    }

    public List<Proveedor> listarTodos() throws SQLException {
        return proveedorDAO.listarTodos();
    }

    public void agregarEquivalencia(Equivalencia e) throws SQLException {
        if (proveedorDAO.existeEquivalencia(e.getIdProveedor(), e.getCodigoInternoProveedor()))
            throw new IllegalStateException("Ya existe esa equivalencia para este proveedor");
        proveedorDAO.insertarEquivalencia(e);
    }

    public void eliminarEquivalencia(int idProveedor, String codigoInterno) throws SQLException {
        proveedorDAO.eliminarEquivalencia(idProveedor, codigoInterno);
    }

    public List<Equivalencia> listarEquivalencias(int idProveedor) throws SQLException {
        return proveedorDAO.listarEquivalencias(idProveedor);
    }
}