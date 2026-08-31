package cl.antucayen.model.service;

import cl.antucayen.model.dao.AuditoriaProveedorDAO;
import cl.antucayen.model.dao.ProveedorDAO;
import cl.antucayen.model.entity.AuditoriaProveedor;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ServicioProveedor {

    private final ProveedorDAO          proveedorDAO = new ProveedorDAO();
    private final AuditoriaProveedorDAO auditoriaDAO = new AuditoriaProveedorDAO();

    public void registrar(Proveedor p) throws SQLException {
        if (p.getNombre() == null || p.getNombre().isEmpty())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (p.getRut() == null || p.getRut().isEmpty())
            throw new IllegalArgumentException("El RUT es obligatorio");
        if (proveedorDAO.existeNombre(p.getNombre()))
            throw new IllegalStateException("Ya existe un proveedor con ese nombre");
        proveedorDAO.insertar(p);
    }

    /**
     * Modifica un proveedor existente y registra en auditoria_proveedor cada
     * campo que haya cambiado, junto a fecha/hora (automática vía BD) y el
     * usuario responsable (tomado de la sesión activa).
     */
    public void modificar(Proveedor nuevo) throws SQLException {
        if (nuevo.getNombre() == null || nuevo.getNombre().isEmpty())
            throw new IllegalArgumentException("El nombre es obligatorio");

        Proveedor anterior = proveedorDAO.buscarPorId(nuevo.getIdProveedor());
        if (anterior == null)
            throw new IllegalArgumentException("El proveedor no existe");

        int idUsuario = SesionActual.getUsuario().getIdUsuario();

        registrarCambioSiCorresponde(nuevo.getIdProveedor(), idUsuario, "nombre",
                anterior.getNombre(), nuevo.getNombre());
        registrarCambioSiCorresponde(nuevo.getIdProveedor(), idUsuario, "telefono",
                anterior.getTelefono(), nuevo.getTelefono());
        registrarCambioSiCorresponde(nuevo.getIdProveedor(), idUsuario, "correo_electronico",
                anterior.getCorreoElectronico(), nuevo.getCorreoElectronico());

        proveedorDAO.actualizar(nuevo);
    }

    private void registrarCambioSiCorresponde(int idProveedor, int idUsuario, String campo,
                                              String valorAnterior, String valorNuevo) throws SQLException {
        if (Objects.equals(valorAnterior, valorNuevo)) return; // sin cambios en este campo
        AuditoriaProveedor a = new AuditoriaProveedor(idProveedor, idUsuario, campo,
                valorAnterior, valorNuevo);
        auditoriaDAO.insertar(a);
    }

    public List<AuditoriaProveedor> listarAuditoria(int idProveedor) throws SQLException {
        return auditoriaDAO.listarPorProveedor(idProveedor);
    }

    public Proveedor buscarPorId(int id) throws SQLException {
        return proveedorDAO.buscarPorId(id);
    }

    public List<Proveedor> listarTodos() throws SQLException {
        return proveedorDAO.listarTodos();
    }
}
