package cl.antucayen.model.service;

import cl.antucayen.model.dao.AuditoriaProveedorDAO;
import cl.antucayen.model.dao.ProveedorDAO;
import cl.antucayen.model.entity.AuditoriaProveedor;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.DBConexion;
import cl.antucayen.security.SesionActual;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ServicioProveedor {

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final AuditoriaProveedorDAO auditoriaDAO = new AuditoriaProveedorDAO();

    public void registrar(Proveedor proveedor) throws SQLException {
        Autorizacion.verificarGestionProveedores();
        validarCamposObligatorios(proveedor);
        if (proveedorDAO.existeNombre(proveedor.getNombre()))
            throw new IllegalStateException("Ya existe un proveedor con ese nombre");
        if (proveedor.getRut() != null && !proveedor.getRut().isBlank()
                && proveedorDAO.existeRut(proveedor.getRut()))
            throw new IllegalStateException("Ya existe un proveedor con ese RUT");
        proveedorDAO.insertar(proveedor);
    }

    /** Modifica y audita todos los cambios dentro de una única transacción. */
    public void modificar(Proveedor nuevo) throws SQLException {
        Autorizacion.verificarGestionProveedores();
        validarCamposObligatorios(nuevo);

        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            Proveedor anterior = proveedorDAO.buscarPorId(nuevo.getIdProveedor());
            if (anterior == null) throw new IllegalArgumentException("El proveedor no existe");
            if (proveedorDAO.existeNombreEnOtroProveedor(nuevo.getNombre(), nuevo.getIdProveedor()))
                throw new IllegalStateException("Ya existe otro proveedor con ese nombre");
            if (nuevo.getRut() != null && !nuevo.getRut().isBlank()
                    && proveedorDAO.existeRutEnOtroProveedor(nuevo.getRut(), nuevo.getIdProveedor()))
                throw new IllegalStateException("Ya existe otro proveedor con ese RUT");

            proveedorDAO.actualizar(nuevo);

            int idUsuario = SesionActual.getUsuario().getIdUsuario();
            registrarCambioSiCorresponde(nuevo.getIdProveedor(), idUsuario, "rut",
                    anterior.getRut(), nuevo.getRut());
            registrarCambioSiCorresponde(nuevo.getIdProveedor(), idUsuario, "nombre",
                    anterior.getNombre(), nuevo.getNombre());
            registrarCambioSiCorresponde(nuevo.getIdProveedor(), idUsuario, "telefono",
                    anterior.getTelefono(), nuevo.getTelefono());
            registrarCambioSiCorresponde(nuevo.getIdProveedor(), idUsuario, "correo_electronico",
                    anterior.getCorreoElectronico(), nuevo.getCorreoElectronico());
            return null;
        });
    }

    private void validarCamposObligatorios(Proveedor proveedor) {
        if (proveedor == null) throw new IllegalArgumentException("El proveedor es obligatorio");
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (proveedor.getTelefono() == null || proveedor.getTelefono().isBlank())
            throw new IllegalArgumentException("El teléfono es obligatorio");
        if (proveedor.getCorreoElectronico() == null || proveedor.getCorreoElectronico().isBlank())
            throw new IllegalArgumentException("El correo electrónico es obligatorio");
        proveedor.setNombre(proveedor.getNombre().trim());
        proveedor.setRut(normalizarRut(proveedor.getRut()));
        proveedor.setTelefono(proveedor.getTelefono().trim());
        proveedor.setCorreoElectronico(proveedor.getCorreoElectronico().trim());
    }

    private void registrarCambioSiCorresponde(int idProveedor, int idUsuario, String campo,
                                              String valorAnterior, String valorNuevo) throws SQLException {
        if (Objects.equals(valorAnterior, valorNuevo)) return;
        auditoriaDAO.insertar(new AuditoriaProveedor(
                idProveedor, idUsuario, campo, valorAnterior, valorNuevo));
    }


    /**
     * Normaliza el RUT opcional a una representación canónica sin puntos y con
     * guion antes del dígito verificador. La normalización evita que variantes
     * de formato eludan la restricción UNIQUE del esquema.
     */
    private String normalizarRut(String rut) {
        if (rut == null || rut.isBlank()) return null;
        String limpio = rut.trim().toUpperCase()
                .replace(".", "")
                .replace("-", "")
                .replaceAll("\\s+", "");
        if (limpio.length() < 2) {
            throw new IllegalArgumentException("El RUT informado no tiene un formato válido");
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.chars().allMatch(Character::isDigit)
                || !(Character.isDigit(dv) || dv == 'K')) {
            throw new IllegalArgumentException("El RUT informado no tiene un formato válido");
        }
        return cuerpo + "-" + dv;
    }

    public List<AuditoriaProveedor> listarAuditoria(int idProveedor) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        if (idProveedor <= 0) throw new IllegalArgumentException("Proveedor no válido");
        return auditoriaDAO.listarPorProveedor(idProveedor);
    }

    public Proveedor buscarPorId(int id) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return proveedorDAO.buscarPorId(id);
    }

    public List<Proveedor> listarTodos() throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return proveedorDAO.listarTodos();
    }
}
